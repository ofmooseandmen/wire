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
package io.omam.wire.io.json;

import static io.omam.wire.io.IoProperties.SENDER_ID;

import java.io.IOException;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import io.omam.wire.io.CastChannel.CastMessage;
import io.omam.wire.io.CastChannel.CastMessage.PayloadType;
import io.omam.wire.io.CastChannel.CastMessage.ProtocolVersion;

/**
 * JSON Payloads exchanged with a Cast device.
 */
public final class Payloads {

    /** Gson instance. */
    private static final Gson GSON = new Gson();

    /**
     * Constructor.
     */
    private Payloads() {
        // empty.
    }

    /**
     * Returns a new message for the given destination with the given namespace and payload.
     *
     * @param namespace namespace
     * @param payload payload
     * @param <T> type of the payload
     * @param destination destination ID
     * @return a new {@link CastMessage}
     */
    public static <T extends Payload> CastMessage buildMessage(final String namespace, final String destination,
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
     * Returns a new request for the given destination with the given namespace and payload. The given request ID
     * is inserted in the given payload.
     *
     * @param namespace namespace
     * @param payload payload
     * @param <T> type of the payload
     * @param destination destination ID
     * @param requestId the request ID - must be unique
     * @return a new {@link CastMessage}
     */
    public static <T extends Payload> CastMessage buildRequest(final String namespace, final String destination,
            final T payload, final int requestId) {
        payload.setRequestId(requestId);
        return buildMessage(namespace, destination, payload);
    }

    /**
     * Determines whether the given message has the given type.
     *
     * @param msg message
     * @param type type
     * @return {@code true} if given message has the given type
     */
    public static boolean hasType(final CastMessage msg, final String type) {
        return GSON.fromJson(msg.getPayloadUtf8(), AnyPayload.class).type().map(t -> t.equals(type)).orElse(false);
    }

    /**
     * Obtains an instance of {@code Payload} from the payload of the given message.
     *
     * @param msg message
     * @return the parsed payload
     * @throws IOException if the payload cannot be parsed
     */
    public static Payload parse(final CastMessage msg) throws IOException {
        Objects.requireNonNull(msg);
        final String payload = msg.getPayloadUtf8();
        if (payload == null || payload.isEmpty()) {
            throw new IOException("Could not parse null or empty payload");
        }
        try {
            return GSON.fromJson(payload, AnyPayload.class);
        } catch (final JsonSyntaxException | ClassCastException | IllegalArgumentException e) {
            throw new IOException("Could not parse [" + payload + "]", e);
        }
    }

    /**
     * Obtains an instance of {@code T} from the payload of the given message.
     *
     * @param <T> the type of the desired object
     * @param msg message
     * @param type expected payload type
     * @param clazz the class of {@code T}
     * @return the parsed {@code T}
     * @throws IOException if the payload is not of the given type or the it cannot be parsed
     */
    public static <T extends Payload> T parse(final CastMessage msg, final String type, final Class<T> clazz)
            throws IOException {
        Objects.requireNonNull(msg);
        Objects.requireNonNull(type);
        Objects.requireNonNull(clazz);

        final String payload = msg.getPayloadUtf8();
        if (payload == null || payload.isEmpty()) {
            throw new IOException("Could not parse null or empty payload");
        }

        try {

            final AnyPayload parsedType = GSON.fromJson(payload, AnyPayload.class);
            final String actualType = parsedType.responseType().orElseGet(() -> parsedType.type().orElse(null));
            if (!type.equals(actualType)) {
                throw new IOException("Error: " + actualType + "; expected: " + type);
            }

            return GSON.fromJson(payload, clazz);
        } catch (final JsonSyntaxException | ClassCastException | IllegalArgumentException e) {
            throw new IOException("Could not parse [" + payload + "]", e);
        }
    }

}
