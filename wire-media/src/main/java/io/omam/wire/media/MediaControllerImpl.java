/*
Copyright 2020-2020 Cedric Liegeois

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

    * Neither the name of the copyright holder nor the names of other
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package io.omam.wire.media;

import static io.omam.wire.io.json.Payloads.parse;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.omam.wire.app.ApplicationData;
import io.omam.wire.app.ApplicationWire;
import io.omam.wire.io.CastChannel.CastMessage;
import io.omam.wire.io.json.Payload;
import io.omam.wire.media.MediaStatus.PlayerState;
import io.omam.wire.media.Payloads.ErrorData;
import io.omam.wire.media.Payloads.GetStatus;
import io.omam.wire.media.Payloads.Load;
import io.omam.wire.media.Payloads.MediaStatusData;
import io.omam.wire.media.Payloads.MediaStatusResponse;
import io.omam.wire.media.Payloads.Pause;
import io.omam.wire.media.Payloads.Play;
import io.omam.wire.media.Payloads.QueueData;
import io.omam.wire.media.Payloads.QueueGetItems;
import io.omam.wire.media.Payloads.QueueGetItemsIds;
import io.omam.wire.media.Payloads.QueueInsert;
import io.omam.wire.media.Payloads.QueueItemData;
import io.omam.wire.media.Payloads.QueueItemIds;
import io.omam.wire.media.Payloads.QueueItems;
import io.omam.wire.media.Payloads.QueueRemove;
import io.omam.wire.media.Payloads.QueueUpdate;
import io.omam.wire.media.Payloads.Seek;
import io.omam.wire.media.Payloads.Stop;

/**
 * Default Media Receiver controller.
 * <p>
 * Only a sub-part of the API is implemented.
 * <p>
 * The receiving Cast device can support either video and audio streams or audio streams only. Do not load a video
 * streams if not supported.
 * <p>
 * This application is available on all devices.
 *
 * @see <a href="https://developers.google.com/cast/docs/reference/caf_receiver/cast.framework.messages">Google
 *      Cast Framework Messages</a>
 */
final class MediaControllerImpl implements MediaController {

    /** media namespace. */
    private static final String NAMESPACE = "urn:x-cast:com.google.cast.media";

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(MediaControllerImpl.class.getName());

    /** all error types. */
    private static final Collection<String> ERRORS =
            Arrays.stream(ErrorType.values()).map(ErrorType::name).collect(Collectors.toList());

    /** application details. */
    private final ApplicationData details;

    /** application wire. */
    private final ApplicationWire wire;

    /** listeners. */
    private final ConcurrentLinkedQueue<MediaStatusListener> listeners;

    /** current media session ID or empty if not load. */
    private Optional<Integer> mediaSessionId;

    /**
     * Constructor.
     *
     * @param someDetails application details
     * @param aWire application wire
     */
    public MediaControllerImpl(final ApplicationData someDetails, final ApplicationWire aWire) {
        details = someDetails;
        wire = aWire;
        listeners = new ConcurrentLinkedQueue<>();
        mediaSessionId = Optional.empty();
    }

    /**
     * Throws {@link MediaRequestException} if given response is an error.
     *
     * @param resp message
     * @throws IOException in case of I/O error
     * @throws MediaRequestException if message is an error
     */
    private static void throwIfError(final CastMessage resp) throws IOException, MediaRequestException {
        final Optional<String> opType = parse(resp).type();
        if (!opType.isPresent()) {
            throw new IOException("Invalid response - unknown type");
        }
        final String type = opType.get();
        if (ERRORS.contains(type)) {
            final Error error = parse(resp, type, ErrorData.class);
            throw new MediaRequestException(error);
        }
    }

    /**
     * List of Medias to list of QueueItems.
     * <p>
     * autoplay is true, preload time is 1 second and start time is 0 second.
     *
     * @param medias list of Medias
     * @return List of QueueItems
     */
    private static List<QueueItem> toItems(final List<MediaInfo> medias) {
        return medias.stream().map(m -> new QueueItemData(true, m, 1, 0)).collect(Collectors.toList());
    }

    /**
     * Formats media status.
     *
     * @param ms media status or empty
     * @return string
     */
    private static String toString(final Optional<MediaStatusData> ms) {
        return "session "
            + ms.map(MediaStatus::mediaSessionId).map(i -> Integer.toString(i)).orElse("?")
            + "; player is "
            + ms.map(MediaStatus::playerState).map(PlayerState::toString).orElse("UNKNOWN");
    }

    @Override
    public void addListener(final MediaStatusListener listener) {
        Objects.requireNonNull(listener);
        listeners.add(listener);
    }

    @Override
    public final MediaStatus appendToQueue(final List<MediaInfo> medias, final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        LOGGER.info(() -> "Appending " + medias.size() + " media to queue");
        return request(id -> new QueueInsert(id, toItems(medias)), timeout);
    }

    @Override
    public final MediaStatus changeRepeatMode(final RepeatMode mode, final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        LOGGER.info(() -> "Setting playback repeat mode to " + mode);
        return request(id -> QueueUpdate.repeatMode(id, mode), timeout);
    }

    @Override
    public final ApplicationData details() {
        return details;
    }

    @Override
    public final MediaStatus getMediaStatus(final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        LOGGER.info(() -> "Requesting media status");
        return request(GetStatus.INSTANCE, timeout);
    }

