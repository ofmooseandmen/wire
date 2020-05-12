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
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Facility to await for a condition to be satisfied.
 */
final class Monitor {

    /**
     * A duration continuously decreasing until {@link Duration#ZERO}.
     * <p>
     * This class is helpful when awaiting for a {@code java.util.concurrent.locks.Condition} to be signaled.
     */
    private static class Timeout {

        /** remaining duration. */
        private final Duration duration;

        /** when the timeout was created, milliseconds from the epoch of 1970-01-01T00:00Z. */
        private final long start;

        /**
         * Class constructor.
         *
         * @param initialDuration initial duration, not null
         */
        Timeout(final Duration initialDuration) {
            Objects.requireNonNull(initialDuration);
            duration = initialDuration;
            start = System.nanoTime();
        }

        /**
         * Assess and returns the remaining duration or {@link Duration#ZERO} if the initial duration has elapsed.
         *
         * @return the remaining duration
         */
        final Duration remaining() {
            final long elapsedNs = System.nanoTime() - start;
            Duration remaining = duration.minus(elapsedNs, ChronoUnit.NANOS);
            if (remaining.isNegative()) {
                remaining = Duration.ZERO;
            }
            return remaining;
        }

    }

    /** Logger. */
    private static final Logger LOGGER = Logger.getLogger(Monitor.class.getName());

    /** lock. */
    private final Lock lock;

    /** the condition. */
    private final Condition cdt;

    /** a supplier that returns {@code true} only if condition being monitored is satisfied. */
    private final Supplier<Boolean> cdtSatisfied;

    /**
     * Class constructor.
     *
     * @param conditionSatisfied a supplier that returns {@code true} only if condition being monitored is
     *            satisfied
     */
    Monitor(final Supplier<Boolean> conditionSatisfied) {
        lock = new ReentrantLock();
        cdt = lock.newCondition();
        cdtSatisfied = conditionSatisfied;
    }

    /**
     * Await until the condition is satisfied or the given timeout has elapsed which ever occurs first.
     *
     * @param timeout how long to wait before giving up
     * @return {@code true} if condition is satisfied before timeout
     */
    final boolean await(final Duration timeout) {
        lock.lock();
        boolean success = false;
        try {
            /*
             * check if condition is already satisfied.
             */
            if (cdtSatisfied.get()) {
                return true;
            }
            final Timeout to = new Timeout(timeout);
            Duration remaining = to.remaining();
            do {
                success = cdt.await(remaining.toMillis(), TimeUnit.MILLISECONDS);
                remaining = to.remaining();
            } while (!cdtSatisfied.get() && !remaining.isZero());
        } catch (final InterruptedException e) {
            LOGGER.log(Level.WARNING, "Interrupted while waiting for condition to be satisfied", e);
            Thread.currentThread().interrupt();
            success = false;
        } finally {
            lock.unlock();
        }
        return success;
    }

    /**
     * Acquires the lock.
     *
     * @see Lock#lock()
     */
    final void lock() {
        lock.lock();
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
     * Releases the lock.
     *
     * @see Lock#unlock()
     */
    final void unlock() {
        lock.unlock();
    }

}
