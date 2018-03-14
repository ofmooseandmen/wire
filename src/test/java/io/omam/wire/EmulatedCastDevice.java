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
import static io.omam.wire.CastV2Protocol.FRIENDLY_NAME;
import static io.omam.wire.CastV2Protocol.REGISTRATION_TYPE;
import static io.omam.wire.Payloads.build;
import static io.omam.wire.Payloads.is;
import static io.omam.wire.Payloads.parse;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;

import io.omam.halo.Attributes;
import io.omam.halo.Service;
import io.omam.wire.CastChannel.AuthError;
import io.omam.wire.CastChannel.AuthError.ErrorType;
import io.omam.wire.CastChannel.CastMessage;
import io.omam.wire.CastChannel.DeviceAuthMessage;
import io.omam.wire.CastDeviceVolume.VolumeControlType;
import io.omam.wire.Payloads.Message;
import io.omam.wire.ReceiverController.CastDeviceVolumeData;
import io.omam.wire.ReceiverController.ReceiverStatus;
import io.omam.wire.ReceiverController.SetVolumeLevel;
import io.omam.wire.ReceiverController.SetVolumeMuted;

/**
 * An emulated Cast device that implements the Cast V2 Protocol an behaves as closely as possible as an real device
 * (audio) running 1.30 firmware.
 */
final class EmulatedCastDevice implements AutoCloseable {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(EmulatedCastDevice.class.getName());

    /** authentication namespace/protocol. */
    private static final String AUTH_NS = "urn:x-cast:com.google.cast.tp.deviceauth";

    /** heartbeat namepspace. */
    private static final String HEARTBEAT_NS = "urn:x-cast:com.google.cast.tp.heartbeat";

    /** receiver namespace. */
    static final String RECEIVER_NS = "urn:x-cast:com.google.cast.receiver";

    @SuppressWarnings("javadoc")
    private static final int PORT = 8009;

    @SuppressWarnings("javadoc")
    private static final Inet4Address ADDR;

    /** authentication error. */
    private static final ByteString AUTH_ERR;

    /** authentication OK. */
    private static final ByteString AUTH_OK;

    /** the friendly name of this emulated device. */
    static final String NAME = "Emulated Cast Device";