    @Override
    public final List<QueueItem> getQueueItems(final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        LOGGER.info(() -> "Requesting queued items");
        final int mid = mediaSessionId.orElseThrow(() -> new IOException("Invalid request - no media session"));
        final String destination = details.transportId();
        CastMessage resp = wire.request(NAMESPACE, destination, new QueueGetItemsIds(mid), timeout);

        throwIfError(resp);

        final QueueItemIds queueItemIds = parse(resp, QueueItemIds.TYPE, QueueItemIds.class);
        resp = wire.request(NAMESPACE, destination, new QueueGetItems(mid, queueItemIds.itemIds()), timeout);

        final List<QueueItem> items = parse(resp, QueueItems.TYPE, QueueItems.class).items();
        LOGGER.info(() -> "Received response with " + items.size() + " queue items");
        return items;
    }

    @Override
    public final MediaStatus insertInQueue(final int beforeItemId, final List<MediaInfo> medias,
            final Duration timeout) throws IOException, TimeoutException, MediaRequestException {
        LOGGER.info(() -> "Inserting " + medias.size() + " media to queue before item " + beforeItemId);
        return request(id -> new QueueInsert(id, beforeItemId, toItems(medias)), timeout);
    }

    @Override
    public final MediaStatus jump(final int nb, final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        LOGGER.info(() -> "Requesting to " + (nb > 0 ? "skip " : "go back ") + nb + " items in the queue");
        return request(id -> QueueUpdate.jump(id, nb), timeout);
    }

    @Override
    public final MediaStatus load(final List<MediaInfo> medias, final RepeatMode repeatMode,
            final boolean autoplay, final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        LOGGER.info(() -> "Loading " + medias.size() + " media");
        final QueueData queue = new QueueData(toItems(medias), repeatMode);
        final Load load = new Load(details.sessionId(), medias.get(0), autoplay, 0, queue);
        final MediaStatus resp = request(load, timeout);
        mediaSessionId = Optional.of(resp.mediaSessionId());
        return resp;
    }

    @Override
    public final MediaStatus pause(final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        LOGGER.info(() -> "Pausing playback");
        return request(Pause::new, timeout);
    }

    @Override
    public final MediaStatus play(final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        LOGGER.info(() -> "Resuming playback");
        return request(Play::new, timeout);
    }

    @Override
    public final MediaStatus removeFromQueue(final List<Integer> itemIds, final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        LOGGER.info(() -> "Removing queue items " + itemIds);
        return request(id -> new QueueRemove(id, itemIds), timeout);
    }

    @Override
    public final void removeListener(final MediaStatusListener listener) {
        Objects.requireNonNull(listener);
        listeners.add(listener);
    }

    @Override
    public final MediaStatus seek(final Duration amount, final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        LOGGER.info(() -> "Seeking current media by " + amount);
        return request(id -> new Seek(id, amount.getSeconds()), timeout);
    }

    @Override
    public final MediaStatus stop(final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        LOGGER.info(() -> "Stopping playback");
        final MediaStatus resp = request(Stop::new, timeout);
        /* media session terminated. */
        mediaSessionId = Optional.empty();
        return resp;
    }

    @Override
    public final void uncorrelatedResponseReceived(final CastMessage message) {
        notifyListeners(message, true);
    }

    @Override
    public final void unsolicitedMessageReceived(final CastMessage message) {
        notifyListeners(message, false);
    }

    /**
     * Notifies listeners.
     *
     * @param message received message
     * @param timeout true if and only if the message is a response to a request which had timeout
     */
    private void notifyListeners(final CastMessage message, final boolean timeout) {
        try {
            final MediaStatusResponse status = parse(message, MediaStatusResponse.TYPE, MediaStatusResponse.class);
            notifyMediaStatus(status, timeout);
        } catch (final IOException e1) {
            try {
                final Error error = parse(message, ErrorType.ERROR.name(), ErrorData.class);
                LOGGER.warning(() -> "Received media error: " + error);
                listeners.forEach(l -> l.mediaErrorReceived(error, timeout));
            } catch (final IOException e2) {
                LOGGER.log(Level.FINE, e2, () -> "Could not parse received media message");
            }
        }
    }

    /**
     * Notifies listeners about the received media status.
     *
     * @param message the received message
     * @param timeout true if and only if the message is a response to a request which had timeout
     */
    private void notifyMediaStatus(final MediaStatusResponse message, final boolean timeout) {
        final Optional<MediaStatusData> optStatus = message.status();
        LOGGER.info(() -> "Received updated media status " + toString(optStatus));
        if (optStatus.isPresent()) {
            final MediaStatus ms = optStatus.get();
            if (!mediaSessionId.isPresent()) {
                /* load response not received before timeout, but was successful anyway. */
                mediaSessionId = Optional.of(ms.mediaSessionId());
            }
            listeners.forEach(l -> l.mediaStatusUpdated(ms, timeout));
        } else {
            LOGGER.warning(() -> "Received empty media status");
        }
    }

    /**
     * Sends a request to the player and wait for the response.
     *
     * @param f function that takes a media session ID and returns the request payload
     * @param timeout response timeout
     * @return current media status, never null
     * @throws IOException in case of I/O error
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    private MediaStatus request(final Function<Integer, Payload> f, final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        final Payload payload = mediaSessionId
            .map(i -> f.apply(i))
            .orElseThrow(() -> new IOException("Invalid request - no media session"));
        return request(payload, timeout);
    }

    /**
     * Sends a request to the player and wait for the response.
     *
     * @param payload request payload
     * @param timeout response timeout
     * @return current media status, never null
     * @throws IOException in case of I/O error
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    private MediaStatus request(final Payload payload, final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        final String destination = details.transportId();
        final CastMessage resp = wire.request(NAMESPACE, destination, payload, timeout);

        throwIfError(resp);

        final Optional<MediaStatusData> status =
                parse(resp, MediaStatusResponse.TYPE, MediaStatusResponse.class).status();
        LOGGER.info(() -> "Received response media status " + toString(status));
        return status.orElseThrow(() -> new IOException("Invalid response - no media status"));
    }

}
