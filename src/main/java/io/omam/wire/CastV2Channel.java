/**
 * Copyright (c) Thales Air Systems SA 2018-2018 all rights reserved.
 * This software is the property of Thales Air Systems SA
 * and may not be used in any manner
 * except under a license agreement signed with Thales Air Systems SA.
 */
package io.omam.wire;

import static io.omam.wire.CastV2Protocol.USE_TLS;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.omam.wire.CastChannel.CastMessage;

/**
 * TCP Channel to communicate with a Cast device over the CAST V2 protocol.
 */
@SuppressWarnings("javadoc")
final class CastV2Channel implements AutoCloseable {

    private static final class ByNamespaceListener implements ChannelListener {

        private final String ns;

        private final ChannelListener l;

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
     * Cast message receiver.
     * <p>
     * Listeners will be notified of the received message.
     */
    private final class Receiver implements Runnable {

        /**
         * Constructor.
         */
        Receiver() {
            // empty.
        }

        @SuppressWarnings("synthetic-access")
        @Override
        public final void run() {
            boolean endOfStream = false;
            while (!endOfStream && !Thread.currentThread().isInterrupted()) {
                try {
                    final InputStream is = socket.getInputStream();
                    /* 4 first bytes is size of message. */
                    final Optional<CastMessage> optMsg = CastMessageCodec.read(is);
                    if (optMsg.isPresent()) {
                        final CastMessage msg = optMsg.get();
                        LOGGER.fine(() -> "Received message [" + msg + "]");
                        listeners.forEach(l -> l.messageReceived(msg));
                    } else {
                        endOfStream = true;
                    }
                } catch (final SSLHandshakeException | SocketException e) {
                    LOGGER.log(Level.FINE, "Socket closed", e);
                    listeners.forEach(ChannelListener::socketError);
                    break;
                } catch (final IOException e) {
                    LOGGER.log(Level.WARNING, "I/O error while receiving message", e);
                }
            }
        }

    }

    /**
     * Cast message sender.
     * <p>
     * Messages are taken from the sending queue.
     */
    private final class Sender implements Runnable {

        /**
         * Constructor.
         */
        Sender() {
            // empty.
        }

        @SuppressWarnings("synthetic-access")
        @Override
        public final void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    final CastMessage msg = sq.take();
                    doSend(msg);
                } catch (final SocketException e) {
                    LOGGER.log(Level.FINE, "Socket closed", e);
                    listeners.forEach(ChannelListener::socketError);
                    break;
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

    /** device socket address. */
    private final InetSocketAddress ias;

    /** SSL context. */
    private final SSLContext sc;

    /** TLS socket. */
    private Socket socket;

    /** executor service to send/receive messages. */
    private ExecutorService es;

    /** received message listeners. */
    private final List<ByNamespaceListener> listeners;

    /** future to cancel receiving messages. */
    private Future<?> receiver;

    /** future to cancel sending messages. */
    private Future<?> sender;

    /** queue of messages to be sent. */
    private final BlockingQueue<CastMessage> sq;

    /**
     * Constructor.
     *
     * @param address device address
     * @param port device port
     */
    private CastV2Channel(final InetAddress address, final int port, final SSLContext sslContext) {
        ias = new InetSocketAddress(address, port);
        sc = sslContext;
        socket = null;
        es = null;
        listeners = new CopyOnWriteArrayList<>();
        receiver = null;
        sender = null;
        sq = new LinkedBlockingQueue<>();
    }

    /**
     * Creates a new TLS channel.
     *
     * @param address device address
     * @param port device port
     * @return a new TLS channel
     * @throws GeneralSecurityException in case of security error
     */
    static CastV2Channel create(final InetAddress address, final int port) throws GeneralSecurityException {
        final SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, new TrustManager[] { new CastX509TrustManager() }, new SecureRandom());
        return new CastV2Channel(address, port, sc);
    }

    @Override
    public final synchronized void close() {
        LOGGER.fine("Closing channel");
        shutdown();
        closeSocket();
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
     * Closes this channel by first sending the given message.
     *
     * @param msg the message to send before closing the channel
     */
    final synchronized void close(final CastMessage msg) {
        LOGGER.fine("Closing channel with message");
        shutdown();
        try {
            doSend(msg);
        } catch (final IOException e) {
            LOGGER.log(Level.WARNING, "I/O error when sending CLOSE message", e);
        }
        closeSocket();
    }

    final synchronized void connect() throws IOException {
        if (socket == null) {
            LOGGER.fine(() -> "Connecting to " + ias);
            socket = createSocket();
            socket.connect(ias);
            es = Executors.newScheduledThreadPool(2, new WireThreadFactory("channel"));
            sender = es.submit(new Sender());
            receiver = es.submit(new Receiver());
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
        sq.add(message);
    }

    /**
     * Closes the socket.
     */
    private void closeSocket() {
        try {
            socket.close();
        } catch (final IOException e) {
            LOGGER.log(Level.WARNING, "I/O error when closing socket", e);
        } finally {
            socket = null;
        }
    }

    /**
     * Creates a new socket: either a SSL socket using the socket factory or a simple socket if
     * {@code io.omam.wire.ssl} is {@code false}.
     * <p>
     * This is hopefully a temporary workaround until server side authentication is implemented in the testing
     * framework.
     *
     * @return
     * @throws IOException in case of I/O error
     */
    private Socket createSocket() throws IOException {
        return USE_TLS ? sc.getSocketFactory().createSocket() : new Socket();
    }

    /**
     * Immediately sends the given message.
     *
     * @param message message to send
     * @throws IOException in case of I/O error
     */
    private void doSend(final CastMessage message) throws IOException {
        LOGGER.fine(() -> "Sending message [" + message + "]");
        CastMessageCodec.write(message, socket.getOutputStream());
        LOGGER.fine(() -> "Sent message [" + message + "]");
    }

    /**
     * Shutdowns background sender and receiver.
     */
    private void shutdown() {
        sq.clear();
        if (sender != null) {
            sender.cancel(true);
            sender = null;
        }
        if (receiver != null) {
            receiver.cancel(true);
            receiver = null;
        }
        es.shutdownNow();
        try {
            es.awaitTermination(1, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            LOGGER.log(Level.WARNING, "Interrupted while waiting for scheduler termination", e);
            Thread.currentThread().interrupt();
        }
        es = null;
    }
}
