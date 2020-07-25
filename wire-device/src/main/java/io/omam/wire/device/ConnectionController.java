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
package io.omam.wire.device;

import static io.omam.wire.device.DeviceProperties.PING_INTERVAL;
import static io.omam.wire.device.DeviceProperties.PONG_TIMEOUT;
import static io.omam.wire.io.IoProperties.DEFAULT_RECEIVER_ID;
import static io.omam.wire.io.json.Payloads.buildMessage;
import static io.omam.wire.io.json.Payloads.hasType;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.InvalidProtocolBufferException;

import io.omam.wire.app.ApplicationController;
import io.omam.wire.io.CastChannel.AuthChallenge;
import io.omam.wire.io.CastChannel.CastMessage;
import io.omam.wire.io.CastChannel.DeviceAuthMessage;
import io.omam.wire.io.NamespaceListener;
import io.omam.wire.io.json.Payload;

/**
 * {@code ConnectionController} implements the {@code urn:x-cast:com.google.cast.tp.deviceauth},
 * {@code urn:x-cast:com.google.cast.tp.connection} and {@code urn:x-cast:com.google.cast.tp.heartbeat}
 * namespaces/protocols to control the connection with the Cast device.
 * <p>
 * Once established the connection is kept alive by exchanging PING/PONG messages with the device. The connection
 * is closed if 3 consecutive PONG messages are missed or upon request.
 * <p>
 * This controller also answers any received PING message by a PONG to the appropriate destination.
 */
final class ConnectionController implements NamespaceListener, SocketErrorHandler, AutoCloseable {

    /**
     * Close message.
     */
    private static final class Close extends Connection {

        /** unique instance. */
        static final Close INSTANCE = new Close();

        /**
         * Constructor.
         */
        private Close() {
            super("CLOSE");
        }

    }

    /**
     * Connect message.
     */
    private static final class Connect extends Connection {

        /** unique instance. */
        static final Connect INSTANCE = new Connect();

        /** user agent. */
        @SuppressWarnings("unused")
        private final String userAgent;

        /**
         * Constructor.
         */
        private Connect() {
            super("CONNECT");
            userAgent = "wire";
        }

    }

    /**
     * Connection request payload.
     */
    private static abstract class Connection extends Payload {

        /** origin. */
        @SuppressWarnings("unused")
        private final Object origin;

        /**
         * Constructor.
         *
         * @param aType type
         */
        Connection(final String aType) {
            super(aType, null);
            origin = new Object();
        }

    }

    /**
     * Ping message payload.
     */
    private static final class Ping extends Payload {

        /** unique instance. */
        static final Ping INSTANCE = new Ping();

        /**
         * Constructor.
         */
        private Ping() {
            super("PING", null);
        }
    }

    /**
     * Pong message payload.
     */
    private static final class Pong extends Payload {

        /** unique instance. */
        static final Pong INSTANCE = new Pong();

        /**
         * Constructor.
         */
        private Pong() {
            super("PONG", null);
        }
    }

    /**
     * Connection state.
     */
    private enum State {
        /** connection is closed. */
        CLOSED,
        /** trying to connect. */
        CONNECTING,
        /** connection is opened. */
        OPENED;
    }

    /** connection namespace. */
    private static final String CONNECTION_NS = "urn:x-cast:com.google.cast.tp.connection";

    /** authentication namespace */
    private static final String AUTH_NS = "urn:x-cast:com.google.cast.tp.deviceauth";

    /** heartbeat namepspace. */
    private static final String HEARTBEAT_NS = "urn:x-cast:com.google.cast.tp.heartbeat";

    /** the authentication message. */
    private static final DeviceAuthMessage AUTHENTICATION;

    /** heartbeat PING message. */
    private static final CastMessage PING_MSG;

    /** connection connect message. */
    private static final CastMessage CONNECT_MSG;

    /** connection close message. */
    private static final CastMessage CLOSE_MSG;

