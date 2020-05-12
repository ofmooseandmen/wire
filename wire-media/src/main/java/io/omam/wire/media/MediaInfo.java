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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the media information.
 * <p>
 * Only a sub-part of the API is implemented.
 *
 * @see <a href=
 *      "https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media.MediaInformation">Google
 *      Cast Reference: MediaInformation</a>
 */
public final class MediaInfo {

    /**
     * Media stream types.
     *
     * @see <a href="https://developers.google.com/cast/docs/reference/receiver/cast.receiver.media#.StreamType">
     *      Google Cast Reference: StreamType </a>
     */
    public enum StreamType {
        /** Stored media streamed from an existing data store. */
        BUFFERED,
        /** Live media generated on the fly. */
        LIVE,
        /** None of the above. */
        NONE;
    }

    /** {@link #contentId()}, never null. */
    private final String contentId;

    /** {@link #contentId()}, never null. */
    private final String contentType;

    /** {@link #duration()}, may be null. */
    private final Double duration;

    /** {@link #contentId()}, never null. */
    private final StreamType streamType;

    /**
     * Constructor.
     *
     * @param aContentId content ID (URL), not null
     * @param aContentType content MIME type, not null
     * @param aStreamType stream type, not null
     */
    public MediaInfo(final String aContentId, final String aContentType, final StreamType aStreamType) {
        contentId = Objects.requireNonNull(aContentId);
        duration = null;
        contentType = Objects.requireNonNull(aContentType);
        streamType = Objects.requireNonNull(aStreamType);
    }

    /**
     * Stored media streamed from an existing data store, {@link Files#probeContentType(java.nio.file.Path)
     * probing} its content MIME.
     *
     * @param contentId content ID (URL) of the media, not null
     * @return {@link MediaInfo} instance
     * @throws IOException if an I/O error occurs
     */
    public static MediaInfo fromDataStream(final String contentId) throws IOException {
        final String contentType = Files.probeContentType(Paths.get(contentId));
        return new MediaInfo(contentId, contentType, StreamType.BUFFERED);
    }

    /**
     * Live media generated on the fly, {@link Files#probeContentType(java.nio.file.Path) probing} its content
     * MIME.
     *
     * @param contentId content ID (URL) of the media, not null
     * @return {@link MediaInfo} instance
     * @throws IOException if an I/O error occurs
     */
    public static MediaInfo fromLiveStream(final String contentId) throws IOException {
        final String contentType = Files.probeContentType(Paths.get(contentId));
        return new MediaInfo(contentId, contentType, StreamType.LIVE);
    }

    /**
     * Returns the URL of the media.
     *
     * @return the URL of the media
     */
    public final String contentId() {
        return contentId;
    }

    /**
     * Returns the content MIME type.
     *
     * @return the content MIME type
     */
    public final String contentType() {
        return contentType;
    }

    /**
     * Returns the media duration if known.
     *
     * @return the media duration if known
     */
    public final Optional<Duration> duration() {
        return duration == null ? Optional.empty() : Optional.of(Duration.ofMillis((long) (duration * 1000)));
    }

    /**
     * Returns the media stream type.
     *
     * @return the media stream type.
     */
    public final StreamType streamType() {
        return streamType;
    }

}
