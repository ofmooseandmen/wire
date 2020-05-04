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

import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import io.omam.wire.testkit.EmulatedCastDevice;
import io.omam.wire.testkit.WireTestKit;

/**
 * The scenario runtime provides the specifics of the runtime environment, such as the client being tested and the
 * emulated device when running the {@code Emulated} test suite.
 */
final class ScenarioRuntime implements CastDeviceControllerListener {

    /**
     * listener event.
     */
    @SuppressWarnings("javadoc")
    static final class Event {

        private final EventType type;

        private final Object data;

        Event(final EventType aType, final Object someData) {
            type = aType;
            data = someData;
        }

        final Object data() {
            return data;
        }

        final EventType type() {
            return type;
        }

    }

    /**
     * Event types.
     */
    @SuppressWarnings("javadoc")
    enum EventType {
        CONNECTION_DEAD,
        REMOTE_CONNECTION_CLOSED,
        DEVICE_STATUS;
    }

    /**
     * Thread-safe singleton, see the "Effective Java" book, Item 71.
     */
    private static class InstanceHolder {

        /** unique instance. */
        @SuppressWarnings("synthetic-access")
        static final ScenarioRuntime INSTANCE = new ScenarioRuntime();

        /**
         * Constructor.
         */
        private InstanceHolder() {
            // empty.
        }

    }

    /** {@code true} if setup has been performed. */
    private boolean setup;

    /** the controller being tested. */
    private CastDeviceController ctrl;

    /** emulated Cast device, null when running the {@code Real} test suite. */
    private EmulatedCastDevice ecd;

    /** queue of listener events. */
    private final BlockingQueue<Event> evts;

    /**
     * Constructor.
     */
    private ScenarioRuntime() {
        evts = new LinkedBlockingQueue<>();
    }

    /**
     * @return the unique {@link ScenarioRuntime} instance.
     */
    static ScenarioRuntime rt() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public final void connectionDead() {
        evts.add(new Event(EventType.CONNECTION_DEAD, null));
    }

    @Override
    public final void deviceStatusUpdated(final CastDeviceStatus status) {
        evts.add(new Event(EventType.DEVICE_STATUS, status));
    }

    @Override
    public final void remoteConnectionClosed() {
        evts.add(new Event(EventType.REMOTE_CONNECTION_CLOSED, null));
    }

    /**
     * @return the {@link CastDeviceController} being tested.
     */
    final CastDeviceController controller() {
        return Objects.requireNonNull(ctrl);
    }

    /**
     * @return the emulated Cast device.
     */
    final EmulatedCastDevice emulatedCastDevice() {
        return Objects.requireNonNull(ecd);
    }

    /**
     * @return all received events from the listener, must be empty at the end of the scenario, i.e. events must be
     *         asserted.
     */
    final BlockingQueue<Event> events() {
        return evts;
    }

    final void setup() throws GeneralSecurityException {
        if (WireTestKit.requiresEmulatedDevice()) {
            setupEmulatedDevice();
        } else {
            setupRealDevice();
        }
    }

    /**
     * Tears down the run time.
     */
    final void tearDown() {
        setup = false;
        if (ctrl != null) {
            ctrl.close();
            ctrl = null;
        }
        if (ecd != null) {
            ecd.close();
        }
    }

    /**
     * Creates an emulated Cast device and a controller for it.
     *
     * @throws GeneralSecurityException from controller
     */
    private void setupEmulatedDevice() throws GeneralSecurityException {
        if (setup) {
            throw new IllegalStateException("Setup already done");
        }
        System.setProperty("io.omam.wire.device.heartbeatIntervalMs", "2000");
        ecd = new EmulatedCastDevice();
        final String deviceId = UUID.randomUUID().toString();
        ctrl = CastDeviceController.v2(deviceId, EmulatedCastDevice.ADDRESS, Optional.empty());
        ctrl.addListener(this);

    }

    /**
     * Creates a controller for a the real cast device being used.
     *
     * @throws GeneralSecurityException from controller
     */
    private void setupRealDevice() throws GeneralSecurityException {
        if (setup) {
            throw new IllegalStateException("Setup already done");
        }
        final String deviceId = UUID.randomUUID().toString();
        final InetSocketAddress address = WireTestKit.realDeviceAddr();
        ctrl = CastDeviceController.v2(deviceId, address, Optional.empty());
        ctrl.addListener(this);
    }

}