    static {
        AUTHENTICATION = DeviceAuthMessage.newBuilder().setChallenge(AuthChallenge.newBuilder().build()).build();
        PING_MSG = buildMessage(HEARTBEAT_NS, DEFAULT_RECEIVER_ID, Ping.INSTANCE);
        CONNECT_MSG = buildMessage(CONNECTION_NS, DEFAULT_RECEIVER_ID, Connect.INSTANCE);
        CLOSE_MSG = buildMessage(CONNECTION_NS, DEFAULT_RECEIVER_ID, Close.INSTANCE);
    }

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(ConnectionController.class.getName());

    /** communication channel with the Cast device. */
    private final SocketChannel channel;

    /** requestor. */
    private final Requestor requestor;

    /** scheduled executor service. */
    private final ScheduledExecutorService ses;

    /** future that represents regular PING messages. */
    private Future<?> fPing;

    /** future that represents PONG timeout. */
    private Future<?> fTimeout;

    /** connection state. */
    private volatile State state;

    /** lock. */
    private final Lock lock;

    /** condition signaled when the connection is opened. */
    private final Condition connectionOpened;

    /** listeners. */
    private final ConcurrentLinkedQueue<ConnectionListener> listeners;

    /** set of application sessions. */
    private final Set<String> sessions;

    /**
     * Constructor.
     *
     * @param aChannel communication channel with the Cast device
     * @param aRequestor requestor
     */
    ConnectionController(final SocketChannel aChannel, final Requestor aRequestor) {
        channel = aChannel;
        requestor = aRequestor;
        ses = Executors.newSingleThreadScheduledExecutor(new WireThreadFactory("connection"));
        fPing = null;
        fTimeout = null;

        state = State.CLOSED;
        lock = new ReentrantLock();
        connectionOpened = lock.newCondition();

        listeners = new ConcurrentLinkedQueue<>();

        sessions = new HashSet<>();
    }

    /**
     * Returns a new message representing a heartbeat {@code PONG} .
     *
     * @param destination PONG message destination
     * @return a new message representing a heartbeat {@code PONG}
     */
    private static CastMessage pong(final String destination) {
        return buildMessage(HEARTBEAT_NS, destination, Pong.INSTANCE);
    }

    /**
     * Closes the connection with the Cast device.
     */
    @Override
    public final void close() {
        close(c -> c.close(CLOSE_MSG), l -> {
            // no listener to notify.
        });
    }

    @Override
    public final void socketError(final IOException e) {
        LOGGER.log(Level.INFO, "Closing connection after socket error", e);
        close(c -> c.close(null), ConnectionListener::remoteConnectionClosed);
    }

    @Override
    public final void uncorrelatedResponseReceived(final CastMessage message) {
        LOGGER.warning(() -> "Uncorrelated response received: " + message);
    }

