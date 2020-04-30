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

import static io.omam.wire.CastV2Protocol.REQUEST_TIMEOUT;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import io.omam.wire.ApplicationController;
import io.omam.wire.ApplicationData;
import io.omam.wire.ApplicationWire;

/**
 * Default Media Receiver controller.
 * <p>
 * Only a sub-part of the API is implemented.
 * <p>
 * The receiving Cast device can support either video and audio streams or audio streams only. Do not load a video
 * streams if not supported.
 * <p>
 * This application is available on all devices.
 */
public interface MediaController extends ApplicationController {

    /** the ID of the default media receiver application. */
    static final String APP_ID = "CC1AD845";

    /**
     * Returns a new instance of {@code MediaController}.
     *
     * @param appDetails the default media receiver application details
     * @param wire API to communicate with the Cast device
     * @return a new instance of {@code MediaController}
     */
    static MediaController newInstance(final ApplicationData appDetails, final ApplicationWire wire) {
        return new MediaControllerImpl(appDetails, wire);
    }

    /**
     * Adds the given listener to receive media status update events.
     *
     * @param listener listener, not null
     */
    void addListener(final MediaStatusListener listener);

    /**
     * Requests the player to add the given medias to the end of the queue.
     *
     * @param medias list of medias to add
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    default MediaStatus addToQueue(final List<MediaInfo> medias)
            throws IOException, TimeoutException, MediaRequestException {
        return addToQueue(medias, REQUEST_TIMEOUT);
    }

    /**
     * Requests the player to add the given medias to the end of the queue.
     *
     * @param medias list of medias to add
     * @param timeout response timeout
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    MediaStatus addToQueue(final List<MediaInfo> medias, final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException;

    /**
     * Requests and returns current media status.
     *
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     * @throws MediaRequestException if the request is rejected by the device
     */
    default MediaStatus getMediaStatus() throws IOException, TimeoutException, MediaRequestException {
        return getMediaStatus(REQUEST_TIMEOUT);
    }

