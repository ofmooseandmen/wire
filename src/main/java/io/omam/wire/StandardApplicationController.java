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

import static io.omam.wire.Payloads.parse;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Predicate;

import io.omam.wire.CastChannel.CastMessage;
import io.omam.wire.Payloads.Message;

/**
 * Base application controller that correlates responses with requests using the JSON {@code requestId}.
 * <p>
 * This class is intended to be extended to implement the application protocol.
 *
 * @see CastDeviceController#launchApp(String, java.util.function.Function, java.time.Duration)
 * @see CastDeviceController#stopApp(ApplicationController, Duration)
 */
public abstract class StandardApplicationController extends ApplicationController {

    /** standard response predicate. */
    private static final Predicate<CastMessage> STD_RESP_PREDICATE = m -> {
        final Optional<Message> rs = parse(m, Message.class);
        return rs.isPresent() && !rs.get().requestId().isPresent();
    };

    /**
     * Constructor.
     * <p>
     * Any message containing a requestId property are assumed to be a response to a request.
     *
     * @param someDetails the details of this application
     */
    protected StandardApplicationController(final Application someDetails) {
        super(someDetails, STD_RESP_PREDICATE);
    }

}