    @Override
    public final void unsolicitedMessageReceived(final CastMessage message) {
        lock.lock();
        try {
            if (hasType(message, "PONG")) {
                if (fTimeout != null) {
                    fTimeout.cancel(true);
                    fTimeout = null;
                }
                final boolean connecting = state == State.CONNECTING;
                state = State.OPENED;
                if (connecting) {
                    LOGGER.info(() -> "Connection with device opened");
                    connectionOpened.signalAll();
                }
            } else if (hasType(message, "PING")) {
                channel.send(pong(message.getSourceId()));
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Adds the given listener to receive connection events.
     *
     * @param listener listener
     */
    final void addListener(final ConnectionListener listener) {
        listeners.add(listener);
    }

    /**
     * Connects to this Cast device. This method blocks until the connection has been established or the given
     * connection timeout has elapsed.
     *
     * @param timeout connection timeout
     * @throws IOException in case of I/O error (including authentication error)
     * @throws TimeoutException if the timeout has elapsed before the connection could be opened
     */
    final void connect(final Duration timeout) throws IOException, TimeoutException {
        if (state == State.CLOSED) {
            state = State.CONNECTING;
            channel.connect();
            /* authenticate. */
            final long start = System.nanoTime();
            /* auth response is assumed to be the first binary message received after sending the request. */
            final CastMessage authResp =
                    requestor.sendBinaryRequest(AUTH_NS, AUTHENTICATION, (rep, req) -> true, timeout);
            try {
                if (DeviceAuthMessage.parseFrom(authResp.getPayloadBinary()).hasError()) {
                    LOGGER.warning("Failed to authenticate with Cast device");
                    throw new IOException("Failed to authenticate with Cast device");
                }
            } catch (final InvalidProtocolBufferException e) {
                LOGGER.log(Level.WARNING, e, () -> "Failed to parse authentication message");
                throw new IOException(e);
            }

            lock.lock();
            try {
                channel.addNamespaceListener(this, CONNECTION_NS);
                channel.addNamespaceListener(this, HEARTBEAT_NS);
                LOGGER.info(() -> "Received authentication response, connecting...");
                /* OK to connect. */
                /* ping the device. */
                channel.send(PING_MSG);

                /* send connection message. */
                channel.send(CONNECT_MSG);

                /* start heartbeat. */
                fPing = ses
                    .scheduleAtFixedRate(this::sendPing, PING_INTERVAL.toMillis(), PING_INTERVAL.toMillis(),
                            TimeUnit.MILLISECONDS);
                final Duration connectionTimeout = timeout.minus(System.nanoTime() - start, ChronoUnit.NANOS);
                awaitOpen(connectionTimeout);
            } finally {
                lock.unlock();
            }
        }
        final boolean opened = isOpened();
        LOGGER.info(() -> "Connection opened? " + opened);
        if (!opened) {
            LOGGER.warning("Failed to open connection with Cast device");
            throw new IOException("Failed to open connection with Cast device");
        }
    }

    /**
     * @return {@code true} iff connected.
     */
    final boolean isOpened() {
        return state == State.OPENED;
    }

    /**
     * Joins an application session. Messages can be sent to device to control the application using an instance of
     * {@link ApplicationController}.
     *
     * @param transpordId the application transport ID
     */
    final void joinAppSession(final String transpordId) {
        synchronized (sessions) {
            if (!sessions.contains(transpordId)) {
                channel.send(buildMessage(CONNECTION_NS, transpordId, Connect.INSTANCE));
                sessions.add(transpordId);
            }
        }
    }

    /**
     * Leaves an application session. Messages can no longer be sent to device to control the application.
     *
     * @param transpordId the application transport ID
     */
    final void leaveAppSession(final String transpordId) {
        synchronized (sessions) {
            channel.send(buildMessage(CONNECTION_NS, transpordId, Close.INSTANCE));
            sessions.remove(transpordId);
        }
    }

    /**
     * Removes the given listener so that it no longer receives connection events.
     *
     * @param listener listener
     */
    final void removeListener(final ConnectionListener listener) {
        listeners.remove(listener);
    }

    /**
     * Await until the connection is opened or the given timeout has elapsed which ever occurs first.
     *
     * @param timeout how long to wait before giving up
     */
    private void awaitOpen(final Duration timeout) {
        long nanos = timeout.toNanos();
        lock.lock();
        try {
            while (state != State.OPENED) {
                if (nanos <= 0L) {
                    return;
                }
                nanos = connectionOpened.awaitNanos(nanos);
            }
        } catch (final InterruptedException e) {
            LOGGER.log(Level.WARNING, "Interrupted while waiting for connection to be opened", e);
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Closes the connection with the Cast device.
     *
     * @param closer method to close the channel
     * @param notifier method to notify the listener
     */
    private final void close(final Consumer<SocketChannel> closer, final Consumer<ConnectionListener> notifier) {
        if (state != State.CLOSED) {
            channel.removeNamespaceListener(this);
            if (fPing != null) {
                fPing.cancel(true);
            }
            if (fTimeout != null) {
                fTimeout.cancel(true);
            }
            /* close the channel. */
            closer.accept(channel);

            state = State.CLOSED;

            /* notify listeners. */
            listeners.forEach(notifier::accept);
        }
    }

    /**
     * Sends a PING message to the Cast device and starts the timeout for the PONG response.
     */
    private void sendPing() {
        channel.send(PING_MSG);
        if (fTimeout == null) {
            fTimeout = ses.schedule(this::timeout, PONG_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Closes after timeout.
     */
    private void timeout() {
        LOGGER.info("Closing connection after heartbeat timeout");
        close(c -> c.close(CLOSE_MSG), ConnectionListener::connectionDead);
    }
}