    static {
        AUTH_ERR = DeviceAuthMessage
            .newBuilder()
            .setError(AuthError.newBuilder().setErrorType(ErrorType.INTERNAL_ERROR).build())
            .build()
            .toByteString();
        AUTH_OK = DeviceAuthMessage.newBuilder().build().toByteString();
        try {
            ADDR = (Inet4Address) InetAddress.getByName("127.0.0.1");
        } catch (final UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    /** JSON request ID property. */
    private static final String REQUEST_ID = "requestId";

    /** Gson instance. */
    private static final Gson GSON = new Gson();

    /** the named service corresponding to this device */
    private final Service service;

    /** queue of received messages. */
    private final BlockingQueue<CastMessage> receivedMessages;

    /** server socket. */
    private final ServerSocket serverSocket;

    /** reader thread. */
    private final Thread readerThread;

    /** socket, created when a connection is made by the client. */
    private Socket socket;

    /** when {@code true} all authentication requests are rejected. */
    private boolean rejectAllAuthenticationRequests;

    /** {@code true} if communications with the client are suspended. */
    private boolean suspended;

    /**
     * Constructor.
     */
    EmulatedCastDevice() {
        service = Service
            .create(NAME, REGISTRATION_TYPE, PORT)
            .hostname("localhost")
            .ipv4Address(ADDR)
            .attributes(Attributes.create().with(FRIENDLY_NAME, NAME, StandardCharsets.UTF_8).get())
            .get();
        rejectAllAuthenticationRequests = false;
        suspended = false;
        receivedMessages = new LinkedBlockingQueue<>();
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(ADDR, PORT));

            readerThread = new Thread(this::handleConnection);
            readerThread.start();

        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public final void close() {
        if (readerThread != null) {
            readerThread.interrupt();
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (final IOException e) {
                LOGGER.log(Level.WARNING, "I/O error when closing server socket", e);
            }
        }
    }

    /**
     * Indicates that all authentication requests shall be rejected.
     */
    final void rejectAllAuthenticationRequests() {
        rejectAllAuthenticationRequests = true;
    }

    /**
     * @return the named service corresponding to this device.
     */
    final Service service() {
        return service;
    }

    /**
     * Suspends communications with the client.
     */
    final void suspend() {
        suspended = true;
    }

    /**
     * Retrieves and remove the oldest received message waiting if necessary until available.
     *
     * @param timeout timeout
     * @return first received message or empty if timeout has elapsed
     * @throws InterruptedException if interrupted while waiting for a message to become available
     */
    final Optional<CastMessage> takeReceivedMessage(final Duration timeout) throws InterruptedException {
        return Optional.ofNullable(receivedMessages.poll(timeout.toMillis(), TimeUnit.MILLISECONDS));
    }

    /**
     * Await for a connection to be opened, and then handles received messages.
     */
    private void handleConnection() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                socket = serverSocket.accept();
                receivedMessages.clear();
                handleMessages();
            }
        } catch (final SocketException e) {
            LOGGER.log(Level.FINE, "socket closed", e);
        } catch (final IOException e) {
            LOGGER.log(Level.SEVERE, "I/O error during message reception, exiting", e);
        }
    }

    /**
     * Handles messages from the client: try to respond when appropriate.
     *
     * @throws IOException in case of I/O error
     */
    private void handleMessages() throws IOException {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                final InputStream is = socket.getInputStream();
                final CastMessage message = CastMessageCodec.read(is).orElseThrow(
                        () -> new IOException("Could not read received message"));
                receivedMessages.add(message);
                if (message.getNamespace().equals(AUTH_NS)) {
                    final ByteString resp = rejectAllAuthenticationRequests ? AUTH_ERR : AUTH_OK;
                    final CastMessage msg = CastMessage
                        .newBuilder()
                        .setProtocolVersion(CastMessage.ProtocolVersion.CASTV2_1_0)
                        .setSourceId(DEFAULT_RECEIVER_ID)
                        .setDestinationId(message.getSourceId())
                        .setNamespace(AUTH_NS)
                        .setPayloadType(CastMessage.PayloadType.BINARY)
                        .setPayloadBinary(resp)
                        .build();
                    send(msg);
                } else if (is(message, "PING")) {
                    send(build(HEARTBEAT_NS, message.getSourceId(), new Message("PONG")));
                } else if (is(message, "GET_STATUS")) {
                    respond(message, build(RECEIVER_NS, message.getSourceId(), new ReceiverStatus(Collections
                        .emptyList(), new CastDeviceVolumeData(VolumeControlType.MASTER, 0.0, false, 0.1))));
                } else if (is(message, "SET_VOLUME")) {
                    final boolean muted =
                            parse(message, SetVolumeMuted.class).map(SetVolumeMuted::isMuted).orElse(false);
                    final double level =
                            parse(message, SetVolumeLevel.class).map(SetVolumeLevel::level).orElse(0.0);
                    respond(message, build(RECEIVER_NS, message.getSourceId(), new ReceiverStatus(Collections
                        .emptyList(), new CastDeviceVolumeData(VolumeControlType.MASTER, level, muted, 0.1))));
                } else if (is(message, "CLOSE")) {
                    break;
                }
            }
        } catch (final SocketException e) {
            /*
             * socket closed, exit and await for next connection
             */
            LOGGER.log(Level.FINE, "Connection closed", e);
        } finally {
            try {
                suspended = false;
                rejectAllAuthenticationRequests = false;
                socket.close();
            } catch (final IOException e) {
                LOGGER.log(Level.WARNING, "I/O error when closing socket", e);
            }
            socket = null;
        }
    }

    /**
     * Responds to the given request with the given response.
     *
     * @param request request
     * @param response response
     * @throws IOException in case of I/O error
     */
    private void respond(final CastMessage request, final CastMessage response) throws IOException {
        final JsonElement elt = GSON.fromJson(response.getPayloadUtf8(), JsonElement.class);
        final JsonObject obj = elt.getAsJsonObject();
        obj.remove(REQUEST_ID);
        final int reqId =
                parse(request, Message.class).flatMap(Message::requestId).orElseThrow(AssertionError::new);
        obj.addProperty(REQUEST_ID, reqId);
        send(CastMessage.newBuilder(response).clearPayloadUtf8().setPayloadUtf8(GSON.toJson(elt)).build());
    }

    /**
     * Immediately sends the given message to the client.
     *
     * @param message message to send
     * @throws IOException in case of I/O error
     */
    private void send(final CastMessage message) throws IOException {
        if (!suspended) {
            CastMessageCodec.write(message, socket.getOutputStream());
        }
    }

}
