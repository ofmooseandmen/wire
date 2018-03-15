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
import java.util.function.Predicate;

import io.omam.wire.CastChannel.CastMessage;

/**
 * Base application controller.
 * <p>
 * This class is intended to be extended to implement the application protocol.
 *
 * @see CastDeviceController#launchApp(String, java.util.function.Function, java.time.Duration)
 * @see CastDeviceController#stopApp(ApplicationController, Duration)
 */
public abstract class ApplicationController implements ChannelListener {

    /** the details of this application. */
    private final Application details;

    /** communication channel with the application. */
    private CastV2Channel channel;

    /** {@code Predicate} that tests whether a message is a response. */
    private final Predicate<CastMessage> isResp;

    /** {@code true} when {@link #stopped()} has been invoked. */
    private boolean stopped;

    /**
     * Constructor.
     *
     * @param someDetails the details of this application
     * @param isResponse a {@code Predicate} that tests whether a message is a response
     */
    protected ApplicationController(final Application someDetails, final Predicate<CastMessage> isResponse) {
        details = someDetails;
        isResp = isResponse;
    }

    /**
     * Returns the details of this application.
     * <p>
     * {@link Application#sessionId()} must be used to
     * {@link CastDeviceController#stopApp(ApplicationController, java.time.Duration) stop} this application.
     *
     * @return the details of this application
     */
    public final Application details() {
        return details;
    }

    @Override
    public final void messageReceived(final CastMessage message) {
        if (!isResp.test(message)
            && details.namespaces().stream().anyMatch(n -> n.name().equals(message.getNamespace()))) {
            appMessageReceived(message);
        }
    }

    @Override
    public final void socketError() {
        // ignore, handled by connection.
    }

    /**
     * Invoked when a message from the application has been received.
     * <p>
     * This method is invoked only with messages of the {@link Application#namespaces() application namespaces}
     * which are not responses to requests.
     *
     * @param message application message
     */
    protected abstract void appMessageReceived(final CastMessage message);

    /**
     * Sends the given request and await for a correlated response using the given correlator.
     *
     * @param request request
     * @param correlator a {@code BiPredicate} that accepts a request and a received message and returns
     *            {@code true} if that latter is correlated with the request
     * @param timeout response timeout
     * @return received response
     * @throws TimeoutException if the timeout elapsed before the response was received
     */
    protected final CastMessage request(final CastMessage request,
            final BiPredicate<CastMessage, CastMessage> correlator, final Duration timeout)
            throws TimeoutException {
        ensureNotStopped();
        return new Requestor(channel, correlator).request(request, timeout);
    }

    /**
     * Sends the given request and await for a correlated response using the standard request ID mechanism.
     * <p>
     * This method assumes that both the request and response JSON payload contain a {@code requestId} attribute.
     *
     * @param request request
     * @param timeout response timeout
     * @return received response
     * @throws TimeoutException if the timeout elapsed before the response was received
     */
    protected final CastMessage request(final CastMessage request, final Duration timeout)
            throws TimeoutException {
        ensureNotStopped();
        return new StandardRequestor(channel).request(request, timeout);
    }

    /**
     * Sends a message (i.e. not expecting a response) to the application.
     *
     * @param message message
     */
    protected final void send(final CastMessage message) {
        ensureNotStopped();
        channel.send(message);
    }

    /**
     * Sets the communication channel with the application.
     *
     * @param aChannel channel
     */
    final void setChannel(final CastV2Channel aChannel) {
        ensureNotStopped();
        channel = aChannel;
        details.namespaces().forEach(ns -> channel.addListener(this, ns.name()));
    }

    /**
     * Invoked when the application has been stopped.
     * <p>
     * This controller is no longer usable.
     */
    final void stopped() {
        stopped = true;
        channel.removeListener(this);
    }

    /**
     * Throws an {@link IllegalStateException} if stopped.
     */
    private void ensureNotStopped() {
        if (stopped) {
            throw new IllegalStateException("Application has been stopped");
        }
    }

}
