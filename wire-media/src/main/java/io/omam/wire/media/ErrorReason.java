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

/**
 * Represents media error message reasons.
 *
 * @see <a href=
 *      "https://developers.google.com/cast/docs/reference/caf_receiver/cast.framework.messages#.ErrorReason">Google
 *      Cast Reference: ErrorReason</a>
 */
public enum ErrorReason {
    /** Returned when the application state is invalid to fulfill the request. */
    APP_ERROR,
    /**
     * Returned when a request cannot be performed because authentication has expired, e.g. user changed password
     * or the token was revoked.
     */
    AUTHENTICATION_EXPIRED,
    /** Returned when too many concurrent streams are detected. */
    CONCURRENT_STREAM_LIMIT,
    /** Returned when the requested content is already playing. */
    CONTENT_ALREADY_PLAYING,
    /** Returned when the content is blocked due to filter. */
    CONTENT_FILTERED,
    /** Returned when the request ID is not unique (the receiver is processing a request with the same ID). */
    DUPLICATE_REQUEST_ID,
    /**
     * Returned when skip is not possible due to going back beyond the first item or forward beyond the last item
     * in the queue.
     */
    END_OF_QUEUE,
    /** Returned when the load request encounter intermittent issue. */
    GENERIC_LOAD_ERROR,
    /** Returned when the command is not valid or not implemented. */
    INVALID_COMMAND,
    /** Returned when the media session does not exist. */
    INVALID_MEDIA_SESSION_ID,
    /** Returned when the params are not valid or a non optional param is missing. */
    INVALID_PARAMS,
    /** Returned when the request is not valid. */
    INVALID_REQUEST,
    /** Returned when the requested language is not supported./ */
    LANGUAGE_NOT_SUPPORTED,
    /** Returned when the content is blocked due to being regionally unavailable. */
    NOT_AVAILABLE_IN_REGION,
    /** Returned when the request is not supported by the application. */
    NOT_SUPPORTED,
    /** Returned when the content is blocked due to parental controls. */
    PARENTAL_CONTROL_RESTRICTED,
    /** Returned when premium account is required for the request to succeed. */
    PREMIUM_ACCOUNT_REQUIRED,
    /** Returned when cannot skip more items due to reaching skip limit. */
    SKIP_LIMIT_REACHED,
    /** Returned when the request cannot be completed because a video-capable device is required. */
    VIDEO_DEVICE_REQUIRED;
}
