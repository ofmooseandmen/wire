/*
Copyright 2018 Cedric Liegeois

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
package io.omam.wire;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Represents the status of a media session.
 * <p>
 * TODO: check optional + use time api instead of int.
 *
 * @see <a href="https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media.MediaStatus">Google
 *      Cast Reference: MediaStatus</a>
 */
public interface MediaStatus {

    /**
     * Represents current status of break.
     *
     * @see <a href=
     *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media.BreakStatus">Google
     *      Cast Reference: BreakStatus</a>
     */
    public static interface BreakStatus {

        /**
         * Returns the time elapsed after current break clip starts.
         *
         * @return the time elapsed after current break clip starts
         */
        Duration currentBreakClipTime();

        /**
         * Returns the time elapsed after current break starts.
         *
         * @return the time elapsed after current break starts
         */
        Duration currentBreakTime();
    }

    /**
     * Extended media status information.
     *
     * @see <a href=
     *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media.ExtendedMediaStatus">Google
     *      Cast Reference: ExtendedMediaStatus</a>
     *
     */
    public static interface ExtendedMediaStatus {
        // TODO
    }

    /**
     * The reason for the player to be in IDLE state.
     *
     * @see <a href=
     *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.IdleReason">Google
     *      Cast Reference: IdleReason</a>
     */
    public enum IdleReason {
        /** A sender requested to stop playback using the STOP command. */
        CANCELLED,
        /** A sender requested playing a different media using the LOAD command. */
        INTERRUPTED,
        /** The media playback completed. */
        FINISHED,
        /**
         * The media was interrupted due to an error, this could happen if, for example, the player could not
         * download media due to networking errors.
         */
        ERROR;
    }

    /**
     * Provides live seekable range with start and end time.
     *
     * @see <a href=
     *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media.LiveSeekableRange">Google
     *      Cast Reference: LiveSeekableRange</a>
     */
    public static interface LiveSeekableRange {

        /**
         * Returns the end of this seekable range.
         *
         * @return the end of this seekable range
         */
        Duration end();

        /**
         * Whether a live stream has ended. If it is done, the end of live seekable range should stop updating.
         *
         * @return {@code true} if live stream has ended
         */
        boolean isLiveDone();

        /**
         * Whether the live seekable range is a moving window. When {@code false} if will be either an expanding
         * range or a fixed range meaning live has ended.
         *
         * @return {@code true} if live seekable range is a moving window
         */
        boolean isMovingWindow();

        /**
         * Returns the start of this seekable range.
         *
         * @return the start of this seekable range
         */
        Duration start();

    }

    /**
     * Represents the media information.
     *
     * @see <a href=
     *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media.MediaInformation">Google
     *      Cast Reference: MediaInformation</a>
     */
    public static interface MediaInformation {
        // TODO
    }

    /**
     * Represents the player state.
     *
     * @see <a href=
     *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.PlayerState">Google
     *      Cast Reference: PlayerState</a>
     */
    public enum PlayerState {
        /** The player is in IDLE state. */
        IDLE,
        /** The player is in PLAYING state. */
        PLAYING,
        /** The player is in PAUSED state. */
        PAUSED,
        /** The player is in BUFFERING state. */
        BUFFERING;
    }

    /**
     * Queue data as part of the LOAD request.
     *
     * @see <a href=
     *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media.QueueData">Google Cast
     *      Reference: QueueData</a>
     */
    public static interface QueueData {
        // TODO
    }

    /**
     * Queue item information.
     *
     * @see <a href=
     *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media.QueueItem">Google Cast
     *      Reference: QueueItem</a>
     */
    public static interface QueueItem {

        /**
         * Returns the ID of each active track.
         *
         * @return the ID of each active track
         */
        List<Integer> activeTracksIds();

        /**
         * Whether the media player will begin playing the element in the queue when the item becomes current.
         *
         * @return {@code true} if the media player will begin playing the element in the queue when the item
         *         becomes current
         */
        boolean autoplay();

        /**
         * Returns extra queue item information defined by the application.
         *
         * @return extra queue item information defined by the application
         */
        Object customData();

        /**
         * Returns the unique identifier of the item in the queue.
         *
         * @return the unique identifier of the item in the queue
         */
        int itemId();

        /**
         * Returns the metadata of the playlist element.
         *
         * @return the metadata of the playlist element
         */
        MediaInformation media();

        /**
         * Returns the playback duration of the item.
         *
         * @return the playback duration of the item
         */
        Duration playbackDuration();

