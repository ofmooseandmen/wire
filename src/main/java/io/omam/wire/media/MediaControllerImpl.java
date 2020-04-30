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

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.omam.wire.ApplicationData;
import io.omam.wire.ApplicationWire;
import io.omam.wire.CastChannel.CastMessage;
import io.omam.wire.Payload;
import io.omam.wire.StandardApplicationController;
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
final class MediaControllerImpl extends StandardApplicationController implements MediaController {

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

    /** current media session ID or -1. */
    private int mediaSessionId;

    /**
     * Constructor.
     *
     * @param someDetails application details
     * @param aWire application wire
     */
    public MediaControllerImpl(final ApplicationData someDetails, final ApplicationWire aWire) {
        super(someDetails);
        details = someDetails;
        wire = aWire;
        listeners = new ConcurrentLinkedQueue<>();
        mediaSessionId = -1;
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

    @Override
    public void addListener(final MediaStatusListener listener) {
        Objects.requireNonNull(listener);
        listeners.add(listener);
    }

    @Override
    public final MediaStatus addToQueue(final List<MediaInfo> medias, final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        LOGGER.info(() -> "Adding " + medias.size() + " media to queue");
        return request(new QueueInsert(mediaSessionId, toItems(medias)), timeout);
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
        final String destination = details.transportId();
        CastMessage resp = wire.request(NAMESPACE, destination, new QueueGetItemsIds(mediaSessionId), timeout);

        throwIfError(resp);

        final QueueItemIds queueItemIds = wire.parse(resp, QueueItemIds.TYPE, QueueItemIds.class);
        resp = wire
            .request(NAMESPACE, destination, new QueueGetItems(mediaSessionId, queueItemIds.itemIds()), timeout);
        return wire.parse(resp, QueueItems.TYPE, QueueItems.class).items();
    }

    @Override
    public final MediaStatus load(final List<MediaInfo> medias, final RepeatMode repeatMode,
            final boolean autoplay, final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        LOGGER.info(() -> "Loading " + medias.size() + " media");
        final QueueData queue = new QueueData(toItems(medias), repeatMode);
        final Load load = new Load(details.sessionId(), medias.get(0), autoplay, 0, queue);
        final MediaStatus resp = request(load, timeout);
        mediaSessionId = resp.mediaSessionId();
        return resp;
    }

    @Override
    public final MediaStatus next(final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        LOGGER.info(() -> "Requesting playback of next queued media");
        return request(QueueUpdate.jump(mediaSessionId, 1), timeout);
    }

    @Override
    public final MediaStatus pause(final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        LOGGER.info(() -> "Pausing playback");
        return request(new Pause(mediaSessionId), timeout);
    }

    @Override
    public final MediaStatus play(final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        LOGGER.info(() -> "Resuming playback");
        return request(new Play(mediaSessionId), timeout);
    }

    @Override
    public final MediaStatus previous(final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        LOGGER.info(() -> "Requesting playback of previous queued media");
        return request(QueueUpdate.jump(mediaSessionId, -1), timeout);
    }

    @Override
    public final MediaStatus removeFromQueue(final List<Integer> itemIds, final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        LOGGER.info(() -> "Removing queue items " + itemIds);
        return request(new QueueRemove(mediaSessionId, itemIds), timeout);
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
        return request(new Seek(mediaSessionId, amount.getSeconds()), timeout);
    }

    @Override
    public final MediaStatus setRepeatMode(final RepeatMode mode, final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        LOGGER.info(() -> "Setting playback repeat mode to " + mode);
        return request(QueueUpdate.repeatMode(mediaSessionId, mode), timeout);
    }

    @Override
    public final MediaStatus stop(final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        LOGGER.info(() -> "Stopping playback");
        return request(new Stop(mediaSessionId), timeout);
    }

    @Override
    protected final void unsolicitedMessageReceived(final String type, final CastMessage message) {
        if (MediaStatusResponse.TYPE.equals(type)) {
            notifyMediaStatus(message);
        } else if (ErrorType.ERROR.name().equals(type)) {
            notifyMediaError(message);
        }
    }

    /**
     * Notifies listeners about the received error.
     *
     * @param message the received message
     */
    private void notifyMediaError(final CastMessage message) {
        LOGGER.warning(() -> "Received media error");
        try {
            final Error error = wire.parse(message, ErrorType.ERROR.name(), ErrorData.class);
            LOGGER.warning(() -> "Received media error [" + message.getPayloadUtf8() + "]");
            listeners.forEach(l -> l.mediaErrorReceived(error));
        } catch (final IOException e) {
            LOGGER.log(Level.FINE, e, () -> "Could not parse received media error");
        }
    }

    /**
     * Notifies listeners about the received media status.
     *
     * @param message the received message
     */
    private void notifyMediaStatus(final CastMessage message) {
        LOGGER.info(() -> "Received updated media status");
        try {
            final Optional<MediaStatusData> optStatus =
                    wire.parse(message, MediaStatusResponse.TYPE, MediaStatusResponse.class).status();
            if (optStatus.isPresent()) {
                final MediaStatus ms = optStatus.get();
                LOGGER.fine(() -> "Received new media status [" + message.getPayloadUtf8() + "]");
                listeners.forEach(l -> l.mediaStatusUpdated(ms));
            } else {
                LOGGER.fine(() -> "Received invalid media status");
            }
        } catch (final IOException e) {
            LOGGER.log(Level.FINE, e, () -> "Could not parse received media status");
        }
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
                wire.parse(resp, MediaStatusResponse.TYPE, MediaStatusResponse.class).status();
        return status.orElseThrow(() -> new IOException("Invalid response - no media status"));
    }

    /**
     * Throws {@link MediaRequestException} if given response is an error.
     *
     * @param resp message
     * @throws IOException in case of I/O error
     * @throws MediaRequestException if message is an error
     */
    private void throwIfError(final CastMessage resp) throws IOException, MediaRequestException {
        final Optional<String> opType = wire.parse(resp).type();
        if (opType.isEmpty()) {
            throw new IOException("Invalid response - unknown type");
        }

        final String type = opType.get();
        if (ERRORS.contains(type)) {
            final Error error = wire.parse(resp, type, ErrorData.class);
            throw new MediaRequestException(error);
        }
    }

}
