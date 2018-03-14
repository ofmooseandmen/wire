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
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;

/**
 * Steps to log start/end of scenario.
 */
public final class ScenarioSteps {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(ScenarioSteps.class.getName());

    /**
     * Closes the connection with the device and logs end of scenario and the most severe status of the scenario's
     * steps.
     *
     * @param scenario scenario
     */
    @After(order = 1)
    public final void after(final Scenario scenario) {
        assertTrue("Expected events to be empty but was ["
            + rt().events().stream().map(e -> e.type().toString()).collect(Collectors.joining(", "))
            + "]", rt().events().isEmpty());
        rt().controller().close();
        LOGGER.info(() -> "Scenario '"
            + scenario.getName()
            + "' ended @ "
            + Instant.now()
            + " with status "
            + scenario.getStatus());
    }

    /**
     * Logs start of scenario.
     *
     * @param scenario scenario
     */
    @Before
    public final void before(final Scenario scenario) {
        LOGGER.info(() -> "Scenario '" + scenario.getName() + "' started @ " + Instant.now());
    }

}
