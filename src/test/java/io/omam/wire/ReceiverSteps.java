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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

/**
 * Steps to control applications on the device.
 */
@SuppressWarnings("javadoc")
public final class ReceiverSteps {

    private final Exceptions exs;

    private CastDeviceStatus status;

    public ReceiverSteps(final Exceptions exceptions) {
        exs = exceptions;
    }

    @Then("^the received device status shall report a muted volume$")
    public final void thenDeviceStatusMutedLevel() {
        assertNotNull(status);
        assertTrue(status.volume().isMuted());
    }

    @Then("^the received device status shall report no running application$")
    public final void thenDeviceStatusNoRunningApp() {
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
