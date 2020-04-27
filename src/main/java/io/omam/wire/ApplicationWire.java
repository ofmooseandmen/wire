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
package io.omam.wire;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import io.omam.wire.CastChannel.CastMessage;

/**
 * An API provided to {@link ApplicationController application controller}s to communicate with the Cast device.
 */
public interface ApplicationWire {

    /**
     * Determines if given message is an unsolicited message of the given type.
     *
     * @param message message
     * @param type expected type of the payload
     * @return {@code true} if unsolicited message of the given type, {@code false} otherwise
     */
    boolean isUnsolicited(final CastMessage message, final String type);

    /**
     * Obtains an instance of {@code T} from the payload of the given message.
     *
     * @param <T> the type of the desired object
     * @param message message
     * @param type expected payload type
     * @param clazz the class of {@code T}
     * @return the parsed {@code T}
     * @throws IOException if the payload is not of the given type or the it cannot be parsed
     */
    <T extends Payload> T parse(final CastMessage message, final String type, final Class<T> clazz)
            throws IOException;

    /**
     * Sends the given request and await for a correlated response using the standard request ID mechanism.
     * <p>
     * This method assumes that both the request and response JSON payload contain a {@code requestId} attribute.
     *
     * @param namespace request namespace
     * @param destination request destination
     * @param payload request payload
     * @param <T> the type of the payload
     * @param timeout response timeout
     * @return received response
     * @throws IOException in case of I/O error
     * @throws TimeoutException if the timeout elapsed before the response was received
     */
    <T extends Payload> CastMessage request(final String namespace, final String destination, final T payload,
            final Duration timeout) throws IOException, TimeoutException;

    /**
     * Sends a message (i.e. not expecting a response) to the application.
     *
     * @param namespace request namespace
     * @param destination request destination
     * @param payload request payload
     * @param <T> the type of the payload
     */
    <T extends Payload> void send(final String namespace, final String destination, final T payload);

}
