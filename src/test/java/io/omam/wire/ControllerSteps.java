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

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cucumber.api.java.After;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.omam.wire.CastChannel.CastMessage;
import io.omam.wire.ScenarioRuntime.Event;
import io.omam.wire.ScenarioRuntime.EventType;

/**
 * Steps to interact with the {@link CastDeviceController}.
 */
@SuppressWarnings("javadoc")
public final class ControllerSteps {

    private static final class FakeAppController extends StandardApplicationController {

        protected FakeAppController(final Application someDetails) {
            super(someDetails);
        }

        @Override
        protected final void appMessageReceived(final CastMessage message) {
            // ignore.
        }

    }

    private final Exceptions exs;

    private CastDeviceStatus status;

    private FakeAppController app;

    public ControllerSteps(final Exceptions exceptions) {
        exs = exceptions;
    }

    @After
    public final void after() throws Exception {
        if (app != null) {
            rt().controller().stopApp(app);
            /* the cast device sends a DEVICE_STATUS event when stopping an app. */
            assertNotNull(rt().events().poll(1, TimeUnit.SECONDS));
        }
    }

    @Given("^the application \"(\\w+)\" has been launched")
    public final void givenApplicationLaunched(final String appId) {
        whenApplicationLaunched(appId);
    }

    @Given("^the connection with the device has been opened$")
    public final void givenDeviceConnectionOpened() {
        whenConnectionOpened();
    }

    @Then("^the application \"(\\w+)\" shall be (not )?running on the device$")
    public final void thenApplicationRunning(final String appId, final String not) {
        try {
            assertTrue(rt().controller().isAppAvailable(appId));
            if (not != null) {
                assertNotNull(status);
                assertTrue(status.applications().isEmpty());
            }
        } catch (IOException | TimeoutException e) {
            exs.thrown(e);
        }
    }

    @Then("^the connection shall be closed after \"([^\"]*)\"$")
    public void thenConnectionClosed(final String duration) {
        await().atLeast(Duration.parse(duration).toMillis(), TimeUnit.MILLISECONDS).until(
                () -> !rt().controller().isConnected());
    }

    @Then("^the connection shall be opened$")
    public final void thenConnectionOpened() {
        assertTrue(rt().controller().isConnected());
    }

    @Then("^the device controller listener shall be notified of the following events:$")
    public final void thenDeviceControllerListenerNotified(final List<EventType> expecteds) {
        for (final EventType expected : expecteds) {
            await().atMost(1, TimeUnit.SECONDS).until(() -> {
                final Event e = rt().events().poll();
                return e != null && e.type() == expected;
            });
        }
    }

    @Then("^the received device status shall report a muted volume$")
    public final void thenDeviceStatusMutedLevel() {
        assertNotNull(status);
        assertTrue(status.volume().isMuted());
    }

    @Then("^the received device status shall report no available application$")
    public final void thenDeviceStatusNoApp() {
        assertNotNull(status);
        assertTrue(status.applications().isEmpty());
    }

    @Then("^the received device status shall report an umuted volume$")
    public final void thenDeviceStatusUmutedLevel() {
        assertNotNull(status);
        assertFalse(status.volume().isMuted());
    }

    @Then("^the received device status shall report a volume level of (\\d+.\\d+)$")
    public final void thenDeviceStatusVolumeLevel(final double level) {
        assertNotNull(status);
        assertEquals(level, status.volume().level(), 1e-10);
    }

    @When("^the application \"(\\w+)\" is requested to be launched")
    public final void whenApplicationLaunched(final String appId) {
        try {
            app = rt().controller().launchApp(appId, FakeAppController::new);
        } catch (final IOException | TimeoutException e) {
            exs.thrown(e);
        }
    }

    @When("^the application \"(\\w+)\" is requested to be stopped")
    public final void whenApplicationStopped(final String appId) {
        try {
            assertNotNull(app);
            assertEquals(appId, app.details().id());
            status = rt().controller().stopApp(app);
            app = null;
        } catch (final IOException | TimeoutException e) {
            exs.thrown(e);
        }
    }

    @When("^the connection is closed$")
    public final void whenConnectionClosed() {
        rt().controller().close();
    }

    @When("^the connection with the device is opened$")
    public final void whenConnectionOpened() {
        try {
            rt().controller().connect();
        } catch (final IOException | TimeoutException e) {
            exs.thrown(e);
        }
    }

    @When("^the connection with the device is opened with a timeout of \"([^\"]*)\"$")
    public void whenConnectionOpened(final String timeout) {
        try {
            rt().controller().connect(Duration.parse(timeout));
        } catch (final IOException | TimeoutException e) {
            exs.thrown(e);
        }
    }

    @When("^the device is requested to be muted$")
    public final void whenDeviceMuted() {
        try {
            status = rt().controller().muteDevice();
        } catch (final IOException | TimeoutException e) {
            exs.thrown(e);
        }
    }

    @When("^the device status is requested$")
    public final void whenDeviceStatusRequested() {
        try {
            status = rt().controller().deviceStatus();
        } catch (final IOException | TimeoutException e) {
            exs.thrown(e);
        }
    }

    @When("^the device is requested to be unmuted$")
    public final void whenDeviceUnmuted() {
        try {
            status = rt().controller().unmuteDevice();
        } catch (final IOException | TimeoutException e) {
            exs.thrown(e);
        }
    }

    @When("^the device volume is requested to be set to (\\d+.\\d+)$")
    public final void whenDeviceVolumeSet(final double level) {
        try {
            status = rt().controller().setDeviceVolume(level);
        } catch (final IOException | TimeoutException e) {
            exs.thrown(e);
        }
    }

}
