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
package io.omam.wire;

import static io.omam.wire.ScenarioRuntime.rt;

import java.io.InputStream;
import java.util.logging.LogManager;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

/**
 * Associates Cucumber JVM with the Junit runner to run all Wire features targeting an real Cast device.
 * <p>
 * This test suite is NOT intended to be run on the CI where obviously no real Cast device is available: an idle
 * Cast device with must be reachable on the local network in order for the tests of this suite to pass.
 */
@RunWith(Cucumber.class)
@CucumberOptions(monochrome = true, dryRun = false, strict = true, tags = { "@RealDevice" }, plugin = {}, features = {})
public final class WireRealDeviceTestSuite {

    /**
     * Tear down, once all tests have been run.
     */
    @AfterClass
    public static void after() {
        rt().tearDown();
    }

    /**
     * Reads 'logging.properties', configures all Loggers and browses the local network for a real Cast device.
     *
     * @throws SecurityException if a security manager exists and if the caller does not have
     *                           LoggingPermission("control")
     * @throws Exception         if test environment setup fails
     */
    @BeforeClass
    public static void before() throws Exception {
        try (final InputStream ins = ClassLoader.getSystemResourceAsStream("logging.properties")) {
            LogManager.getLogManager().readConfiguration(ins);
        }
        rt().setup();
    }

}
