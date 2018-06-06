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
import static io.omam.wire.CastV2Protocol.SENDER_ID;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import io.omam.wire.CastChannel.CastMessage;
import io.omam.wire.CastChannel.CastMessage.PayloadType;
import io.omam.wire.CastChannel.CastMessage.ProtocolVersion;

/**
 * JSON Payloads exchanged with a Cast device.
 */
final class Payloads {

    /**
     * Base class of all CAST V2 messages with a {@code PayloadType.STRING} payload.
     * <p>
     * Depending on the message either {@link #type()} or {@link #responseType()} is present. Note that some
     * response contain {@link #type()} instead of {@link #responseType()}.
     */
    static class Message {

        /** message type, or null if response. */
        private final String type;

        /** response type, or null if message or request. */
        private final String responseType;

        /** request ID, null or 0 when not a request. */
        private final Integer requestId;

        /**
         * Constructor.
         */
        Message() {
            this(null, null);
        }

        /**
         * Constructor.
         *
         * @param aType type
         * @param aResponseType response type
         */
        Message(final String aType, final String aResponseType) {
            type = aType;
            responseType = aResponseType;
            requestId = null;
        }

        /**
         * @return the Id of the request, used to correlate request/response, if present.
         */
        final Optional<Integer> requestId() {
            return requestId == null || requestId == 0 ? Optional.empty() : Optional.of(requestId);
        }

        /**
         * @return the response type, if present.
         */
        final Optional<String> responseType() {
            return Optional.ofNullable(responseType);
        }

        /**
         * @return the message type, if present.
         */
        final Optional<String> type() {
            return Optional.ofNullable(type);
        }

    }

    /** JSON request ID property. */
    private static final String REQUEST_ID = "requestId";

    /** request id counter. */
    private static final AtomicInteger NEXT_REQUEST_ID = new AtomicInteger(0);

    /** Gson instance. */
    private static final Gson GSON = new Gson();

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(Payloads.class.getName());

    /**
     * Constructor.
     */
    private Payloads() {
        // empty.
    }

    /**
     * Adds a request ID to the JSON payload of the given message.
     *
     * @param message message
     * @return the resulting message
     */
    static CastMessage addRequestId(final CastMessage message) {
        final JsonElement elt = GSON.fromJson(message.getPayloadUtf8(), JsonElement.class);
        final JsonObject obj = elt.getAsJsonObject();
        obj.remove(REQUEST_ID);
        obj.addProperty(REQUEST_ID, NEXT_REQUEST_ID.incrementAndGet());
        return CastMessage.newBuilder(message).clearPayloadUtf8().setPayloadUtf8(GSON.toJson(elt)).build();
    }

    /**
     * Returns a new {@link CastMessage} for the given destination with the given namespace and payload.
     *
     * @param namespace namespace
     * @param payload payload
     * @param <T> type of the payload
     * @param destination destination ID
     * @return a new {@link CastMessage}
     */
    static <T extends Message> CastMessage build(final String namespace, final String destination,
            final T payload) {
        return CastMessage
            .newBuilder()
            .setProtocolVersion(ProtocolVersion.CASTV2_1_0)
            .setSourceId(SENDER_ID)
            .setDestinationId(destination)
            .setNamespace(namespace)
            .setPayloadType(PayloadType.STRING)
            .setPayloadUtf8(GSON.toJson(payload))
            .build();
    }

    /**
     * Returns a new {@link CastMessage} for the {@link CastV2Protocol#DEFAULT_RECEIVER_ID} with the given
     * namespace and payload.
     *
     * @param namespace namespace
     * @param payload payload
     * @param <T> type of the payload
     * @return a new {@link CastMessage}
     */
    static <T extends Message> CastMessage build(final String namespace, final T payload) {
        return build(namespace, DEFAULT_RECEIVER_ID, payload);
    }

    /**
     * Determines whether the given message has the given type.
     *
     * @param msg message
     * @param type type
     * @return {@code true} if given message has the given type
     */
    static boolean is(final CastMessage msg, final String type) {
        return GSON.fromJson(msg.getPayloadUtf8(), Message.class).type().map(t -> t.equals(type)).orElse(false);
    }

    /**
     * Obtains an instance of {@code T} from the payload of the given message.
     *
     * @param <T> the type of the desired object
     * @param msg message
     * @param clazz the class of {@code T}
     * @return the parsed status, empty if given message could not be parsed
     */
    static <T extends Message> Optional<T> parse(final CastMessage msg, final Class<T> clazz) {
        try {
            final String payload = msg.getPayloadUtf8();
            if (payload == null || payload.isEmpty()) {
                LOGGER.warning(() -> "Could not parse null or empty payload");
                return Optional.empty();
            }
            return Optional.of(GSON.fromJson(payload, clazz));
        } catch (final JsonSyntaxException e) {
            LOGGER.log(Level.WARNING, e,
                    () -> "Could not parse [" + msg.getPayloadUtf8() + "] into an instance of Message");
            return Optional.empty();
        }
    }

}
