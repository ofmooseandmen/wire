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

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * {@link CastDeviceController} implementing the CAST V2 protocol.
 */
final class CastV2DeviceController implements CastDeviceController {

    /** message for exception. */
    private static final String CONNECTION_IS_NOT_OPENED = "Connection is not opened";

    /** Cast device name. */
    private final String name;

    /** communication channel. */
    private final CastV2Channel channel;

    /** connection controller. */
    private final ConnectionController connection;

    /** device status controller. */
    private final ReceiverController receiver;

    /**
     * Constructor.
     *
     * @param aName Cast device name
     * @param aChannel communication channel
     */
    CastV2DeviceController(final String aName, final CastV2Channel aChannel) {
        name = aName;
        channel = aChannel;
        connection = new ConnectionController(channel);
        receiver = new ReceiverController(channel);
    }

    @Override
    public final void addListener(final CastDeviceControllerListener listener) {
        connection.addListener(listener);
        receiver.addListener(listener);
    }

    @Override
    public final AppAvailabilities appsAvailability(final Collection<String> appIds, final Duration timeout)
            throws IOException, TimeoutException {
        ensureConnected();
        return receiver.appAvailability(appIds, timeout);
    }

    @Override
    public final void close() {
        connection.close();
    }

    @Override
    public final void connect(final Duration timeout) throws IOException, TimeoutException {
        connection.connect(timeout);
    }

    @Override
    public final String deviceName() {
        return name;
    }

    @Override
    public final CastDeviceStatus deviceStatus(final Duration timeout) throws IOException, TimeoutException {
        ensureConnected();
        return receiver.receiverStatus(timeout);
    }

    @Override
    public final boolean isConnected() {
        return connection.isOpened();
    }

    @Override
    public final <T extends ApplicationController> T launchApp(final String appId,
            final Function<Application, T> controllerSupplier, final Duration timeout)
            throws IOException, TimeoutException {
        ensureConnected();
        final CastDeviceStatus status = receiver.launch(appId, timeout);
        final Collection<Application> apps = status.applications();
        final Application app = apps.stream().filter(a -> a.id().equals(appId)).findFirst().orElseThrow(
                () -> new IOException("Received status does not contain application [" + appId + "]"));
        final T ctrl = controllerSupplier.apply(app);
        ctrl.setChannel(channel);
        return ctrl;
    }

    @Override
    public final CastDeviceStatus muteDevice(final Duration timeout) throws IOException, TimeoutException {
        ensureConnected();
        return receiver.mute(timeout);
    }

    @Override
    public void removeListener(final CastDeviceControllerListener listener) {
        connection.removeListener(listener);
        receiver.removeListener(listener);
    }

    @Override
    public final CastDeviceStatus setDeviceVolume(final double level, final Duration timeout)
            throws IOException, TimeoutException {
        ensureConnected();
        return receiver.setVolume(level, timeout);
    }

    @Override
    public final CastDeviceStatus stopApp(final ApplicationController app, final Duration timeout)
            throws IOException, TimeoutException {
        ensureConnected();
        final CastDeviceStatus cds = receiver.stopApp(app.details().sessionId(), timeout);
        app.stopped();
        return cds;
    }

    @Override
    public final String toString() {
        return "CastV2Device [name=" + name + "]";
    }

    @Override
    public final CastDeviceStatus unmuteDevice(final Duration timeout) throws IOException, TimeoutException {
        ensureConnected();
        return receiver.unmute(timeout);
    }

    /**
     * Throws an {@link IOException} if the connection with the device is not opened.
     *
     * @throws IOException if the connection with the device is not opened
     */
    private void ensureConnected() throws IOException {
        if (!connection.isOpened()) {
            throw new IOException(CONNECTION_IS_NOT_OPENED);
        }
    }

}
