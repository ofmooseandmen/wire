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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import io.omam.wire.ApplicationData;
import io.omam.wire.ApplicationWire;
import io.omam.wire.CastChannel.CastMessage;
import io.omam.wire.Payload;
import io.omam.wire.StandardApplicationController;
import io.omam.wire.media.Payloads.GetStatus;
import io.omam.wire.media.Payloads.Load;
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
@SuppressWarnings("javadoc")
final class MediaControllerImpl extends StandardApplicationController implements MediaController {

    /** media namespace. */
    private static final String NAMESPACE = "urn:x-cast:com.google.cast.media";

    /** application details. */
    private final ApplicationData details;

    /** application wire. */
    private final ApplicationWire wire;

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
        mediaSessionId = -1;
    }

    private static List<QueueItem> toItems(final List<MediaInfo> medias) {
        return medias.stream().map(m -> new QueueItemData(true, m, 1, 0)).collect(Collectors.toList());
    }

    @Override
    public final MediaStatus addToQueue(final List<MediaInfo> medias, final Duration timeout)
            throws IOException, TimeoutException {
        return request(new QueueInsert(mediaSessionId, toItems(medias)), timeout);
    }

    @Override
    public final MediaStatus load(final List<MediaInfo> medias, final RepeatMode repeatMode, final boolean autoplay,
            final Duration timeout) throws IOException, TimeoutException {
        final QueueData queue = new QueueData(toItems(medias), repeatMode);
        final Load load = new Load(details.sessionId(), medias.get(0), autoplay, 0, queue);
        final MediaStatus resp = request(load, timeout);
        mediaSessionId = resp.mediaSessionId();
        return resp;
    }

    @Override
    public final MediaStatus getMediaStatus(final Duration timeout) throws IOException, TimeoutException {
        return request(GetStatus.INSTANCE, timeout);
    }

    @Override
    public final MediaStatus next(final Duration timeout) throws IOException, TimeoutException {
        return request(QueueUpdate.jump(mediaSessionId, 1), timeout);
    }

    @Override
    public final MediaStatus pause(final Duration timeout) throws IOException, TimeoutException {
        return request(new Pause(mediaSessionId), timeout);
    }

    @Override
    public final MediaStatus play(final Duration timeout) throws IOException, TimeoutException {
        return request(new Play(mediaSessionId), timeout);
    }

    @Override
    public final MediaStatus previous(final Duration timeout) throws IOException, TimeoutException {
        return request(QueueUpdate.jump(mediaSessionId, -1), timeout);
    }

    @Override
    public final List<QueueItem> getQueueItems(final Duration timeout) throws IOException, TimeoutException {
        final String destination = details.transportId();
        CastMessage resp = wire.request(NAMESPACE, destination, new QueueGetItemsIds(mediaSessionId), timeout);
        final QueueItemIds queueItemIds =
                wire.parse(resp, QueueItemIds.class).orElseThrow(() -> new IOException("invalid queue item ids"));
        resp = wire
            .request(NAMESPACE, destination, new QueueGetItems(mediaSessionId, queueItemIds.itemIds()), timeout);
        return wire
            .parse(resp, QueueItems.class)
            .map(QueueItems::items)
            .orElseThrow(() -> new IOException("invalid queue items"));
    }

    @Override
    public final MediaStatus removeFromQueue(final List<Integer> itemIds, final Duration timeout)
            throws IOException, TimeoutException {
        return request(new QueueRemove(mediaSessionId, itemIds), timeout);
    }

    @Override
    public final MediaStatus seek(final Duration elapsed, final Duration timeout)
            throws IOException, TimeoutException {
        return request(new Seek(mediaSessionId, elapsed.getSeconds()), timeout);
    }

    @Override
    public final MediaStatus setRepeatMode(final RepeatMode mode, final Duration timeout)
            throws IOException, TimeoutException {
        return request(QueueUpdate.repeatMode(mediaSessionId, mode), timeout);
    }

    @Override
    public final MediaStatus stop(final Duration timeout) throws IOException, TimeoutException {
        return request(new Stop(mediaSessionId), timeout);
    }

    @Override
    protected final void appMessageReceived(final CastMessage message) {
        // TODO, if status notify listener(s).
    }

    private MediaStatus request(final Payload payload, final Duration timeout)
            throws IOException, TimeoutException {
        final String destination = details.transportId();
        final CastMessage resp = wire.request(NAMESPACE, destination, payload, timeout);
        // TODO: check for possible errors: type":"INVALID_REQUEST","reason":"INVALID_MEDIA_SESSION_ID"
        final Optional<MediaStatusResponse> parsed = wire.parse(resp, MediaStatusResponse.class);
        return parsed
            .flatMap(MediaStatusResponse::status)
            .orElseThrow(() -> new IOException("invalid media status"));
    }

}
