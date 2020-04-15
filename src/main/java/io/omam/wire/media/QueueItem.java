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

import java.time.Duration;

/**
 * Queue item information.
 *
 * @see <a href= "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media.QueueItem">Google
 *      Cast Reference: QueueItem</a>
 */
public interface QueueItem {

    /**
     * Whether the media player will begin playing the element in the queue when the item becomes current.
     *
     * @return {@code true} if the media player will begin playing the element in the queue when the item becomes
     *         current
     */
    boolean autoplay();

    /**
     * Returns the unique identifier of the item in the queue.
     *
     * @return the unique identifier of the item in the queue
     */
    int itemId();

    /**
     * Returns the media data of the playlist element.
     *
     * @return the media data of the playlist element
     */
    Media media();

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
