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
import static io.omam.wire.discovery.DiscoveryProperties.REGISTRATION_TYPE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.omam.halo.Attributes;
import io.omam.halo.Halo;
import io.omam.halo.RegisterableService;
import io.omam.halo.RegisteredService;
import io.omam.wire.device.CastDeviceController;

/**
 * Steps to discover Cast devices using mDNS service discovery.
 */
@SuppressWarnings("javadoc")
public final class DiscoverySteps {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private Halo halo;

    private CastDeviceBrowser browser;

    private final Queue<CastDeviceController> discovered;

    private RegisteredService registered;

    /**
     * Constructor.
     */
    public DiscoverySteps() {
        discovered = new ConcurrentLinkedQueue<>();
    }

    @After
    public final void after() {
        if (browser != null) {
            browser.close();
        }
        if (halo != null) {
            try {
                halo.deregisterAll();
            } catch (final IOException e) {
                // ignore
            }
            halo.close();
        }
    }

    @Given("the cast device {string} exists")
    public final void givenDevice(final String id) throws IOException {
        register(id, Optional.empty());
    }

    @Given("the cast device {string} reporting the friendly name {string} exists")
    public final void givenDevice(final String id, final String friendlyName) throws IOException {
        register(id, Optional.of(friendlyName));
    }

    @Given("the cast device {string} has been discovered on the network")
    public final void givenDiscovered(final String id) throws IOException {
        register(id, Optional.empty());
        browse();
        awaitDiscovery(cdc -> cdc.deviceId().equals(id));
    }

    @Then("a cast device shall be discovered")
    public final void thenDiscovered() {
        await().atMost(TIMEOUT).until(() -> !discovered.isEmpty());
    }

    @Then("the cast device {string} shall be discovered")
    public final void thenDiscovered(final String id) {
        awaitDiscovery(cdc -> cdc.deviceId().equals(id));
    }

    @Then("the cast device {string} reporting the friendly name {string} shall be discovered")
    public final void thenDiscovered(final String id, final String friendlyName) {
        awaitDiscovery(cdc -> cdc.deviceId().equals(id) && cdc.deviceName().equals(Optional.of(friendlyName)));
    }

    @Then("the cast device {string} is no longer discovered")
    public final void thenNoLongerDiscovered(final String id) {
        await().atMost(TIMEOUT).until(() -> discovered.stream().noneMatch(cdc -> cdc.deviceId().equals(id)));
    }

    @When("the network is browsed for cast devices")
    public final void whenBrowsing() throws IOException {
        browse();
    }

    @When("the cast device {string} is deregistered")
    public final void whenDeregistered(final String id) throws IOException {
        assertNotNull(registered);
        assertEquals(id, registered.instanceName());
        halo.deregister(registered);
    }

    private void awaitDiscovery(final Predicate<CastDeviceController> predicate) {
        await().atMost(TIMEOUT).until(() -> discovered.stream().anyMatch(predicate));
    }

    private void browse() throws IOException {
        assertNull(browser);
        final CastDeviceBrowserListener listener = new CastDeviceBrowserListener() {

            @Override
            public final void deviceDiscovered(final CastDeviceController controller) {
                discovered.add(controller);
            }

            @Override
            public final void deviceRemoved(final CastDeviceController controller) {
                discovered.remove(controller);
            }
        };
        browser = CastDeviceBrowser.start(listener);
    }

    private void register(final String id, final Optional<String> friendlyName) throws IOException {
        assertNull(halo);
        halo = Halo.allNetworkInterfaces(Clock.systemUTC());
        final Attributes attributes = friendlyName
            .map(name -> Attributes.create().with(FRIENDLY_NAME, name, StandardCharsets.UTF_8).get())
            .orElseGet(Attributes::empty);
        final RegisterableService service =
                RegisterableService.create(id, REGISTRATION_TYPE, 9154).attributes(attributes).get();
        registered = halo.register(service);
    }

}
