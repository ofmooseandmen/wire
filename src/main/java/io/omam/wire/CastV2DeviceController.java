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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.logging.Logger;

/**
 * {@link CastDeviceController} implementing the Cast V2 protocol.
 */
final class CastV2DeviceController implements CastDeviceController {

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(CastV2DeviceController.class.getName());

    /** message for exception. */
    private static final String CONNECTION_IS_NOT_OPENED = "Connection is not opened";

    /** Cast device identifier. */
    private final String id;

    /** communication channel. */
    private final CastV2Channel channel;

    /** Cast device friendly name. */
    private final Optional<String> name;

    /** connection controller. */
    private final ConnectionController connection;

    /** device status controller. */
    private final ReceiverController receiver;

    /**
     * Constructor.
     *
     * @param anId Cast device unique identifier
     * @param aChannel communication channel
     * @param aName Cast device name
     */
    CastV2DeviceController(final String anId, final CastV2Channel aChannel, final Optional<String> aName) {
        id = anId;
        channel = aChannel;
        name = aName;
        connection = new ConnectionController(channel);
        receiver = new ReceiverController(channel);
    }

    @Override
    public final void addListener(final CastDeviceControllerListener listener) {
        Objects.requireNonNull(listener);
        connection.addListener(listener);
        receiver.addListener(listener);
    }

    @Override
    public final AppAvailabilities appsAvailability(final Collection<String> appIds, final Duration timeout)
            throws IOException, TimeoutException {
        LOGGER.info(() -> "Request availability of applications  " + appIds + " on device " + deviceNameOrId());
        ensureConnected();
        return receiver.appAvailability(appIds, timeout);
    }

    @Override
    public final void close() {
        LOGGER.info(() -> "Closing connection with device " + deviceNameOrId());
        connection.close();
    }

    @Override
    public final void connect(final Duration timeout) throws IOException, TimeoutException {
        LOGGER.info(() -> "Opening connection with device " + deviceNameOrId());
        connection.connect(timeout);
    }

    @Override
    public final InetSocketAddress deviceAddress() {
        return channel.address();
    }

    @Override
    public final String deviceId() {
        return id;
    }

    @Override
    public final Optional<String> deviceName() {
        return name;
    }

    @Override
    public final CastDeviceStatus getDeviceStatus(final Duration timeout) throws IOException, TimeoutException {
        LOGGER.info(() -> "Requesting status of device " + deviceNameOrId());
        ensureConnected();
        return receiver.receiverStatus(timeout);
    }

    @Override
    public final boolean isConnected() {
        return connection.isOpened();
    }

    @Override
    public final void joinAppSession(final ApplicationController app) {
        final String transportId = app.details().transportId();
        LOGGER.info(() -> "Joining application session " + transportId + " on device " + deviceNameOrId());
        connection.joinAppSession(transportId);
    }

    @Override
    public final <T extends ApplicationController> T launchApp(final String appId,
            final BiFunction<ApplicationData, ApplicationWire, T> controllerSupplier, final boolean joinAppSession,
            final Duration timeout) throws IOException, TimeoutException {
        LOGGER.info(() -> "Launching application " + appId + " on device " + deviceNameOrId());
        ensureConnected();
        final CastDeviceStatus status = receiver.launch(appId, timeout);
        final Collection<ApplicationData> apps = status.applications();
        final ApplicationData app = apps
            .stream()
            .filter(a -> a.id().equals(appId))
            .findFirst()
            .orElseThrow(() -> new IOException("Received status does not contain application [" + appId + "]"));
        final T ctrl = controllerSupplier.apply(app, new DefaultApplicationWire(channel));
        app.namespaces().forEach(ns -> channel.addListener(ctrl, ns.name()));
        if (joinAppSession) {
            joinAppSession(ctrl);
        }
        return ctrl;
    }

    @Override
    public final CastDeviceStatus muteDevice(final Duration timeout) throws IOException, TimeoutException {
        LOGGER.info(() -> "Muting device " + deviceNameOrId());
        ensureConnected();
        return receiver.mute(timeout);
    }

    @Override
    public void removeListener(final CastDeviceControllerListener listener) {
        Objects.requireNonNull(listener);
        connection.removeListener(listener);
        receiver.removeListener(listener);
    }

    @Override
    public final CastDeviceStatus setDeviceVolume(final double level, final Duration timeout)
            throws IOException, TimeoutException {
        LOGGER.info(() -> "Setting volume of device " + deviceNameOrId() + " to " + level);
        ensureConnected();
        return receiver.setVolume(level, timeout);
    }

    @Override
    public final CastDeviceStatus stopApp(final ApplicationController app, final Duration timeout)
            throws IOException, TimeoutException {
        LOGGER.info(() -> "Stopping application " + app.details().id() + " on device " + deviceNameOrId());
        ensureConnected();
        connection.leaveAppSession(app.details().transportId());
        final CastDeviceStatus cds = receiver.stopApp(app.details().sessionId(), timeout);
        channel.removeListener(app);
        return cds;
    }

    @Override
    public final String toString() {
        return "CastV2Device [name=" + name + "]";
    }

    @Override
    public final CastDeviceStatus unmuteDevice(final Duration timeout) throws IOException, TimeoutException {
        LOGGER.info(() -> "Unmuting device " + deviceNameOrId());
        ensureConnected();
        return receiver.unmute(timeout);
    }

    /**
     * Returns {@link #deviceName()} if present, {@link #deviceId()} otherwise.
     *
     * @return device name or id
     */
    private String deviceNameOrId() {
        return name.orElse(id);
    }

    /**
     * Throws an {@link IOException} if the connection with the device is not opened.
     *
     * @throws IOException if the connection with the device is not opened
     */
    private void ensureConnected() throws IOException {
        if (!connection.isOpened()) {
            LOGGER.warning(() -> "Not connected with device");
            throw new IOException(CONNECTION_IS_NOT_OPENED);
        }
    }

}
