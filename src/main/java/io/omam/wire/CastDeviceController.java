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

import static io.omam.wire.CastV2Protocol.REQUEST_TIMEOUT;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;

import io.omam.wire.AppAvailabilities.AppAvailability;

/**
 * A client to connect and interact with a Cast device.
 */
public interface CastDeviceController extends AutoCloseable {

    /**
     * Browses the network for Cast devices and notifies changes in their availability to the given listener.
     * <p>
     * Cast devices already notified to the listener are not {@link #close() closed}, when the browser is closed.
     *
     * @param listener the listener to be notified when a Cast device availability changes
     * @return a {@link CastDeviceBrowser browser} to terminate the browsing operation
     * @throws IOException in case of I/O error
     */
    static CastDeviceBrowser browse(final CastDeviceBrowserListener listener) throws IOException {
        return new CastDeviceBrowserImpl(listener);
    }

    /**
     * Returns a new controller implementing the Cast V2 protocol.
     *
     * @param id unique Cast device identifier (usually the instance name of the mDNS named service)
     * @param address Cast device IP address
     * @param port Cast device port
     * @param name the friendly name of the device is set by the owner
     * @return a new client implementing the Cast V2 protocol
     * @throws GeneralSecurityException in case of security error
     */
    static CastDeviceController v2(final String id, final InetAddress address, final int port,
            final Optional<String> name) throws GeneralSecurityException {
        final CastV2Channel channel = CastV2Channel.create(address, port);
        return new CastV2DeviceController(id, channel, name);
    }

    /**
     * Adds the given listener to receive Cast device events.
     *
     * @param listener listener, not null
     */
    void addListener(final CastDeviceControllerListener listener);

    /**
     * Requests and returns the availability of the given applications.
     *
     * @param appIds ID of each application
     * @return the availability of the given applications, never null
     * @throws IOException in case of I/O error (including if connection has not be opened)
     * @throws TimeoutException if the default timeout has elapsed before the availability of the given
     *             applications was received
     */
    default AppAvailabilities appsAvailability(final Collection<String> appIds)
            throws IOException, TimeoutException {
        return appsAvailability(appIds, REQUEST_TIMEOUT);
    }

    /**
     * Requests and returns the availability of the given applications.
     *
     * @param appIds ID of each application
     * @param timeout response timeout
     * @return the availability of the given applications, never null
     * @throws IOException in case of I/O error (including if connection has not be opened)
     * @throws TimeoutException if the timeout has elapsed before the availability of the given applications was
     *             received
     */
    AppAvailabilities appsAvailability(final Collection<String> appIds, final Duration timeout)
            throws IOException, TimeoutException;

    /**
     * Closes the connection with the Cast device.
     * <p>
     * It is possible to subsequently {@link #connect(Duration) reconnect}.
     */
    @Override
    void close();

    /**
     * Connects to the Cast device. This method blocks until the connection has been established or the default
     * connection timeout has elapsed.
     *
     * @throws IOException in case of I/O error (including authentication error)
     * @throws TimeoutException if the timeout has elapsed before the connection could be opened
     */
    default void connect() throws IOException, TimeoutException {
        connect(REQUEST_TIMEOUT);
    }

    /**
     * Connects to the Cast device. This method blocks until the connection has been established or the given
     * connection timeout has elapsed.
     *
     * @param timeout connection timeout
     * @throws IOException in case of I/O error (including authentication error)
     * @throws TimeoutException if the timeout has elapsed before the connection could be opened
     */
    void connect(final Duration timeout) throws IOException, TimeoutException;

    /**
     * Returns the IP Socket Address (IP address + port number) of the Cast device.
     *
     * @return the IP Socket Address of the Cast device
     */
    InetSocketAddress deviceAddress();

    /**
     * Returns the unique identifier Cast device; usually the instance name of the mDNS named service.
     *
     * @return the unique identifier of the Cast device
     */
    String deviceId();

    /**
     * Returns the friendly name of the Cast device if set by the owner.
     * 
     * @return the friendly name or empty
     */
    Optional<String> deviceName();

    /**
     * Requests and returns the current status of the Cast device.
     *
     * @return the current status of the Cast device, never null
     * @throws IOException in case of I/O error (including if connection has not be opened)
     * @throws TimeoutException if the default timeout has elapsed before the status was received
     */
    default CastDeviceStatus getDeviceStatus() throws IOException, TimeoutException {
        return getDeviceStatus(REQUEST_TIMEOUT);
    }

    /**
     * Request and returns the status of the Cast device.
     *
     * @param timeout response timeout
     * @return the current status of the Cast device, never null
     * @throws IOException in case of I/O error (including if connection has not be opened)
     * @throws TimeoutException if the timeout has elapsed before the status was received
     */
    CastDeviceStatus getDeviceStatus(final Duration timeout) throws IOException, TimeoutException;

    /**
     * Determines whether the given application is available on the device.
     *
     * @param appId application id
     * @return {@code true} if the given application is available on the device
     * @throws IOException in case of I/O error (including if connection has not be opened)
     * @throws TimeoutException if the default timeout has elapsed before the availability of the given
     *             applications was received
     */
    default boolean isAppAvailable(final String appId) throws IOException, TimeoutException {
        return isAppAvailable(appId, REQUEST_TIMEOUT);
    }

