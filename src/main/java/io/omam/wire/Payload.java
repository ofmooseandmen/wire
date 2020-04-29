/*
Copyright 2018-2020 Cedric Liegeois

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

import java.util.Optional;

/**
 * Base class of all Cast V2 {@code PayloadType.STRING} payloads.
 * <p>
 * Depending on the message either {@link #type()} or {@link #responseType()} is present. Note that some response
 * contain {@link #type()} instead of {@link #responseType()}.
 */
public abstract class Payload {

    /**
     * Class representing any payload - use for JSON parsing.
     */
    public static final class Any extends Payload {

        /**
         * Constructor.
         */
        Any() {
            // empty.
        }

    }

    /** message type, or null if response. */
    private final String type;

    /** response type, or null if message or request. */
    private final String responseType;

    /** request ID, null or 0 when not a request. */
    private Integer requestId;

    /**
     * Constructor.
     */
    public Payload() {
        this(null, null);
    }

    /**
     * Constructor.
     *
     * @param aType type
     * @param aResponseType response type
     */
    protected Payload(final String aType, final String aResponseType) {
        type = aType;
        responseType = aResponseType;
        requestId = null;
    }

    /**
     * @return the Id of the request, used to correlate request/response, if present.
     */
    final Optional<Integer> requestId() {
        if (requestId == null || requestId.intValue() == 0) {
            return Optional.empty();
        }
        return Optional.of(requestId);
    }

    /**
     * @return the response type, if present.
     */
    final Optional<String> responseType() {
        return Optional.ofNullable(responseType);
    }

    /**
     * Sets the request ID of this payload.
     *
     * @param aRequestId request ID
     */
    final void setRequestId(final int aRequestId) {
        requestId = aRequestId;
    }

    /**
     * @return the message type, if present.
     */
    final Optional<String> type() {
        return Optional.ofNullable(type);
    }

}
