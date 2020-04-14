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

import io.omam.wire.Application;
import io.omam.wire.ApplicationController;
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
@SuppressWarnings("javadoc")
public interface MediaController extends ApplicationController {

    /** the ID of the default media receiver application. */
    static final String APP_ID = "CC1AD845";

    static MediaController newInstance(final Application appDetails, final ApplicationWire wire) {
        return new MediaControllerImpl(appDetails, wire);
    }

    default MediaStatus load(final List<Media> medias) throws IOException, TimeoutException {
        return load(medias, REQUEST_TIMEOUT);
    }

    default MediaStatus load(final List<Media> medias, final Duration timeout)
            throws IOException, TimeoutException {
        return load(medias, RepeatMode.REPEAT_OFF, true, timeout);
    }

    default MediaStatus load(final List<Media> medias, final RepeatMode repeatMode, final boolean autoplay)
            throws IOException, TimeoutException {
        return load(medias, repeatMode, autoplay, REQUEST_TIMEOUT);
    }

    MediaStatus load(final List<Media> medias, final RepeatMode repeatMode, final boolean autoplay,
            final Duration timeout) throws IOException, TimeoutException;

    default MediaStatus mediaStatus() throws IOException, TimeoutException {
        return mediaStatus(REQUEST_TIMEOUT);
    }

    MediaStatus mediaStatus(final Duration timeout) throws IOException, TimeoutException;

    default MediaStatus next() throws IOException, TimeoutException {
        return next(REQUEST_TIMEOUT);
    }

    MediaStatus next(final Duration timeout) throws IOException, TimeoutException;

    default MediaStatus pause() throws IOException, TimeoutException {
        return pause(REQUEST_TIMEOUT);
    }

    MediaStatus pause(final Duration timeout) throws IOException, TimeoutException;

    default MediaStatus play() throws IOException, TimeoutException {
        return play(REQUEST_TIMEOUT);
    }

    MediaStatus play(final Duration timeout) throws IOException, TimeoutException;

    default MediaStatus previous() throws IOException, TimeoutException {
        return previous(REQUEST_TIMEOUT);
    }

    MediaStatus previous(final Duration timeout) throws IOException, TimeoutException;

    default MediaStatus seek(final Duration elapsed) throws IOException, TimeoutException {
        return seek(elapsed, REQUEST_TIMEOUT);
    }

    MediaStatus seek(final Duration elapsed, final Duration timeout) throws IOException, TimeoutException;

    default MediaStatus stop() throws IOException, TimeoutException {
        return stop(REQUEST_TIMEOUT);
    }

    MediaStatus stop(final Duration timeout) throws IOException, TimeoutException;

}
