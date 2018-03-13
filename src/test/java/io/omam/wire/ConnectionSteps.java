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

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.omam.wire.ScenarioRuntime.Event;
import io.omam.wire.ScenarioRuntime.EventType;

/**
 * Steps to handle connection with the device.
 */
@SuppressWarnings("javadoc")
public final class ConnectionSteps {

    private final Exceptions exs;

    public ConnectionSteps(final Exceptions exceptions) {
        exs = exceptions;
    }

    @Given("^the connection with the device has been opened$")
    public final void givenDeviceConnectionOpened() {
        whenConnectionOpened();
    }

    @Then("^the connection shall be closed after \"([^\"]*)\"$")
    public void thenConnectionClosed(final String duration) {
        await().atLeast(Duration.parse(duration).toMillis(), TimeUnit.MILLISECONDS).until(
                () -> !rt().controller().isConnected());
    }

    @Then("^the device controller listener is notified that the connection is dead$")
    public final void thenDeviceControllerListenerNotifiedConnectionDead() {
        await().atMost(1, TimeUnit.SECONDS).until(() -> {
            final Event e = rt().events().poll();
            return e != null && e.type() == EventType.CONNECTION_DEAD;
        });
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

}
