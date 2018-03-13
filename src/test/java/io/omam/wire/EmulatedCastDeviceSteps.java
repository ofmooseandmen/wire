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

import static io.omam.wire.Payloads.parse;
import static io.omam.wire.ScenarioRuntime.rt;
import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.omam.wire.CastChannel.CastMessage;
import io.omam.wire.Payloads.Message;

/**
 * Steps to control the behaviour and probe the state of the emulated Cast device.
 */
@SuppressWarnings("javadoc")
public final class EmulatedCastDeviceSteps {

    private static final Supplier<AssertionError> UNPARSABLE =
            () -> new AssertionError("Received unparsable message");

    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    @Given("^the device rejects all authentication requests$")
    public final void givenDeviceRejectsAllAuthenticationRequests() {
        rt().emulatedCastDevice().rejectAllAuthenticationRequests();
    }

    @Given("^the device has become unresponsive$")
    public final void givenDeviceUnresponsive() {
        whenDeviceUnresponsive();
    }

    @Then("^the following messages shall be received by the device:$")
    public final void thenMessagesReceived(final List<ReceivedMessage> expecteds) throws InterruptedException {
        for (final ReceivedMessage expected : expecteds) {
            final CastMessage msg = rt().emulatedCastDevice().takeReceivedMessage(TIMEOUT).orElseThrow(
                    () -> new AssertionError("Message " + expected + " was not received"));
            assertEquals(msg.getNamespace(), expected.namespace());
            if (!expected.type().equals("AUTH")) {
                final Message parsed = parse(msg, Message.class).orElseThrow(UNPARSABLE);
                assertEquals(parsed.type(), expected.type());
            }
        }
    }

    @When("^the device becomes unresponsive$")
    public final void whenDeviceUnresponsive() {
        rt().emulatedCastDevice().suspend();
    }

}
