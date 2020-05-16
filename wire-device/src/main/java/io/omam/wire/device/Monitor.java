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

import java.time.Duration;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Facility to await for a condition to be satisfied.
 */
abstract class Monitor {

    /** Logger. */
    private static final Logger LOGGER = Logger.getLogger(Monitor.class.getName());

    /** lock. */
    private final Lock lock;

    /** the condition. */
    private final Condition cdt;

    /**
     * Class constructor.
     */
    protected Monitor() {
        lock = new ReentrantLock();
        cdt = lock.newCondition();
    }

    /**
     * Await until the condition is satisfied or the given timeout has elapsed which ever occurs first.
     *
     * @param timeout how long to wait before giving up
     * @return {@code true} if condition is satisfied before timeout
     */
    final boolean await(final Duration timeout) {
        long nanos = timeout.toNanos();
        lock.lock();
        try {
            while (!isConditionSatisfied()) {
                if (nanos <= 0L) {
                    return false;
                }
                nanos = cdt.awaitNanos(nanos);
            }
        } catch (final InterruptedException e) {
            LOGGER.log(Level.WARNING, "Interrupted while waiting for condition to be satisfied", e);
            Thread.currentThread().interrupt();
            return false;
        } finally {
            lock.unlock();
        }
        return true;
    }

    /**
     * Wakes up all waiting threads on the condition being monitored.
     */
    final void signalAll() {
        lock.lock();
        try {
            cdt.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return {@true} if the condition waited for is satisfied.
     */
    protected abstract boolean isConditionSatisfied();

}
