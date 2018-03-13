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

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.BiPredicate;
import java.util.logging.Logger;

import io.omam.wire.CastChannel.CastMessage;

/**
 * A {@code Requestor} transmits a request to the device and awaits for a correlated response.
 * <p>
 * This class is not thread-safe.
 */
final class Requestor implements ChannelListener {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(Requestor.class.getName());

    /** communication channel. */
    private final CastV2Channel channel;

    /** request/response correlator. */
    private final BiPredicate<CastMessage, CastMessage> correlator;

    /** the transmitted request. */
    private CastMessage req;

    /** whether a response has been received for the transmitted request. */
    private volatile boolean receivedResponse;

    /** monitors when request has been responded. */
    private final Monitor monitor;

    /**
     * last received response, null if no request has been transmitted, no response has been received.
     */
    private volatile CastMessage response;

    /**
     * Constructor.
     *
     * @param aChannel communication channel
     * @param aCorrelator a {@code BiPredicate} that accepts a request and a received message and returns
     *            {@code true} if that latter is correlated with the request
     */
    Requestor(final CastV2Channel aChannel, final BiPredicate<CastMessage, CastMessage> aCorrelator) {
        channel = aChannel;
        correlator = aCorrelator;
        req = null;
        receivedResponse = false;
        monitor = new Monitor(() -> receivedResponse);
        response = null;
    }

    @Override
    public final void messageReceived(final CastMessage message) {
        monitor.lock();
        try {
            if (correlator.test(req, message)) {
                LOGGER.fine(() -> "Received response [" + message + "] answering request");
                receivedResponse = true;
                response = message;
                monitor.signalAll();
            }
        } finally {
            monitor.unlock();
        }
    }

    @Override
    public final void socketError() {
        // ignore, handled by connection.
    }

    /**
     * Transmits the given request and returns the received response.
     *
     * @param request the request to transmit
     * @param timeout status timeout
     * @return the received response, never null
     * @throws TimeoutException if the timeout has elapsed before a response was received
     */
    final CastMessage request(final CastMessage request, final Duration timeout) throws TimeoutException {
        monitor.lock();
        try {
            channel.addListener(this, request.getNamespace());
            req = request;
            receivedResponse = false;
            response = null;
            channel.send(request);
            monitor.await(timeout);
        } finally {
            channel.removeListener(this);
            monitor.unlock();
        }
        if (response == null) {
            throw new TimeoutException("No response received within " + timeout);
        }
        return response;
    }

}
