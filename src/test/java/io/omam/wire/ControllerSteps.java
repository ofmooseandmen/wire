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

import static io.omam.wire.ScenarioRuntime.rt;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import cucumber.api.java8.En;
import io.omam.wire.CastChannel.CastMessage;
import io.omam.wire.ScenarioRuntime.Event;
import io.omam.wire.ScenarioRuntime.EventType;

/**
 * Steps to interact with the {@link CastDeviceController}.
 */
@SuppressWarnings("javadoc")
public final class ControllerSteps implements En {

    private static final class FakeAppController extends StandardApplicationController {

        FakeAppController(final Application someDetails) {
            super(someDetails);
        }

        @Override
        protected final void appMessageReceived(final CastMessage message) {
            // ignore.
        }

    }

    @FunctionalInterface
    private static interface ThrowingRunnable {

        void run() throws Exception;
    }

    private Exceptions exs;

    private CastDeviceStatus status;

    private FakeAppController app;

    public ControllerSteps(final Exceptions exceptions) {

        exs = exceptions;

        After(() -> {
            if (app != null) {
                rt().controller().stopApp(app);
                /* the cast device sends a DEVICE_STATUS event when stopping an app. */
                assertNotNull(rt().events().poll(1, TimeUnit.SECONDS));
            }
        });

        Given("the application {string} has been launched", (final String appId) -> app =
                exs.call(() -> rt().controller().launchApp(appId, FakeAppController::new)));

        Given("the connection with the device has been opened", () -> exs.run(() -> rt().controller().connect()));

        Then("the application {string} shall be running on the device",
                (final String appId) -> exs.run(() -> assertTrue(rt().controller().isAppAvailable(appId))));

        Then("the application {string} shall be not running on the device", (final String appId) -> exs.run(() -> {
            assertTrue(rt().controller().isAppAvailable(appId));
            assertNotNull(status);
            assertTrue(status.applications().isEmpty());
        }));

        Then("the connection shall be opened", () -> {
            assertTrue(rt().controller().isConnected());
        });

        Then("the connection shall be closed after {duration}", (final Duration dur) -> {
            await().atLeast(dur.toMillis(), TimeUnit.MILLISECONDS).until(() -> !rt().controller().isConnected());
        });

        Then("the device controller listener shall be notified of a {eventType} event", (final EventType et) -> {
            await().atMost(1, TimeUnit.SECONDS).until(() -> {
                final Event e = rt().events().poll();
                return e != null && e.type() == et;
            });
        });

        Then("the received device status shall report a muted volume", () -> {
            assertNotNull(status);
            assertTrue(status.volume().isMuted());
        });

        Then("the received device status shall report an umuted volume", () -> {
            assertNotNull(status);
            assertFalse(status.volume().isMuted());
        });

        Then("the received device status shall report no available application", () -> {
            assertNotNull(status);
            assertTrue(status.applications().isEmpty());
        });

        Then("the received device status shall report a volume level of {float}", (final Float level) -> {
            assertNotNull(status);
            assertEquals(level, status.volume().level(), 1e-10);
        });

        When("the application {string} is requested to be launched", (final String appId) -> app =
                exs.call(() -> rt().controller().launchApp(appId, FakeAppController::new)));

        When("the application {string} is requested to be stopped", (final String appId) -> {
            status = exs.call(() -> {
                assertNotNull(app);
                assertEquals(appId, app.details().id());
                return rt().controller().stopApp(app);
            });
            app = null;
        });

        When("the connection with the device is closed", () -> rt().controller().close());

        When("the connection with the device is opened", () -> exs.run(() -> rt().controller().connect()));

        When("the connection with the device is opened with a timeout of {duration}",
                (final Duration dur) -> exs.run(() -> rt().controller().connect(dur)));

        When("the device is requested to be muted", () -> status = exs.call(() -> rt().controller().muteDevice()));

        When("the device is requested to be unmuted",
                () -> status = exs.call(() -> rt().controller().unmuteDevice()));

        When("the device status is requested", () -> status = exs.call(() -> rt().controller().deviceStatus()));

        When("the device volume is requested to be set to {float}",
                (final Float level) -> status = exs.call(() -> rt().controller().setDeviceVolume(level)));

    }

}
