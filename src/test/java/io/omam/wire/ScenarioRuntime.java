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

import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import io.omam.halo.Halo;
import io.omam.halo.Service;

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

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(ScenarioRuntime.class.getName());

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
    public final void remoteConnectionClosed() {
        evts.add(new Event(EventType.REMOTE_CONNECTION_CLOSED, null));
    }

    @Override
    public final void status(final CastDeviceStatus status) {
        evts.add(new Event(EventType.DEVICE_STATUS, status));
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

    /**
     * Discovers the first available Cast device on the local network and creates a controller for it.
     *
     * @throws IOException in case of I/O error
     */
    final void setup() throws IOException {
        if (setup) {
            throw new IllegalStateException("Setup already done");
        }
        @SuppressWarnings("synthetic-access")
        final CastDeviceBrowserListener l = new CastDeviceBrowserListener() {

            @Override
            public final void down(final CastDeviceController controller) {
                LOGGER.warning(() -> "Device " + controller.deviceName() + " is down");
            }

            @Override
            public final void up(final CastDeviceController controller) {
                if (ctrl == null) {
                    ctrl = controller;
                    ctrl.addListener(ScenarioRuntime.this);
                }
            }
        };
        try (final CastDeviceBrowser browser = CastDeviceController.browse(l)) {
            await().atMost(10, TimeUnit.SECONDS).until(() -> ctrl != null);
            setup = true;
        }
    }

    /**
     * Creates an emulated Cast device and a controller for it.
     *
     * @throws IOException in case of I/O Error
     */
    final void setupEmulatedDevice() throws IOException {
        ecd = new EmulatedCastDevice();
        final Service service = ecd.service();
        try (final Halo halo = Halo.allNetworkInterfaces(Clock.systemDefaultZone())) {
            halo.register(service);
            setup();
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

}