    /**
     * Determines whether the given application is available on the device.
     *
     * @param appId application id
     * @param timeout response timeout
     * @return {@code true} if the given application is available on the device
     * @throws IOException in case of I/O error (including if connection has not be opened)
     * @throws TimeoutException if the timeout has elapsed before the availability of the given applications was
     *             received
     */
    default boolean isAppAvailable(final String appId, final Duration timeout)
            throws IOException, TimeoutException {
        return appsAvailability(Arrays.asList(appId), timeout)
            .availabilities()
            .getOrDefault(appId, AppAvailability.APP_NOT_AVAILABLE) == AppAvailability.APP_AVAILABLE;
    }

    /**
     * Determines whether the connection with the Cast device is opened.
     *
     * @return {@code true} if the connection with the Cast device is opened
     */
    boolean isConnected();

    /**
     * Joins the session of given application which is launched on the device.
     * <p>
     * Requests/messages can be sent to the device from the given controller from this point onwards.
     *
     * @param app application controller
     */
    void joinAppSession(final ApplicationController app);

    /**
     * Requests the launch of the given application, join the application session, and returns the received status
     * of the Cast device.
     *
     * @param <T> {@link ApplicationController} type
     * @param appId application ID
     * @param controllerSupplier application controller supplier
     * @return the status of the Cast device, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the default timeout has elapsed before the status was received
     */
    default <T extends ApplicationController> T launchApp(final String appId,
            final BiFunction<ApplicationData, ApplicationWire, T> controllerSupplier)
            throws IOException, TimeoutException {
        return launchApp(appId, controllerSupplier, REQUEST_TIMEOUT);
    }

    /**
     * Requests the launch of the given application, join the application session, and returns the received status
     * of the Cast device.
     * <p>
     * If {@code joinAppSession} if false, {@link #joinAppSession(ApplicationController)} must be called before any
     * message is to be sent to the application running on the device.
     *
     * @param <T> {@link ApplicationController} type
     * @param appId application ID
     * @param controllerSupplier application controller supplier
     * @param joinAppSession {@code true} if a connect message is to be sent to the application to join the running
     *            session
     * @param timeout response timeout
     * @return the status of the Cast device, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the status was received
     */
    <T extends ApplicationController> T launchApp(final String appId,
            BiFunction<ApplicationData, ApplicationWire, T> controllerSupplier, final boolean joinAppSession,
            final Duration timeout) throws IOException, TimeoutException;

    /**
     * Requests the launch of the given application, join the application session, and returns the received status
     * of the Cast device.
     *
     * @param <T> {@link ApplicationController} type
     * @param appId application ID
     * @param controllerSupplier application controller supplier
     * @param timeout response timeout
     * @return the status of the Cast device, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the status was received
     */
    default <T extends ApplicationController> T launchApp(final String appId,
            final BiFunction<ApplicationData, ApplicationWire, T> controllerSupplier, final Duration timeout)
            throws IOException, TimeoutException {
        return launchApp(appId, controllerSupplier, true, timeout);
    }

    /**
     * Mutes the device.
     *
     * @return the current status of the Cast device, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the default timeout has elapsed before the response was received
     */
    default CastDeviceStatus muteDevice() throws IOException, TimeoutException {
        return muteDevice(REQUEST_TIMEOUT);
    }

    /**
     * Mutes the device.
     *
     * @param timeout response timeout
     * @return the current status of the Cast device, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     */
    CastDeviceStatus muteDevice(final Duration timeout) throws IOException, TimeoutException;

    /**
     * Removes the given listener so that it no longer receives Cast device events.
     *
     * @param listener listener, not null
     */
    void removeListener(final CastDeviceControllerListener listener);

    /**
     * Sets the volume level of the device.
     *
     * @param level the volume level expressed as a double in the range [{@code 0.0}, {@code 1.0}]
     * @return the current status of the Cast device, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the default timeout has elapsed before the response was received
     */
    default CastDeviceStatus setDeviceVolume(final double level) throws IOException, TimeoutException {
        return setDeviceVolume(level, REQUEST_TIMEOUT);
    }

    /**
     * Sets the volume level of the device.
     *
     * @param level the volume level expressed as a double in the range [{@code 0.0}, {@code 1.0}]
     * @param timeout response timeout
     * @return the current status of the Cast device, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     */
    CastDeviceStatus setDeviceVolume(final double level, final Duration timeout)
            throws IOException, TimeoutException;

    /**
     * Requests the running instance of the given application to be stopped and returns the received status of the
     * Cast device.
     *
     * @param app application controller
     * @return the current status of the Cast device, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the default timeout has elapsed before the status was received
     */
    default CastDeviceStatus stopApp(final ApplicationController app) throws IOException, TimeoutException {
        return stopApp(app, REQUEST_TIMEOUT);
    }

    /**
     * Requests the running instance of the given application to be stopped and returns the received status of the
     * Cast device.
     *
     * @param app application controller
     * @param timeout response timeout
     * @return the current status of the Cast device, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the status was received
     */
    CastDeviceStatus stopApp(final ApplicationController app, final Duration timeout)
            throws IOException, TimeoutException;

    /**
     * Un-mutes the device.
     *
     * @return the current status of the Cast device, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the default timeout has elapsed before the response was received
     */
    default CastDeviceStatus unmuteDevice() throws IOException, TimeoutException {
        return unmuteDevice(REQUEST_TIMEOUT);
    }

    /**
     * Un-mutes the device.
     *
     * @param timeout response timeout
     * @return the current status of the Cast device, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     */
    CastDeviceStatus unmuteDevice(final Duration timeout) throws IOException, TimeoutException;

}
