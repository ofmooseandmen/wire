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

import static io.omam.wire.device.DeviceProperties.USE_TLS;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.omam.wire.io.CastChannel.CastMessage;
import io.omam.wire.io.CastMessageCodec;

/**
 * TCP Channel to communicate with a Cast device over the Cast V2 protocol.
 */
final class CastV2Channel {

    /**
     * {@link ChannelListener} that notifies messages only for a given namespace.
     */
    private static final class ByNamespaceListener implements ChannelListener {

        /** namespace. */
        private final String ns;

        /** decorated listener. */
        private final ChannelListener l;

        /**
         * Constructor.
         *
         * @param namespace namespace
         * @param listener decorated listener
         */
        ByNamespaceListener(final String namespace, final ChannelListener listener) {
            ns = namespace;
            l = listener;
        }

        @Override
        public final void messageReceived(final CastMessage message) {
            if (message.getNamespace().equals(ns)) {
                l.messageReceived(message);
            }
        }

        @Override
        public final void socketError() {
            l.socketError();
        }

        /**
         * @return the decorated listener.
         */
        final ChannelListener listener() {
            return l;
        }

    }

    /**
     * Google Cast's certificate cannot be validated against standard keystore, so use a dummy trust-all manager.
     */
    private static final class CastX509TrustManager implements X509TrustManager {

        /**
         * Constructor.
         */
        CastX509TrustManager() {
            // empty.
        }

        @Override
        public final void checkClientTrusted(final X509Certificate[] certs, final String authType) {
            // always trusted.
        }

        @Override
        public final void checkServerTrusted(final X509Certificate[] certs, final String authType) {
            // always trusted.
        }

        @Override
        public final X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    /**
     * Thread to notify listener about received messages and socket errors.
     */
    private final class Notifier extends Thread {

        /**
         * Constructor.
         */
        Notifier() {
            super("wire-channel-notifier");
        }

