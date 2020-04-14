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
import java.util.Optional;

import io.omam.wire.media.Payloads.MediaData;

/**
 * Represents the media information.
 * <p>
 * Only a sub-part of the API is implemented.
 *
 * @see <a href=
 *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media.MediaInformation">Google
 *      Cast Reference: MediaInformation</a>
 */
// FIXME make this a class with fromUrl, fromLiveUrl, a constructor. MediaController takes only List<Media>
public interface Media {

    /**
     * Media stream types.
     *
     * @see <a href="https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.StreamType">
     *      Google Cast Reference: StreamType </a>
     */
    public enum StreamType {
        /** buffered stream . */
        BUFFERED,
        /** live stream. */
        LIVE,
        /** none. */
        NONE;
    }

    /**
     * Returns a new {@link Media}, that can be loaded into the device.
     *
     * @param contentId {@link Media#contentId()}
     * @param contentType {@link Media#contentType()}
     * @param streamType {@link Media#streamType()}
     * @return a new {@link Media}
     */
    static Media newInstance(final String contentId, final String contentType, final StreamType streamType) {
        return new MediaData(contentId, contentType, streamType);
    }

    /**
     * Returns the URL of the media.
     *
     * @return the URL of the media
     */
    String contentId();

    /**
     * Returns the content MIME type.
     *
     * @return the content MIME type
     */
    String contentType();

    /**
     * Returns the media duration if known.
     *
     * @return the media duration if known
     */
    Optional<Duration> duration();

    /**
     * Returns the media stream type.
     *
     * @return the media stream type.
     */
    StreamType streamType();

}
