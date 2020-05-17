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
package io.omam.wire.device;

import static io.omam.wire.device.ScenarioRuntime.rt;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.omam.wire.app.AppAvailabilities;
import io.omam.wire.app.AppAvailabilities.AppAvailability;
import io.omam.wire.app.ApplicationController;
import io.omam.wire.app.ApplicationData;
import io.omam.wire.app.ApplicationWire;
import io.omam.wire.device.ScenarioRuntime.Event;
import io.omam.wire.device.ScenarioRuntime.EventType;
import io.omam.wire.io.CastChannel.CastMessage;

/**
 * Steps to interact with the {@link CastDeviceController}.
 */
@SuppressWarnings("javadoc")
public final class ControllerSteps {

    private static final class FakeAppController implements ApplicationController {

        private final ApplicationData details;

        @SuppressWarnings("unused")
        FakeAppController(final ApplicationData someDetails, final ApplicationWire aWire) {
            details = someDetails;
        }

        @Override
        public final ApplicationData details() {
            return details;
        }

        @Override
        public final void unsolicitedMessageReceived(final String type, final CastMessage message) {
            // empty.
        }

    }

    @FunctionalInterface
    private static interface ThrowingRunnable {

        void run() throws Exception;
    }

    private final Exceptions exs;

    private CastDeviceStatus status;

    private FakeAppController app;

    private AppAvailabilities appAvail;

    public ControllerSteps(final Exceptions exceptions) {
        exs = exceptions;
    }

    @After
    public final void after() throws IOException, TimeoutException, InterruptedException {
        if (app != null) {
            rt().controller().stopApp(app);
            /* the cast device sends a DEVICE_STATUS event when stopping an app. */
            assertNotNull(rt().events().poll(1, TimeUnit.SECONDS));
        }
    }

    @Given("the application {string} has been launched")
    public final void givenAppLaunched(final String appId) {
        app = exs.call(() -> rt().controller().launchApp(appId, FakeAppController::new));
    }

    @Given("the connection with the device has been opened")
    public final void givenConnectionOpened() {
        exs.run(() -> rt().controller().connect());
    }

    @Then("the application {string} shall be not running on the device")
    public final void thenAppNotRunning(final String appId) {
        exs.run(() -> {
            assertNull(app);
            assertTrue(rt().controller().isAppAvailable(appId));
            assertNotNull(status);
            assertTrue(status.applications().isEmpty());
        });
    }

    @Then("the application {string} shall be running on the device")
    public final void thenAppRunning(final String appId) {
        exs.run(() -> {
            assertNotNull(app);
            assertTrue(rt().controller().isAppAvailable(appId));
            assertNull(status);
            assertTrue(rt()
                .controller()
                .getDeviceStatus()
                .applications()
                .stream()
                .anyMatch(a -> a.applicationId().equals(appId)));
        });
    }

    @Then("the device reports that the application {string} is available")
    public final void thenAvailableApp(final String appId) {
        assertEquals(AppAvailability.APP_AVAILABLE, appAvail.availabilities().get(appId));
    }

    @Then("the connection shall be closed after {word}")
    public final void thenConnectionClosed(final String duration) {
        await().atLeast(Duration.parse(duration)).until(() -> !rt().controller().isConnected());
    }

    @Then("the connection shall be opened")
    public final void thenConnectionOpened() {
        assertTrue(rt().controller().isConnected());
    }

    @Then("the device controller listener shall be notified of a {word} event")
    public final void thenListenerNotified(final String eventType) {
        final EventType et = EventType.valueOf(eventType);
        await().atMost(Duration.ofSeconds(1)).until(() -> {
            final Event e = rt().events().poll();
            return e != null && e.type() == et;
        });
    }

    @Then("the received device status shall report a muted volume")
    public final void thenMuted() {
        assertNotNull(status);
        assertTrue(status.volume().isMuted());
    }

    @Then("the received device status shall report no running application")
    public final void thenNoAvailableApp() {
        assertNotNull(status);
        assertTrue(status.applications().isEmpty());
    }

    @Then("the received device status shall report an umuted volume")
    public final void thenUnmuted() {
        assertNotNull(status);
        assertFalse(status.volume().isMuted());
    }

    @Then("the received device status shall report a volume level of {float}")
    public final void thenVolumeIs(final Float level) {
        assertNotNull(status);
        assertEquals(level, status.volume().level(), 1e-10);
    }

    @When("the availability of application {string} is requested")
    public final void whenAppAvailability(final String appId) {
        appAvail = exs.call(() ->

        rt().controller().getAppsAvailability(Collections.singleton(appId)));
    }

    @When("the application {string} is requested to be launched")
    public final void whenAppLaunch(final String appId) {
        app = exs.call(() -> rt().controller().launchApp(appId, FakeAppController::new));
    }

    @When("the application {string} is requested to be stopped")
    public final void whenAppStop(final String appId) {
        status = exs.call(() -> {
            assertNotNull(app);
            assertEquals(appId, app.details().applicationId());
            return rt().controller().stopApp(app);
        });
        app = null;
    }

    @When("the connection with the device is closed")
    public final void whenConnectionClose() {
        rt().controller().disconnect();
    }

    @When("the connection with the device is opened")
    public final void whenConnectionOpen() {
        exs.run(() -> rt().controller().connect());
    }

    @When("the connection with the device is opened with a timeout of {word}")
    public final void whenConnectionOpen(final String duration) {
        exs.run(() -> rt().controller().connect(Duration.parse(duration)));
    }

    @When("the device status is requested")
    public final void whenGetStatus() {
        status = exs.call(() -> rt().controller().getDeviceStatus());
    }

    @When("the device is requested to be muted")
    public final void whenMute() {
        status = exs.call(() -> rt().controller().muteDevice());
    }

    @When("the device is requested to be unmuted")
    public final void whenUnmute() {
        status = exs.call(() -> rt().controller().unmuteDevice());
    }

    @When("the device volume is requested to be set to {float}")
    public final void whenVolumeSet(final float level) {
        status = exs.call(() -> rt().controller().changeDeviceVolume(level));
    }

}
