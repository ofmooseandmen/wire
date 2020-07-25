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
package io.omam.wire.device;

import static io.omam.wire.io.IoProperties.DEFAULT_RECEIVER_ID;
import static io.omam.wire.io.IoProperties.SENDER_ID;
import static io.omam.wire.io.json.Payloads.parse;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.MessageLite;

import io.omam.wire.io.CastChannel.CastMessage;
import io.omam.wire.io.CastChannel.CastMessage.PayloadType;
import io.omam.wire.io.json.Payload;
import io.omam.wire.io.json.Payloads;

/**
 * A {@code Requestor} transmits requests to the device and awaits for correlated responses.
 */
final class Requestor implements ResponseHandler {

    /**
     * {@link PendingRequest} base implementation.
     *
     * @param <T> type of the correlation ID
     */
    private static abstract class BasePendingRequest<T> implements PendingRequest<T> {

        /** lock. */
        private final Lock lock;

        /** condition signaled when a response is received. */
        private final Condition responeReceived;

        /** received response or null. */
        private volatile CastMessage response;

        /**
         * Constructor.
         */
        protected BasePendingRequest() {
            lock = new ReentrantLock();
            responeReceived = lock.newCondition();
            response = null;
        }

        @SuppressWarnings("synthetic-access")
        @Override
        public final void awaitResponse(final Duration timeout) {
            long nanos = timeout.toNanos();
            lock.lock();
            try {
                while (response == null) {
                    if (nanos <= 0L) {
                        return;
                    }
                    nanos = responeReceived.awaitNanos(nanos);
                }
            } catch (final InterruptedException e) {
                LOGGER.log(Level.WARNING, "Interrupted while waiting for response", e);
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }

        @SuppressWarnings("synthetic-access")
        @Override
        public final void notifyResponseReceived(final CastMessage message) {
            lock.lock();
            try {
                LOGGER.fine(() -> "Received response [" + message + "] answering request");
                response = message;
                responeReceived.signalAll();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public final CastMessage response() {
            return response;
        }

    }

    /**
     * Pending binary request.
     */
    private static final class PendingBinaryRequest extends BasePendingRequest<CastMessage> {

        /** transmitted request. */
        private final CastMessage request;

        /** request/response correlator. */
        private final BiPredicate<CastMessage, CastMessage> correlator;

        /**
         * Constructor.
         *
         * @param aRequest transmitted request
         * @param aCorrelator request/response correlator
         */
        PendingBinaryRequest(final CastMessage aRequest, final BiPredicate<CastMessage, CastMessage> aCorrelator) {
            request = aRequest;
            correlator = aCorrelator;
        }

        @Override
        public final boolean responseMatches(final CastMessage response) {
            return correlator.test(request, response);
        }

    }

    /**
     * Represents a request that has been transmitted and for which a response has not yet been received.
     *
     * @param <T> type of the correlation ID
     */
    private static interface PendingRequest<T> {

        /**
         * Await at most given timeout for a response to be received.
         *
         * @param timeout how long to wait for
         */
        void awaitResponse(final Duration timeout);

        /**
         * Notifies this request that a response has been received.
         *
         * @param message the response
         */
        void notifyResponseReceived(final CastMessage message);

        /**
         * @return the received response or null.
         */
        CastMessage response();

        /**
         * Determines if the given correlation ID matches this request.
         *
         * @param correlationId correlation ID
         * @return true if the given correlation ID matches this request
         */
        boolean responseMatches(final T correlationId);
    }

    /**
     * Pending UTF8 request.
     */
    private static final class PendingUt8Request extends BasePendingRequest<Integer> {

        /** request ID. */
        private final Integer requestId;

        /**
         * Constructor.
         *
         * @param aRequestId request ID
         */
        PendingUt8Request(final Integer aRequestId) {
            requestId = aRequestId;
        }

        @Override
        public final boolean responseMatches(final Integer aRequestId) {
            return Objects.equals(requestId, aRequestId);
        }

    }

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(Requestor.class.getName());

    /** channel to transmit requests and receive messages. */
    private final SocketChannel channel;

    /** pending binary request. */
    private final ConcurrentLinkedQueue<PendingRequest<CastMessage>> binaryRequests;

    /** pending UTF8 request. */
    private final ConcurrentLinkedQueue<PendingRequest<Integer>> utf8Requests;

    /** request id counter. */
    private final AtomicInteger nextRequestId;

    /**
     * Constructor.
     *
     * @param aChannel channel to transmit requests and receive messages
     */
    Requestor(final SocketChannel aChannel) {
        channel = aChannel;
        binaryRequests = new ConcurrentLinkedQueue<>();
        utf8Requests = new ConcurrentLinkedQueue<>();
        nextRequestId = new AtomicInteger(0);
    }

    /**
     * Tries and correlates the given message and correlation ID with one of the given pending requests.
     *
     * @param <T> type of the correlation ID
     * @param message received message
     * @param correlationId correlation ID
     * @param requests pending request
     * @return correlation result
     */
    private static <T> CorrelationResult tryCorrelate(final CastMessage message, final T correlationId,
            final Collection<PendingRequest<T>> requests) {
        final Iterator<PendingRequest<T>> it = requests.iterator();
        while (it.hasNext()) {
            final PendingRequest<T> pr = it.next();
            if (pr.responseMatches(correlationId)) {
                pr.notifyResponseReceived(message);
                it.remove();
                return CorrelationResult.CORRELATED;
            }
        }
        return CorrelationResult.UNCORRELATED;
    }

    @Override
    public final CorrelationResult tryCorrelate(final CastMessage message) {
        if (message.getPayloadType() == PayloadType.BINARY) {
            return tryCorrelate(message, message, binaryRequests);
        }
        try {
            return parse(message)
                .requestId()
                .map(ri -> tryCorrelate(message, ri, utf8Requests))
                .orElse(CorrelationResult.UNSOLICITED);
        } catch (final IOException e) {
            LOGGER.log(Level.WARNING, "Could not parse received message", e);
            return CorrelationResult.UNSOLICITED;
        }

    }

    /**
     * Transmits the given binary request and returns the received response.
     *
     * @param namespace request namespace
     * @param payload request payload
     * @param correlator request/response correlator
     * @param timeout response timeout
     * @return the received response, never null
     * @throws TimeoutException if the timeout has elapsed before a response was received
     */
    final CastMessage sendBinaryRequest(final String namespace, final MessageLite payload,
            final BiPredicate<CastMessage, CastMessage> correlator, final Duration timeout)
            throws TimeoutException {
        return sendBinaryRequest(namespace, DEFAULT_RECEIVER_ID, payload, correlator, timeout);
    }

    /**
     * Transmits the given binary request using the {@link io.omam.wire.io.IoProperties#DEFAULT_RECEIVER_ID default
     * receiver ID} and returns the received response.
     *
     * @param namespace request namespace
     * @param destination request destination
     * @param payload request payload
     * @param correlator request/response correlator
     * @param timeout response timeout
     * @return the received response, never null
     * @throws TimeoutException if the timeout has elapsed before a response was received
     */
    final CastMessage sendBinaryRequest(final String namespace, final String destination,
            final MessageLite payload, final BiPredicate<CastMessage, CastMessage> correlator,
            final Duration timeout) throws TimeoutException {
        final CastMessage request = CastMessage
            .newBuilder()
            .setProtocolVersion(CastMessage.ProtocolVersion.CASTV2_1_0)
            .setSourceId(SENDER_ID)
            .setDestinationId(destination)
            .setNamespace(namespace)
            .setPayloadType(CastMessage.PayloadType.BINARY)
            .setPayloadBinary(payload.toByteString())
            .build();
        final PendingBinaryRequest pending = new PendingBinaryRequest(request, correlator);
        binaryRequests.add(pending);
        try {
            return sendRequest(request, pending, timeout);
        } finally {
            binaryRequests.remove(pending);
        }
    }

    /**
     * Transmits the given UTF8 request using the {@link io.omam.wire.io.IoProperties#DEFAULT_RECEIVER_ID default
     * receiver ID} and returns the received response - correlation is achieved using the standard request ID
     * mechanism.
     *
     * @param namespace request namespace
     * @param payload request payload
     * @param timeout response timeout
     * @return the received response, never null
     * @throws TimeoutException if the timeout has elapsed before a response was received
     */
    final CastMessage sendUtf8Request(final String namespace, final Payload payload, final Duration timeout)
            throws TimeoutException {
        return sendUtf8Request(namespace, DEFAULT_RECEIVER_ID, payload, timeout);
    }

    /**
     * Transmits the given UTF8 request and returns the received response - correlation is achieved using the
     * standard request ID mechanism.
     *
     * @param namespace request namespace
     * @param destination request destination
     * @param payload request payload
     * @param timeout response timeout
     * @return the received response, never null
     * @throws TimeoutException if the timeout has elapsed before a response was received
     */
    final CastMessage sendUtf8Request(final String namespace, final String destination, final Payload payload,
            final Duration timeout) throws TimeoutException {
        final int requestId = nextRequestId.incrementAndGet();
        final CastMessage request = Payloads.buildRequest(namespace, destination, payload, requestId);
        final PendingUt8Request pending = new PendingUt8Request(requestId);
        try {
            utf8Requests.add(pending);
            return sendRequest(request, pending, timeout);
        } finally {
            utf8Requests.remove(pending);
        }
    }

    /**
     * Transmits the given request and returns the received response.
     *
     * @param <T> type of the correlation ID
     * @param request request
     * @param pending pending request
     * @param timeout response timeout
     * @return the received response, never null
     * @throws TimeoutException if the timeout has elapsed before a response was received
     */
    private <T> CastMessage sendRequest(final CastMessage request, final PendingRequest<T> pending,
            final Duration timeout) throws TimeoutException {
        channel.send(request);
        pending.awaitResponse(timeout);
        if (pending.response() == null) {
            final String msg = "No response received within " + timeout;
            LOGGER.info(msg);
            throw new TimeoutException(msg);
        }
        return pending.response();
    }

}
