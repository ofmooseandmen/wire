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
package io.omam.wire.testkit;

import static io.omam.wire.io.IoProperties.DEFAULT_RECEIVER_ID;
import static io.omam.wire.io.json.Payloads.buildMessage;
import static io.omam.wire.io.json.Payloads.hasType;
import static io.omam.wire.io.json.Payloads.parse;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;

import io.omam.wire.app.AppAvailabilities.AppAvailability;
import io.omam.wire.device.DeviceProperties;
import io.omam.wire.io.CastChannel.AuthError;
import io.omam.wire.io.CastChannel.AuthError.ErrorType;
import io.omam.wire.io.CastChannel.CastMessage;
import io.omam.wire.io.CastChannel.DeviceAuthMessage;
import io.omam.wire.io.CastMessageCodec;
import io.omam.wire.io.json.Payload;

/**
 * An emulated Cast device that implements the Cast V2 Protocol an behaves as closely as possible as an real device
 * (audio) running 1.30 firmware.
 */
public final class EmulatedCastDevice implements AutoCloseable {

    /**
     * Application(s) availability request payload.
     */
    private static final class AppAvailabilityReq extends Payload {

        public static final String TYPE = "GET_APP_AVAILABILITY";

        /** ID of each application. */
        private Collection<String> appId;

        /**
         * @return ID of each application
         */
        final Collection<String> appId() {
            return appId;
        }

    }

    /**
     * Application(s) availability response payload.
     */
    private static final class AppAvailabilityResp extends Payload {

        /** application availability indexed by application ID. */
        @SuppressWarnings("unused")
        private final Map<String, AppAvailability> availability;

        /**
         * Constructor.
         *
         * @param someAvailability application availability indexed by application ID
         */
        AppAvailabilityResp(final Map<String, AppAvailability> someAvailability) {
            super(null, "GET_APP_AVAILABILITY");
            availability = someAvailability;
        }

    }

    /**
     * Received applications.
     */
    @SuppressWarnings("unused")
    private static final class AppData {

        private final String appId;

        private final String displayName;

        private final boolean isIdleScreen;

        private final boolean launchedFromCloud;

        private final Collection<NamespaceData> namespaces;

        private final String sessionId;

        private final String statusText;

        private final String transportId;

        AppData(final String anAppId, final String aSessionId, final String aTransportId) {
            appId = anAppId;
            displayName = anAppId;
            isIdleScreen = false;
            launchedFromCloud = false;
            namespaces = Collections.emptyList();
            sessionId = aSessionId;
            statusText = "";
            transportId = aTransportId;
        }

        final String id() {
            return appId;
        }

        final String sessionId() {
            return sessionId;
        }

    }

    /**
     * Invalid Request message payload.
     */
    private static final class InvalidRequest extends Payload {

        /** unique instance. */
        static final InvalidRequest INSTANCE = new InvalidRequest();

        /**
         * Constructor.
         */
        private InvalidRequest() {
            super("INVALID_REQUEST", null);
        }
    }

    /**
     * A {@link Consumer} that can throw {@link IOException}.
     *
     * @param <T> the type of the input to the operation
     */
    private static interface IoConsumer<T> extends Consumer<T> {

