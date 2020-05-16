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
package io.omam.wire.discovery;

import static io.omam.wire.discovery.DiscoveryProperties.FRIENDLY_NAME;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.omam.halo.ResolvedService;
import io.omam.halo.ServiceBrowserListener;
import io.omam.wire.device.CastDeviceController;

/**
 * {@link ServiceBrowserListener} that notifies a browser.
 */
final class DeviceBrowserListener implements ServiceBrowserListener {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(DeviceBrowserListener.class.getName());

    /** all discovered clients indexed by service name. */
    private final Map<String, CastDeviceController> clients;

    /** listener to notify. */
    private final CastDeviceBrowserListener l;

    /**
     * Constructor.
     *
     * @param listener browser to notify
     */
    DeviceBrowserListener(final CastDeviceBrowserListener listener) {
        clients = new ConcurrentHashMap<>();
        l = listener;
    }

    @Override
    public final void serviceAdded(final ResolvedService service) {
        // FIXME: avoid duplicate service, i.e. map.containsKey(service.name().toLowerCase())
        final InetAddress address = service.ipv4Address().orElseGet(() -> service.ipv6Address().orElse(null));
        final String instanceName = service.instanceName();
        if (address == null) {
            LOGGER
                .warning(() -> "Ignored Cast Device [" + instanceName + "] as it does not report its IP address");
        } else {
            final int port = service.port();
            try {
                final Optional<String> name = service.attributes().value(FRIENDLY_NAME, StandardCharsets.UTF_8);
                LOGGER.info(() -> "Found Cast Device " + name + " [" + instanceName + "]");
                final InetSocketAddress socketAddress = new InetSocketAddress(address, port);
                final CastDeviceController client = CastDeviceController.v2(instanceName, socketAddress, name);
                clients.put(service.name().toLowerCase(), client);
                l.deviceDiscovered(client);
            } catch (final GeneralSecurityException e) {
                LOGGER.log(Level.WARNING, e, () -> "Ignored Cast Device [" + instanceName + "]");
            }
        }
    }

    @Override
    public final void serviceRemoved(final ResolvedService service) {
        final CastDeviceController client = clients.remove(service.name().toLowerCase());
        if (client != null) {
            l.deviceRemoved(client);
        }
    }

    @Override
    public final void serviceUpdated(final ResolvedService service) {
        // TODO: check what can be changed (maybe the friendly name)?
    }

}