        /**
         * Returns the time relative to the beginning of this item playback at which the item shall be preloaded to
         * allow for smooth transition between items.
         *
         * @return preload time relative to the beginning of this item playback
         */
        Duration preloadTime();

        /**
         * Returns the duration since the beginning of content.
         *
         * @return the duration since the beginning of content
         */
        Duration startTime();

    }

    /**
     * Behavior of the queue when all items have been played.
     *
     * @see <a href=
     *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.RepeatMode">Google
     *      Cast Reference: RepeatMode</a>
     */
    public enum RepeatMode {
        /** When the queue is completed the media session is terminated. */
        REPEAT_OFF,
        /**
         * All the items in the queue will be played indefinitely, when the last item is played it will play the
         * first item again.
         */
        REPEAT_ALL,
        /** The current item will be played repeatedly. */
        REPEAT_SINGLE,
        /**
         * All the items in the queue will be played indefinitely, when the last item is played it will play the
         * first item again (the list will be shuffled by the receiver first).
         */
        REPEAT_ALL_AND_SHUFFLE;
    }

    /**
     * Video information such as video resolution and High Dynamic Range (HDR).
     *
     * @see <a href=
     *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media.VideoInformation">Google
     *      Cast Reference: VideoInformation</a>
     */
    public static interface VideoInformation {
        // TODO
    }

    /**
     * Returns the list of IDs corresponding to the active tracks.
     * <p>
     * The returned list is <strong>unmodifiable</strong>.
     *
     * @return the list of IDs corresponding to the active tracks
     */
    List<Integer> activeTrackIds();

    /**
     * Returns the status of break, if receiver is playing break.
     *
     * @return the status of break or {@code empty} if receiver is not playing break
     */
    Optional<BreakStatus> breakStatus();

    /**
     * Returns the ID of this media item (the item that originated the status change).
     *
     * @return the ID of this media item
     */
    Optional<Integer> currentItemId();

    /**
     * Returns the current playback position.
     *
     * @return the current playback position
     */
    int currentTime();

    /**
     * Returns the extended media status information.
     *
     * @return the extended media status information
     */
    Optional<ExtendedMediaStatus> extendedStatus();

    /**
     * Returns the reason the player went to IDLE state, if the state is IDLE.
     *
     * @return the reason the player went to IDLE state or {@code empty} if player is not idle
     */
    Optional<IdleReason> idleReason();

    /**
     * Returns the list of media queue items.
     * <p>
     * The returned list is <strong>unmodifiable</strong>.
     *
     * @return the list of media queue items
     */
    List<QueueItem> items();

    /**
     * Returns the seekable range of a live or event stream. It uses relative media time.
     *
     * @return the seekable range of a live or event stream or {@code empty} for VOD streams
     */
    Optional<LiveSeekableRange> liveSeekableRange();

    /**
     * Returns ID of the media item currently loading.
     *
     * @return ID of the media item currently loading or {@code empty} if no media item is currently loading
     */
    Optional<Integer> loadingItemId();

    /**
     * Returns the media information.
     *
     * @return the media information
     */
    Optional<MediaInformation> media();

    /**
     * Returns the unique id for the session.
     *
     * @return the unique id for the session
     */
    int mediaSessionId();

    /**
     * Returns playback rate.
     *
     * @return playback rate
     */
    int playbackRate();

    /**
     * Returns the player state.
     *
     * @return the player state
     */
    PlayerState playerState();

    /**
     * Returns ID of the next Item.
     * <p>
     * Media items can be preloaded and cached temporarily in memory, so when they are loaded later on, the process
     * is faster (as the media does not have to be fetched from the network).
     *
     * @return the ID of the next Item or {@code empty} if next item has not been preloaded
     */
    Optional<Integer> preloadedItemId();

    /**
     * Returns the queue data.
     *
     * @return the queue data
     */
    Optional<QueueData> queueData();

    /**
     * Returns the behavior of the queue when all items have been played.
     *
     * @return the behavior of the queue when all items have been played
     */
    RepeatMode repeatMode();

    /**
     * Returns the commands supported by this player.
     *
     * @return the commands supported by this player
     */
    int supportedMediaCommands();

    /**
     * Returns the media message type.
     *
     * @return the media message type
     */
    String type();

    /**
     * Returns the video information.
     *
     * @return the video information or {@code empty} if no available
     */
    Optional<VideoInformation> videoInformation();

    /**
     * Returns the current stream volume.
     *
     * @return the current stream volume
     */
    Volume volmue();

}
