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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Callable;

import io.cucumber.java8.En;

/**
 * Steps pertaining to testing whether expected {@link Exception} have been thrown.
 */
@SuppressWarnings("javadoc")
public final class Exceptions implements En {

    /**
     * A {@link Runnable} that throws {@link Exception}.
     */
    @FunctionalInterface
    static interface ThrowingRunnable {

        /**
         * Runs an action by side-effect, possibly throwing an {@link Exception}.
         *
         * @throws Exception in case of failure
         */
        void run() throws Exception;
    }

    private final Queue<Exception> exs;

    /**
     * Constructor.
     */
    public Exceptions() {
        exs = new ArrayDeque<>();

        After(() -> assertTrue("Unasserted exceptions: " + exs, exs.isEmpty()));

        Then("a {string} shall be thrown with message containing {string}",
                (final String clazz, final String msg) -> {
                    final Exception ex = exs.poll();
                    assertNotNull(ex);
                    assertEquals(clazz, ex.getClass().getName());
                    final String[] sequences = msg.split("\\(\\.\\.\\.\\)");
                    for (final String sequence : sequences) {
                        final String s = sequence.trim();
                        assertTrue("Expected message to contain [" + s + "] but was [" + ex.getMessage() + "]",
                                ex.getMessage().contains(s));
                    }
                });
    }

    final <T> T call(final Callable<T> c) {
        try {
            return c.call();
        } catch (final Exception e) {
            exs.add(e);
            return null;
        }
    }

    final void run(final ThrowingRunnable r) {
        try {
            r.run();
        } catch (final Exception e) {
            exs.add(e);
        }
    }

}
