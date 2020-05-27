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

import java.util.List;
import java.util.Optional;

/**
 * Represents the status of a media session.
 * <p>
 * Only a sub-part of the API is implemented.
 *
 * @see <a href="https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media.MediaStatus">
 *      Google Cast Reference: Media Status </a>
 */
public interface MediaStatus {

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
     * Returns the ID of the current media item.
     *
     * @return the ID of the current media item or {@code empty}
     */
    Optional<Integer> currentItemId();

    /**
     * Returns the current playback position.
     *
     * @return the current playback position
     */
    double currentTime();

    /**
     * Returns the reason the player went to IDLE state, if the state is IDLE.
     *
     * @return the reason the player went to IDLE state or {@code empty} if player is not idle
     */
    Optional<IdleReason> idleReason();

    /**
     * Returns the list of media queue items.
     * <p>
     * Note: the returned list contains the current item and may contain only a couple of next items (not
     * necessarily all loaded items).
     * <p>
     * The returned list is <strong>unmodifiable</strong>.
     *
     * @return the list of media queue items
     */
    List<QueueItem> items();

    /**
     * Returns the media information.
     *
     * @return the media information
     */
    Optional<MediaInfo> media();

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
     * Returns the behavior of the queue when all items have been played.
     *
     * @return the behavior of the queue when all items have been played
     */
    RepeatMode repeatMode();

    /**
     * Returns the current stream volume.
     *
     * @return the current stream volume
     */
    Volume volume();

}