    /**
     * Requests and returns current media status.
     *
     * @param timeout response timeout
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    MediaStatus getMediaStatus(final Duration timeout) throws IOException, TimeoutException, MediaRequestException;

    /**
     * Requests and returns the list of media queue items.
     * <p>
     * This method exists since some devices do not report the full list of media queue items in
     * {@link MediaStatus#items()}.
     *
     * @return the list of media queue items
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    default List<QueueItem> getQueueItems() throws IOException, TimeoutException, MediaRequestException {
        return getQueueItems(REQUEST_TIMEOUT);
    }

    /**
     * Requests and returns the list of media queue items.
     * <p>
     * This method exists since some devices do not report the full list of media queue items in
     * {@link MediaStatus#items()}.
     *
     * @param timeout response timeout
     * @return the list of media queue items
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    List<QueueItem> getQueueItems(final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException;

    /**
     * Requests the player to load the given medias. The media player will begin playing the content when it is
     * loaded and repeat mode is set to {@link RepeatMode#REPEAT_OFF OFF}.
     *
     * @param medias list of medias to load
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    default MediaStatus load(final List<MediaInfo> medias)
            throws IOException, TimeoutException, MediaRequestException {
        return load(medias, REQUEST_TIMEOUT);
    }

    /**
     * Requests the player to load the given medias. The media player will begin playing the content when it is
     * loaded and repeat mode is set to {@link RepeatMode#REPEAT_OFF OFF}.
     *
     * @param medias list of medias to load
     * @param timeout response timeout
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    default MediaStatus load(final List<MediaInfo> medias, final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException {
        return load(medias, RepeatMode.REPEAT_OFF, true, timeout);
    }

    /**
     * Requests the player to load the given medias.
     *
     * @param medias list of medias to load
     * @param repeatMode behaviour of the queue when all items have been played
     * @param autoplay whether the media player should begin playing the content when it is loaded
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    default MediaStatus load(final List<MediaInfo> medias, final RepeatMode repeatMode, final boolean autoplay)
            throws IOException, TimeoutException, MediaRequestException {
        return load(medias, repeatMode, autoplay, REQUEST_TIMEOUT);
    }

    /**
     * Requests the player to load the given medias.
     *
     * @param medias list of medias to load
     * @param repeatMode behaviour of the queue when all items have been played
     * @param autoplay whether the media player should begin playing the content when it is loaded
     * @param timeout response timeout
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    MediaStatus load(final List<MediaInfo> medias, final RepeatMode repeatMode, final boolean autoplay,
            final Duration timeout) throws IOException, TimeoutException, MediaRequestException;

    /**
     * Requests the player to play the next media in the queue.
     *
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    default MediaStatus next() throws IOException, TimeoutException, MediaRequestException {
        return next(REQUEST_TIMEOUT);
    }

    /**
     * Requests the player to play the next media in the queue.
     *
     * @param timeout response timeout
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    MediaStatus next(final Duration timeout) throws IOException, TimeoutException, MediaRequestException;

    /**
     * Requests to pause the player.
     *
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    default MediaStatus pause() throws IOException, TimeoutException, MediaRequestException {
        return pause(REQUEST_TIMEOUT);
    }

    /**
     * Requests to pause the player.
     *
     * @param timeout response timeout
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    MediaStatus pause(final Duration timeout) throws IOException, TimeoutException, MediaRequestException;

    /**
     * Requests the player to continue playing.
     *
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    default MediaStatus play() throws IOException, TimeoutException, MediaRequestException {
        return play(REQUEST_TIMEOUT);
    }

    /**
     * Requests the player to continue playing.
     *
     * @param timeout response timeout
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    MediaStatus play(final Duration timeout) throws IOException, TimeoutException, MediaRequestException;

    /**
     * Requests the player to play the previous media in the queue.
     *
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    default MediaStatus previous() throws IOException, TimeoutException, MediaRequestException {
        return previous(REQUEST_TIMEOUT);
    }

    /**
     * Requests the player to play the previous media in the queue.
     *
     * @param timeout response timeout
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    MediaStatus previous(final Duration timeout) throws IOException, TimeoutException, MediaRequestException;

    /**
     * Requests the player to remove the items corresponding to the given identifiers from the queue. The
     * identifiers can be obtain using {@link #getQueueItems(Duration)}.
     *
     * @param itemIds list of queue items identifiers to remove
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    default MediaStatus removeFromQueue(final List<Integer> itemIds)
            throws IOException, TimeoutException, MediaRequestException {
        return removeFromQueue(itemIds, REQUEST_TIMEOUT);
    }

    /**
     * Requests the player to remove the items corresponding to the given identifiers from the queue. The
     * identifiers can be obtain using {@link #getQueueItems(Duration)}.
     *
     * @param itemIds list of queue items identifiers to remove
     * @param timeout response timeout
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    MediaStatus removeFromQueue(final List<Integer> itemIds, final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException;

    /**
     * Removes the given listener so that it no longer receives media status update events.
     *
     * @param listener listener, not null
     */
    void removeListener(final MediaStatusListener listener);

    /**
     * Requests the player to seek to current playback time + given {@code amount} in the media.
     *
     * @param amount amount of time relative to the current playback position
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    default MediaStatus seek(final Duration amount) throws IOException, TimeoutException, MediaRequestException {
        return seek(amount, REQUEST_TIMEOUT);
    }

    /**
     * Requests the player to seek to current playback time + given {@code amount} in the media.
     *
     * @param amount amount of time relative to the current playback position
     * @param timeout response timeout
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    MediaStatus seek(final Duration amount, final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException;

    /**
     * Sets the behaviour of the queue when all items have been played.
     *
     * @param mode the repeat mode
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the default timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    default MediaStatus setRepeatMode(final RepeatMode mode)
            throws IOException, TimeoutException, MediaRequestException {
        return setRepeatMode(mode, REQUEST_TIMEOUT);
    }

    /**
     * Sets the behaviour of the queue when all items have been played.
     *
     * @param mode the repeat mode
     * @param timeout response timeout
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    MediaStatus setRepeatMode(final RepeatMode mode, final Duration timeout)
            throws IOException, TimeoutException, MediaRequestException;

    /**
     * Requests the device to stop the player.
     *
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    default MediaStatus stop() throws IOException, TimeoutException, MediaRequestException {
        return stop(REQUEST_TIMEOUT);
    }

    /**
     * Requests the device to stop the player.
     *
     * @param timeout response timeout
     * @return the current media status, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     * @throws MediaRequestException if the request is rejected by the device
     */
    MediaStatus stop(final Duration timeout) throws IOException, TimeoutException, MediaRequestException;

}
