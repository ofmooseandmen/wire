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
package io.omam.wire;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

/**
 * CASTV2 protocol constants.
 */
public final class CastV2Protocol {

    /** default timeout when connecting or sending requests to the device. */
    public static final Duration REQUEST_TIMEOUT;

    /** Google Cast registration type. */
    static final String REGISTRATION_TYPE;

    /** friendly name attribute key. */
    static final String FRIENDLY_NAME;

    /** {@code true} if TLS protocol shall used to communicate with the device. */
    static final boolean USE_TLS;

    /** sender id. */
    static final String SENDER_ID;

    /** default receiver id. */
    static final String DEFAULT_RECEIVER_ID;

    /** PING message interval. */
    static final Duration PING_INTERVAL;

    /** PONG message timeout: can miss three PONG messages. */
    static final Duration PONG_TIMEOUT;

    static {
        try (final InputStream is = CastV2Protocol.class.getClassLoader().getResourceAsStream("wire.properties")) {
            final Properties props = new Properties();
            props.load(is);
            REGISTRATION_TYPE = stringProp("io.omam.wire.device.registrationType", props);
            FRIENDLY_NAME = stringProp("io.omam.wire.device.friendlyName", props);
            USE_TLS = Boolean.parseBoolean(stringProp("io.omam.wire.core.tls", props));
            SENDER_ID = stringProp("io.omam.wire.core.senderName", props) + "-" + UUID.randomUUID().toString();
            DEFAULT_RECEIVER_ID = stringProp("io.omam.wire.core.defaultReceiverId", props);
            REQUEST_TIMEOUT = durationProp("io.omam.wire.request.timeout", props);
            PING_INTERVAL = durationProp("io.omam.wire.heartbeat.interval", props);
            PONG_TIMEOUT = PING_INTERVAL.multipliedBy(intProp("io.omam.wire.heartbeat.missed", props));
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Constructor.
     */
    private CastV2Protocol() {
        // empty.
    }

    /**
     * Returns the {@code Duration} corresponding to the given key.
     *
     * @param key   property key
     * @param props properties default values
     * @return value
     */
    private static Duration durationProp(final String key, final Properties props) {
        return Duration.ofMillis(Long.parseLong(stringProp(key, props)));
    }

    /**
     * Returns the {@code int} corresponding to the given key.
     *
     * @param key   property key
     * @param props properties default values
     * @return value
     */
    private static int intProp(final String key, final Properties props) {
        return Integer.parseInt(stringProp(key, props));
    }

    /**
     * Returns the {@code String} corresponding to the given key.
     *
     * @param key   property key
     * @param props properties default values
     * @return value
     */
    private static String stringProp(final String key, final Properties props) {
        return System.getProperty(key, props.getProperty(key));
    }

}
