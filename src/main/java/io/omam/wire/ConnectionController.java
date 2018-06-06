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

import static io.omam.wire.CastV2Protocol.DEFAULT_RECEIVER_ID;
import static io.omam.wire.CastV2Protocol.PING_INTERVAL;
import static io.omam.wire.CastV2Protocol.PONG_TIMEOUT;
import static io.omam.wire.CastV2Protocol.SENDER_ID;
import static io.omam.wire.Payloads.build;
import static io.omam.wire.Payloads.is;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.InvalidProtocolBufferException;

import io.omam.wire.CastChannel.AuthChallenge;
import io.omam.wire.CastChannel.CastMessage;
import io.omam.wire.CastChannel.DeviceAuthMessage;
import io.omam.wire.Payloads.Message;

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
final class ConnectionController implements ChannelListener, AutoCloseable {

    /**
     * Connect message.
     */
    private static final class Connect extends Connection {

        /** user agent. */
        @SuppressWarnings("unused")
        private final String userAgent;

        /**
         * Constructor.
         */
        Connect() {
            super("CONNECT");
            userAgent = "wire";
        }

    }

    /**
     * Connection message.
     */
    private static class Connection extends Message {

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

    /** PING message type. */
    private static final String PING = "PING";

    /** PONG message type. */
    private static final String PONG = "PONG";

    /** close message type. */
    private static final String CLOSE = "CLOSE";

    /** authentication namespace */
    private static final String AUTH_NS = "urn:x-cast:com.google.cast.tp.deviceauth";

    /** connection namespace. */
    private static final String CONNECTION_NS = "urn:x-cast:com.google.cast.tp.connection";

    /** heartbeat namepspace. */
    private static final String HEARTBEAT_NS = "urn:x-cast:com.google.cast.tp.heartbeat";

    /** the authentication message. */
    private static final CastMessage AUTHENTICATION;

    /** heartbeat PING message. */
    private static final CastMessage PING_MSG;

    /** connection connect message. */
    private static final CastMessage CONNECT_MSG;

    /** connection close message. */
    private static final CastMessage CLOSE_MSG;

    static {
        final DeviceAuthMessage authMessage =
                DeviceAuthMessage.newBuilder().setChallenge(AuthChallenge.newBuilder().build()).build();
        AUTHENTICATION = CastMessage
            .newBuilder()
            .setProtocolVersion(CastMessage.ProtocolVersion.CASTV2_1_0)
            .setSourceId(SENDER_ID)
            .setDestinationId(DEFAULT_RECEIVER_ID)
            .setNamespace(AUTH_NS)
            .setPayloadType(CastMessage.PayloadType.BINARY)
            .setPayloadBinary(authMessage.toByteString())
            .build();

        PING_MSG = build(HEARTBEAT_NS, new Message(PING, null));
        CONNECT_MSG = build(CONNECTION_NS, new Connect());
        CLOSE_MSG = build(CONNECTION_NS, new Connection(CLOSE));
    }

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(ConnectionController.class.getName());

    /** communication channel with the Cast device. */
    private final CastV2Channel channel;

    /** scheduled executor service. */
    private final ScheduledExecutorService ses;

    /** future that represents regular PING messages. */
    private Future<?> fPing;

    /** future that represents PONG timeout. */
    private Future<?> fTimeout;

    /** connection state. */
    private State state;

    /** monitors when connection has reached a final state. */
    private final Monitor monitor;

    /** listeners. */
    private final List<ConnectionListener> listeners;

    /**
     * Constructor.
     *
     * @param aChannel communication channel with the Cast device
     *
     */
    ConnectionController(final CastV2Channel aChannel) {
        channel = aChannel;
        ses = Executors.newSingleThreadScheduledExecutor(new WireThreadFactory("connection"));
        fPing = null;
        fTimeout = null;

        state = State.CLOSED;
        monitor = new Monitor(() -> state == State.OPENED);

        listeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Returns a new message representing a heartbeat {@code PONG} .
     *
     * @param destination PONG message destination
     * @return a new message representing a heartbeat {@code PONG}
     */
    private static CastMessage pong(final String destination) {
        return build(HEARTBEAT_NS, destination, new Message(PONG, null));
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
    public final void messageReceived(final CastMessage message) {
        monitor.lock();
        try {
            if (is(message, PONG)) {
                if (fTimeout != null) {
                    fTimeout.cancel(true);
                    fTimeout = null;
                }
                final boolean connecting = state == State.CONNECTING;
                state = State.OPENED;
                if (connecting) {
                    monitor.signalAll();
                }
            } else if (is(message, PING)) {
                channel.send(pong(message.getSourceId()));
            }
        } finally {
            monitor.unlock();
        }
    }

    @Override
    public final void socketError() {
        close(CastV2Channel::close, ConnectionListener::remoteConnectionClosed);
    }

    /**
     * Adds the given listener to receive connection events.
     *
     * @param listener listener, not null
     */
    final void addListener(final ConnectionListener listener) {
        Objects.requireNonNull(listener);
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
            final Requestor auth = new Requestor(channel, (req, resp) -> true);
            final long start = System.nanoTime();
            final CastMessage authResp = auth.request(AUTHENTICATION, timeout);
            try {
                if (DeviceAuthMessage.parseFrom(authResp.getPayloadBinary()).hasError()) {
                    LOGGER.warning("Failed to authenticate with Cast device");
                    throw new IOException("Failed to authenticate with Cast device");
                }
            } catch (final InvalidProtocolBufferException e) {
                LOGGER.log(Level.WARNING, e, () -> "Failed to parse authentication message");
                throw new IOException(e);
            }

            monitor.lock();
            try {
                channel.addListener(this, CONNECTION_NS);
                channel.addListener(this, HEARTBEAT_NS);
                LOGGER.fine(() -> "Received authentication response, connecting...");
                /* OK to connect. */
                /* ping the device. */
                channel.send(PING_MSG);

                /* send connection message. */
                channel.send(CONNECT_MSG);

                /* start heartbeat. */
                fPing = ses.scheduleAtFixedRate(this::sendPing, PING_INTERVAL.toMillis(), PING_INTERVAL.toMillis(),
                        TimeUnit.MILLISECONDS);
                final Duration connectionTimeout = timeout.minus(System.nanoTime() - start, ChronoUnit.NANOS);
                monitor.await(connectionTimeout);
            } finally {
                monitor.unlock();
            }
        }
        final boolean opened = isOpened();
        LOGGER.fine(() -> "Connection opened? " + opened);
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
     * Removes the given listener so that it no longer receives connection events.
     *
     * @param listener listener, not null
     */
    final void removeListener(final ConnectionListener listener) {
        Objects.requireNonNull(listener);
        listeners.remove(listener);
    }

    /**
     * Closes the connection with the Cast device.
     *
     * @param closer method to close the channel
     * @param notifier method to notify the listener
     */
    private final void close(final Consumer<CastV2Channel> closer, final Consumer<ConnectionListener> notifier) {
        if (state != State.CLOSED) {
            channel.removeListener(this);
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