        @Override
        default void accept(final T t) {
            try {
                tryAccept(t);
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        /**
         * Performs this operation on the given argument and potentially throws an {@link IOException}.
         *
         * @param t the input argument
         * @throws IOException in case of I/O error
         */
        void tryAccept(final T t) throws IOException;

    }

    /**
     * Launch application request payload.
     */
    private static final class Launch extends Payload {

        public static final String TYPE = "LAUNCH";

        /** application ID. */
        private String appId;

        /**
         * @return application ID.
         */
        final String appId() {
            return appId;
        }

    }

    /**
     * Launch Error message payload.
     */
    private static final class LaunchError extends Payload {

        /** unique instance. */
        static final LaunchError INSTANCE = new LaunchError();

        /**
         * Constructor.
         */
        private LaunchError() {
            super("LAUNCH_ERROR", null);
        }
    }

    /**
     * Namespace.
     */
    private static final class NamespaceData {

        @SuppressWarnings("unused")
        private String name;

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
     * Cast device status response payload.
     */
    private static final class ReceiverStatus extends Payload {

        @SuppressWarnings("unused")
        private final ReceiverStatusData status;

        ReceiverStatus(final List<AppData> apps, final VolumeData volume) {
            super("RECEIVER_STATUS", null);
            status = new ReceiverStatusData(apps, volume);
        }

    }

    /**
     * Receiver status data.
     */
    private static final class ReceiverStatusData {

        /** applications. */
        @SuppressWarnings("unused")
        private final List<AppData> applications;

        /** volume. */
        @SuppressWarnings("unused")
        private final VolumeData volume;

        ReceiverStatusData(final List<AppData> someApplications, final VolumeData aVolume) {
            applications = someApplications;
            volume = aVolume;
        }
    }

    /**
     * Set volume level request payload.
     */
    private static final class SetVolumeLevel extends Payload {

        public static final String TYPE = "SET_VOLUME";

        /** volume level. */
        private VolumeLevel volume;

        /**
         * @return volume level.
         */
        final double level() {
            return volume.level;
        }
    }

    /**
     * Set volume muted request payload.
     */
    private static final class SetVolumeMuted extends Payload {

        public static final String TYPE = "SET_VOLUME";

        /** volume muted?. */
        private VolumedMuted volume;

        /**
         * @return volume muted?.
         */
        final boolean isMuted() {
            return volume.muted;
        }

    }

    /**
     * Stop application request payload.
     */
    private static final class Stop extends Payload {

        public static final String TYPE = "STOP";

        /** application session ID. */
        private String sessionId;

        /**
         * @return application session ID.
         */
        final String sessionId() {
            return sessionId;
        }
    }

    private static final class VolumeData {

        @SuppressWarnings("unused")
        private final double level;

        @SuppressWarnings("unused")
        private final boolean muted;

        VolumeData(final double aLevel, final boolean isMuted) {
            level = aLevel;
            muted = isMuted;
        }

    }

    /**
     * Toggle volume mute on/off.
     */
    private static final class VolumedMuted {

        /** volume muted?. */
        boolean muted;

    }

    /**
     * Volume level.
     */
    private static final class VolumeLevel {

        /** volume level. */
        double level;

    }

    /** device socket address. */
    public static final InetSocketAddress ADDRESS = new InetSocketAddress("localhost", 8009);

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(EmulatedCastDevice.class.getName());

    /** authentication namespace/protocol. */
    private static final String AUTH_NS = "urn:x-cast:com.google.cast.tp.deviceauth";

    /** heartbeat namepspace. */
    private static final String HEARTBEAT_NS = "urn:x-cast:com.google.cast.tp.heartbeat";

    /** receiver namespace. */
    private static final String RECEIVER_NS = "urn:x-cast:com.google.cast.receiver";

    /** authentication error. */
    private static final ByteString AUTH_ERR;

    /** authentication OK. */
    private static final ByteString AUTH_OK;

    static {
        AUTH_ERR = DeviceAuthMessage
            .newBuilder()
            .setError(AuthError.newBuilder().setErrorType(ErrorType.INTERNAL_ERROR).build())
            .build()
            .toByteString();
        AUTH_OK = DeviceAuthMessage.newBuilder().build().toByteString();
    }

    /** JSON request ID property. */
    private static final String REQUEST_ID = "requestId";

    /** Gson instance. */
    private static final Gson GSON = new Gson();

    /** ID of the default media receiver application. */
    private static final String MEDIA_APP_ID = "CC1AD845";

    /** queue of received messages. */
    private final BlockingQueue<CastMessage> receivedMessages;

    /** received message handlers. */
    private final Map<String, IoConsumer<CastMessage>> handlers;

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

    /** whether the device was muted. */
    private boolean muted;

    /** last set volume level. */
    private double level;

    /** list of launched applications. */
    private final Collection<AppData> launched;

    /** list of available applications. */
    private final Collection<String> avail;

    /**
     * Constructor.
     */
    public EmulatedCastDevice() {
        System.setProperty("io.omam.wire.device.useTls", "false");
        /* force load DeviceProperties class. */
        LOGGER.info("Use TLS? " + DeviceProperties.USE_TLS);
        receivedMessages = new LinkedBlockingQueue<>();
        handlers = new HashMap<>();
        handlers.put("PING", this::handlePing);
        handlers.put("GET_STATUS", this::handleGetStatus);
        handlers.put("SET_VOLUME", this::handleSetVolume);
        handlers.put("LAUNCH", this::handleLaunch);
        handlers.put("STOP", this::handleStop);
        handlers.put("GET_APP_AVAILABILITY", this::handleGetAppAvailability);

        rejectAllAuthenticationRequests = false;
        suspended = false;
        muted = false;
        level = 0.0;
        launched = new ArrayList<>();
        avail = new ArrayList<>();
        avail.add(MEDIA_APP_ID);

        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(ADDRESS);

            readerThread = new Thread(this::handleConnection);
            readerThread.start();

        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns true if the given message request the connection with this device to be closed.
     *
     * @param message message
     * @return true if the given message request the connection with this device to be closed
     */
    private static boolean isCloseConnection(final CastMessage message) {
        /* ignore close request for applications. */
        return message.getDestinationId().equals(DEFAULT_RECEIVER_ID) && hasType(message, "CLOSE");
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
    public final void rejectAllAuthenticationRequests() {
        rejectAllAuthenticationRequests = true;
    }

    /**
     * Suspends communications with the client.
     */
    public final void suspend() {
        suspended = true;
    }

    /**
     * Retrieves and remove the oldest received message waiting if necessary until available.
     *
     * @param timeout timeout
     * @return first received message or empty if timeout has elapsed
     */
    public final Optional<CastMessage> takeReceivedMessage(final Duration timeout) {
        try {
            return Optional.ofNullable(receivedMessages.poll(timeout.toMillis(), TimeUnit.MILLISECONDS));
        } catch (final InterruptedException e) {
            LOGGER.log(Level.WARNING, e, () -> "interrupted while waiting");
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
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
     * Handles a received GET_APP_AVAILABILITY message by sending back the application(s) availability.
     *
     * @param message message
     * @throws IOException in case of I/O error
     */
    private void handleGetAppAvailability(final CastMessage message) throws IOException {
        final Collection<String> ids = parse(message, AppAvailabilityReq.TYPE, AppAvailabilityReq.class).appId();
        final Map<String, AppAvailability> resp = new HashMap<>();
        for (final String id : ids) {
            final boolean isAvail = avail.contains(id);
            resp.put(id, isAvail ? AppAvailability.APP_AVAILABLE : AppAvailability.APP_NOT_AVAILABLE);
        }
        respond(message, buildMessage(RECEIVER_NS, message.getSourceId(), new AppAvailabilityResp(resp)));
    }

    /**
     * Handles a received GET_DEVICE_STATUS message by sending back a DEVICE_STATUS.
     *
     * @param message message
     * @throws IOException in case of I/O error
     */
    private void handleGetStatus(final CastMessage message) throws IOException {
        respond(message, buildMessage(RECEIVER_NS, message.getSourceId(), receiverStatus()));
    }

    /**
     * Handles a received LAUNCH message by sending back a DEVICE_STATUS.
     *
     * @param message message
     * @throws IOException in case of I/O error
     */
    private void handleLaunch(final CastMessage message) throws IOException {
        final String appId = parse(message, Launch.TYPE, Launch.class).appId();
        if (avail.contains(appId)) {
            final String sessionId = UUID.randomUUID().toString();
            final String transportId = "transport-" + UUID.randomUUID().toString();
            final AppData app = new AppData(appId, sessionId, transportId);
            launched.add(app);
            respond(message, buildMessage(RECEIVER_NS, message.getSourceId(), receiverStatus()));
        } else {
            respond(message, buildMessage(RECEIVER_NS, message.getSourceId(), LaunchError.INSTANCE));
        }
    }

    /**
     * Handles messages from the client: try to respond when appropriate.
     */
    private void handleMessages() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                final InputStream is = socket.getInputStream();
                final CastMessage message = CastMessageCodec
                    .read(is)
                    .orElseThrow(() -> new IOException("Could not read received message"));
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
                } else if (isCloseConnection(message)) {
                    break;
                } else {
                    final String type = parse(message)
                        .map(m -> m.responseType().orElseGet(() -> m.type().orElse(null)))
                        .orElseThrow(IOException::new);
                    handlers.getOrDefault(type, t -> {
                        // empty, no specific processing.
                    }).accept(message);
                }
            }
        } catch (final Exception e) {
            /*
             * consider socket closed, exit and await for next connection
             */
            LOGGER.log(Level.FINE, "Connection closed after exception", e);
        } finally {
            try {

                LOGGER.fine(() -> "Closing socket");
                rejectAllAuthenticationRequests = false;
                suspended = false;
                muted = false;
                level = 0.0;
                launched.clear();

                socket.close();
            } catch (final IOException e) {
                LOGGER.log(Level.WARNING, "I/O error when closing socket", e);
            }
            socket = null;
        }
    }

    /**
     * Handles a received PING message by sending back a PONG.
     *
     * @param message message
     * @throws IOException in case of I/O error
     */
    private void handlePing(final CastMessage message) throws IOException {
        send(buildMessage(HEARTBEAT_NS, message.getSourceId(), Pong.INSTANCE));
    }

    /**
     * Handles a received SET_VOLUME message by sending back a DEVICE_STATUS.
     *
     * @param message message
     * @throws IOException in case of I/O error
     */
    private void handleSetVolume(final CastMessage message) throws IOException {
        muted = parse(message, SetVolumeMuted.TYPE, SetVolumeMuted.class).isMuted();
        level = parse(message, SetVolumeLevel.TYPE, SetVolumeLevel.class).level();
        respond(message, buildMessage(RECEIVER_NS, message.getSourceId(), receiverStatus()));
    }

    /**
     * Handles a received STOP message by sending back a DEVICE_STATUS.
     *
     * @param message message
     * @throws IOException in case of I/O error
     */
    private void handleStop(final CastMessage message) throws IOException {
        final String sessionId = parse(message, Stop.TYPE, Stop.class).sessionId();
        final Optional<AppData> optApp =
                launched.stream().filter(a -> a.sessionId().equals(sessionId)).findFirst();
        if (optApp.isPresent()) {
            final AppData app = optApp.get();
            launched.remove(app);
            respond(message, buildMessage(RECEIVER_NS, message.getSourceId(), receiverStatus()));
            send(buildMessage(RECEIVER_NS, message.getSourceId(), receiverStatus()));
        } else {
            respond(message, buildMessage(RECEIVER_NS, message.getSourceId(), InvalidRequest.INSTANCE));
        }
    }

    /**
     * @return a receiver status build with the current status.
     */
    private ReceiverStatus receiverStatus() {
        return new ReceiverStatus(new ArrayList<>(launched), new VolumeData(level, muted));
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
        final int reqId = parse(request).flatMap(Payload::requestId).orElseThrow(AssertionError::new);
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