        @SuppressWarnings("synthetic-access")
        @Override
        public final void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    final Consumer<ChannelListener> event = receivingQueue.take();
                    listeners.forEach(event);
                } catch (final InterruptedException e) {
                    LOGGER.log(Level.FINE, "Interrupted while waiting to notify listeners", e);
                    Thread.currentThread().interrupt();
                }
            }
        }

    }

    /**
     * Cast message receiver.
     * <p>
     * Listeners will be notified of the received message.
     */
    private final class Receiver extends Thread {

        /** whether the received is closed. */
        private boolean closed;

        /**
         * Constructor.
         */
        Receiver() {
            super("wire-channel-receiver");
            closed = false;
        }

        @SuppressWarnings({ "synthetic-access", "resource" })
        @Override
        public final void run() {
            while (!closed) {
                try {
                    final InputStream is = socket.getInputStream();
                    /* 4 first bytes is size of message. */
                    final Optional<CastMessage> optMsg = CastMessageCodec.read(is);
                    if (optMsg.isPresent()) {
                        final CastMessage msg = optMsg.get();
                        LOGGER.fine(() -> "Received message [" + msg + "]");
                        receivingQueue.add(l -> l.messageReceived(msg));
                    } else {
                        closed = true;
                    }
                } catch (final SSLException | SocketException e) {
                    if (!closed) {
                        LOGGER.log(level(e), "Socket error", e);
                        receivingQueue.add(ChannelListener::socketError);
                        closed = true;
                    }
                } catch (final IOException e) {
                    LOGGER.log(Level.WARNING, "I/O error while receiving message", e);
                }
            }
        }

        /**
         * Notifies this receiver that the socket is going to be closed.
         */
        final void close() {
            closed = true;
        }

    }

    /**
     * Cast message sender.
     * <p>
     * Messages are taken from the sending queue.
     */
    private final class Sender extends Thread {

        /**
         * Constructor.
         */
        Sender() {
            super("wire-channel-sender");
        }

        @SuppressWarnings("synthetic-access")
        @Override
        public final void run() {
            boolean error = false;
            while (!Thread.currentThread().isInterrupted() && !error) {
                try {
                    final CastMessage msg = sendingQueue.take();
                    doSend(msg);
                } catch (final SSLException | SocketException e) {
                    LOGGER.log(level(e), "Socket closed", e);
                    listeners.forEach(ChannelListener::socketError);
                    error = true;
                } catch (final IOException e) {
                    LOGGER.log(Level.WARNING, "I/O error while sending message", e);
                } catch (final InterruptedException e) {
                    LOGGER.log(Level.FINE, "Interrupted while waiting to send message", e);
                    Thread.currentThread().interrupt();
                }
            }
        }

    }

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(CastV2Channel.class.getName());

    /** how long to wait for the receiver/sender thread to die. */
    private static final long JOIN_WAIT = 1000L;

    /** device socket address. */
    private final InetSocketAddress ias;

    /** SSL context. */
    private final SSLContext sc;

    /** whether the channel is connected. */
    private boolean connected;

    /** TLS socket. */
    private Socket socket;

    /** received message listeners. */
    private final ConcurrentLinkedQueue<ByNamespaceListener> listeners;

    /** thread to notify listeners. */
    private Notifier notifier;

    /** thread receiving messages. */
    private Receiver receiver;

    /** thread sending messages. */
    private Sender sender;

    /** queue of messages to be sent. */
    private final BlockingQueue<CastMessage> sendingQueue;

    /** queue of received message/events. */
    private final BlockingQueue<Consumer<ChannelListener>> receivingQueue;

    /**
     * Constructor.
     *
     * @param address device socket address
     * @param sslContext secure socket protocol
     */
    private CastV2Channel(final InetSocketAddress address, final SSLContext sslContext) {
        ias = address;
        sc = sslContext;
        connected = false;
        socket = null;
        listeners = new ConcurrentLinkedQueue<>();
        notifier = null;
        receiver = null;
        sender = null;
        sendingQueue = new LinkedBlockingQueue<>();
        receivingQueue = new LinkedBlockingQueue<>();
    }

    /**
     * Creates a new TLS channel.
     *
     * @param address device socket address
     * @return a new TLS channel
     * @throws GeneralSecurityException in case of security error
     */
    static CastV2Channel create(final InetSocketAddress address) throws GeneralSecurityException {
        final SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, new TrustManager[] { new CastX509TrustManager() }, new SecureRandom());
        return new CastV2Channel(address, sc);
    }

    /**
     * Log level from I/O exception.
     *
     * @param e I/O exception
     * @return log level
     */
    private static Level level(final IOException e) {
        if (SSLException.class.isAssignableFrom(e.getClass())) {
            return Level.WARNING;
        }
        return Level.FINE;
    }

    /**
     * Interrupt given thread and wait for it to die.
     *
     * @param thread thread to interrupt
     */
    private static void terminate(final Thread thread) {
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(JOIN_WAIT);
            } catch (final InterruptedException e) {
                LOGGER.log(Level.FINE, "Interrupted while waiting for thread to die", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Adds the given listener to receive messages from the given namespace.
     *
     * @param listener listener
     * @param namespace namespace
     */
    final void addListener(final ChannelListener listener, final String namespace) {
        Objects.requireNonNull(listener);
        listeners.add(new ByNamespaceListener(namespace, listener));
    }

    /**
     * Returns the device socket address.
     *
     * @return the device socket address
     */
    final InetSocketAddress address() {
        return ias;
    }

    /**
     * Closes this channel by first sending the given message if not null. Subsequent {@link #connect()} is
     * possible.
     *
     * @param msg the message to send before closing the channel of null if not message to send.
     */
    final synchronized void close(final CastMessage msg) {
        LOGGER.fine("Closing channel");
        terminate(notifier);
        terminate(sender);
        if (receiver != null) {
            receiver.close();
        }
        sendingQueue.clear();
        receivingQueue.clear();
        if (msg != null) {
            try {
                doSend(msg);
            } catch (final IOException e) {
                LOGGER.log(Level.WARNING, "I/O error when sending CLOSE message", e);
            }
        }
        try {
            socket.close();
        } catch (final IOException e) {
            LOGGER.log(Level.WARNING, "I/O error when closing socket", e);
        } finally {
            connected = false;
        }
    }

    /**
     * Opens a connection to the device.
     *
     * @throws IOException in case of I/O error
     */
    final synchronized void connect() throws IOException {
        if (!connected) {
            LOGGER.fine(() -> "Connecting to " + ias);
            socket = createSocket();
            socket.connect(ias);
            notifier = new Notifier();
            sender = new Sender();
            receiver = new Receiver();
            receiver.start();
            sender.start();
            notifier.start();
            connected = true;
        }
    }

    /**
     * Removes the given listener so that it no longer receives messages.
     *
     * @param listener listener
     */
    final void removeListener(final ChannelListener listener) {
        Objects.requireNonNull(listener);
        listeners.removeIf(bnl -> bnl.listener().equals(listener));
    }

    /**
     * Adds the given message to the queue of messages to send.
     *
     * @param message message to send
     */
    final void send(final CastMessage message) {
        sendingQueue.add(message);
    }

    /**
     * Creates a new socket: either a SSL socket using the socket factory or a simple socket if
     * {@code io.omam.wire.ssl} is {@code false}.
     * <p>
     * This is hopefully a temporary workaround until server side authentication is implemented in the testing
     * framework.
     *
     * @return the created socket
     * @throws IOException in case of I/O error
     */
    @SuppressWarnings("resource")
    private Socket createSocket() throws IOException {
        return USE_TLS ? sc.getSocketFactory().createSocket() : new Socket();
    }

    /**
     * Immediately sends the given message.
     *
     * @param message message to send
     * @throws IOException in case of I/O error
     */
    @SuppressWarnings("resource")
    private void doSend(final CastMessage message) throws IOException {
        CastMessageCodec.write(message, socket.getOutputStream());
        LOGGER.fine(() -> "Sent message [" + message + "]");
    }

}
