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

import static io.omam.wire.Payloads.addRequestId;
import static io.omam.wire.Payloads.parse;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.BiPredicate;

import io.omam.wire.CastChannel.CastMessage;
import io.omam.wire.Payloads.Message;

/**
 * A {@code StandardRequestor} transmits a request to the device and awaits for a correlated response assuming that
 * both the request and response JSON payload contain a {@code requestId} attribute.
 * <p>
 * This class is not thread-safe.
 */
final class StandardRequestor {

    /** request/response correlation predicate. */
    private static final BiPredicate<CastMessage, CastMessage> CORRELATOR = (req, resp) -> {
        final Optional<Message> pReq = parse(req, Message.class);
        final Optional<Message> pResp = parse(resp, Message.class);
        if (pReq.isPresent() && pResp.isPresent()) {
            return pReq.get().requestId().isPresent() && pReq.get().requestId().equals(pResp.get().requestId());
        }
        return false;
    };

    /** requestor. */
    private final Requestor requestor;

    /**
     * Constructor.
     *
     * @param aChannel communication channel
     */
    StandardRequestor(final CastV2Channel aChannel) {
        requestor = new Requestor(aChannel, CORRELATOR);
    }

    /**
     * Transmits a new request and returns the received response
     *
     * @param request the request to transmit
     * @param timeout status timeout
     * @return the received response, never null
     * @throws TimeoutException if the timeout has elapsed before a response was received
     */
    final CastMessage request(final CastMessage request, final Duration timeout) throws TimeoutException {
        return requestor.request(addRequestId(request), timeout);
    }

}
