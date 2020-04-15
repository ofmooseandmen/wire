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
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import io.omam.wire.CastChannel.CastMessage;

/**
 * Default {@link ApplicationWire} implementation.
 */
final class DefaultApplicationWire implements ApplicationWire {

    /** communication channel. */
    private final CastV2Channel channel;

    /**
     * Constructor.
     *
     * @param aChannel communication channel
     */
    DefaultApplicationWire(final CastV2Channel aChannel) {
        channel = aChannel;
    }

    @Override
    public final <T extends Payload> Optional<T> parse(final CastMessage message, final Class<T> clazz) {
        return Payloads.parse(message, clazz);
    }

    @Override
    public final <T extends Payload> CastMessage request(final String namespace, final String destination,
            final T payload, final Duration timeout) throws IOException, TimeoutException {
        return Requestor.stringPayload(channel).request(namespace, destination, payload, timeout);
    }

    @Override
    public final <P extends Payload> void send(final String namespace, final String destination, final P payload) {
        final CastMessage message = Payloads.build(namespace, destination, payload);
        channel.send(message);
    }

}
