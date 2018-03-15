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

import static io.omam.wire.CastV2Protocol.FRIENDLY_NAME;
import static io.omam.wire.CastV2Protocol.REGISTRATION_TYPE;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.omam.halo.Browser;
import io.omam.halo.Halo;
import io.omam.halo.Service;
import io.omam.halo.ServiceBrowserListener;

/**
 * {@link CastDeviceBrowser} implementation.
 */
final class CastDeviceBrowserImpl implements CastDeviceBrowser {

    /**
     * {@link ServiceBrowserListener} that notifies a {@link CastDeviceBrowser}.
     */
    private static final class Listener implements ServiceBrowserListener {

        /** logger. */
        private static final Logger LOGGER = Logger.getLogger(Listener.class.getName());

        /** all discovered clients indexed by service name. */
        private final Map<String, CastDeviceController> clients;

        /** listener to notify. */
        private final CastDeviceBrowserListener l;

        /**
         * Constructor.
         *
         * @param listener {@link CastDeviceBrowser} to notify
         */
        Listener(final CastDeviceBrowserListener listener) {
            clients = new ConcurrentHashMap<>();
            l = listener;
        }

        @Override
        public final void down(final Service service) {
            final CastDeviceController client = clients.remove(service.name().toLowerCase());
            if (client != null) {
                l.down(client);
            }
        }

        @Override
        public final void up(final Service service) {
            final InetAddress address = service.ipv4Address().orElseGet(() -> service.ipv6Address().orElse(null));
            final String name = service.attributes().value(FRIENDLY_NAME, StandardCharsets.UTF_8).orElseGet(
                    service::instanceName);
            if (address == null) {
                LOGGER.warning(() -> "Ignored Cast Device ["
                    + service.instanceName()
                    + "] as it does not report its IP address");
            } else {
                final int port = service.port();
                try {
                    final CastDeviceController client = CastDeviceController.v2(name, address, port);
                    clients.put(service.name().toLowerCase(), client);
                    l.up(client);
                } catch (final GeneralSecurityException e) {
                    LOGGER.log(Level.WARNING, e, () -> "Ignored Cast Device [" + service.instanceName() + "]");
                }
            }
        }

    }

    /** halo. */
    private final Halo halo;

    /** cast browser. */
    private final Browser browser;

    /**
     * Constructor.
     *
     * @param listener listener
     * @throws IOException in case of I/O error
     */
    CastDeviceBrowserImpl(final CastDeviceBrowserListener listener) throws IOException {
        halo = Halo.allNetworkInterfaces(Clock.systemDefaultZone());
        final ServiceBrowserListener l = new Listener(listener);
        browser = halo.browse(REGISTRATION_TYPE, l);
    }

    @Override
    public final void close() {
        browser.close();
        halo.close();
    }

}
