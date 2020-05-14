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
package io.omam.wire.testkit;

import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.util.logging.Logger;

import io.omam.halo.Browser;
import io.omam.halo.Halo;
import io.omam.halo.ResolvedService;
import io.omam.halo.ServiceBrowserListener;

/**
 * Static utility methods.
 */
public final class WireTestKit {

    /**
     * ServiceBrowserListener that returns the socket address of the first discovered device.
     */
    private static final class FirstDiscovery implements ServiceBrowserListener {

        /** socket address or null. */
        private InetSocketAddress socketAddr;

        /**
         * Constructor.
         */
        FirstDiscovery() {
            // empty.
        }

        @Override
        public final void serviceAdded(final ResolvedService service) {
            final InetAddress address = service.ipv4Address().orElseGet(() -> service.ipv6Address().orElse(null));
            if (address != null) {
                socketAddr = new InetSocketAddress(address, service.port());
            }
        }

        @Override
        public final void serviceRemoved(final ResolvedService service) {
            // ignore.
        }

        @Override
        public final void serviceUpdated(final ResolvedService service) {
            // ignore.
        }

        /**
         * Returns the socket address of the first discovered device. Fails if non found after 10 seconds.
         *
         * @return the socket address of the first discovered device
         */
        final InetSocketAddress socketAddress() {
            await().atMost(Duration.ofSeconds(10)).until(() -> socketAddr != null);
            return socketAddr;
        }

    }

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(WireTestKit.class.getName());

    /** name of the system property that defines the socket address of the real device. */
    private static final String REAL_DEVICE_PROP_NAME = "io.omam.wire.testkit.realDevice";

    /** name of the system property that defines the cucumber tags to be run. */
    private static final String TAGS_PROP_NAME = "io.omam.wire.testkit.tags";

    /** google cast registration type. */
    private static final String CAST_REG_TYPE = "_googlecast._tcp.";

    /**
     * Constructor.
     */
    private WireTestKit() {
        // empty.
    }

    /**
     * Determines the real device socket address, either from the system property {@value #REAL_DEVICE_PROP_NAME},
     * or if not defined by browsing the network and returning the first found Cast device within 10 seconds.
     *
     * @return device address from {@code io.omam.wire.test.realDevice}.
     */
    public static InetSocketAddress realDeviceAddr() {
        final String socketAddress = System.getProperty(REAL_DEVICE_PROP_NAME);
        if (socketAddress == null) {
            return fromNetwork();
        }
        final String[] split = socketAddress.split(":");
        try {
            final String hostname = split[0];
            final int port = Integer.parseInt(split[1]);
            return new InetSocketAddress(hostname, port);
        } catch (final ArrayIndexOutOfBoundsException | NumberFormatException e) {
            final String msg = "Invalid system property " + REAL_DEVICE_PROP_NAME + ": " + socketAddress;
            LOGGER.severe(msg);
            throw new IllegalArgumentException(msg, e);
        }
    }

    /**
     * @return {@code true} iff {@value #TAGS_PROP_NAME} contains {@code @EmulatedDevice}.
     */
    public static boolean requiresEmulatedDevice() {
        final String tags = System.getProperty(TAGS_PROP_NAME);
        return tags != null && tags.contains("@EmulatedDevice");
    }

    /**
     * Browses the local network for a Cast device.
     *
     * @return the socket address of the Cast device
     */
    private static InetSocketAddress fromNetwork() {
        try (final Halo halo = Halo.allNetworkInterfaces(Clock.systemUTC())) {
            final FirstDiscovery listener = new FirstDiscovery();
            @SuppressWarnings("resource")
            final Browser browser = halo.browse(CAST_REG_TYPE, listener);
            final InetSocketAddress address = listener.socketAddress();
            browser.close();
            return address;
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
