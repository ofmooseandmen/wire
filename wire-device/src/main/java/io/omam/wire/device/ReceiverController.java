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

import static io.omam.wire.io.json.Payloads.parse;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.omam.wire.app.AppAvailabilities;
import io.omam.wire.app.ApplicationData;
import io.omam.wire.app.ApplicationData.Namespace;
import io.omam.wire.io.CastChannel.CastMessage;
import io.omam.wire.io.NamespaceListener;
import io.omam.wire.io.json.Payload;

/**
 * {@code ReceiverController} implements the {@code urn:x-cast:com.google.cast.receiver} namespace/protocol to
 * control applications.
 */
final class ReceiverController implements NamespaceListener {

    /**
     * Application(s) availability request payload.
     */
    private static final class AppAvailabilityReq extends Payload {

        /** ID of each application. */
        @SuppressWarnings("unused")
        private final Collection<String> appId;

        /**
         * Constructor.
         *
         * @param someAppId ID of each application
         */
        AppAvailabilityReq(final Collection<String> someAppId) {
            super("GET_APP_AVAILABILITY", null);
            appId = someAppId;
        }

    }

    /**
     * Application(s) availability response payload.
     */
    private static final class AppAvailabilityResp extends Payload implements AppAvailabilities {

        /** payload type. */
        static final String TYPE = "GET_APP_AVAILABILITY";

        /** application availability indexed by application ID. */
        private Map<String, AppAvailability> availability;

        /**
         * Constructor.
         */
        private AppAvailabilityResp() {
            /* empty, initialised by GSON. */
        }

        @Override
        public final Map<String, AppAvailability> availabilities() {
            return availability == null ? Collections.emptyMap() : Collections.unmodifiableMap(availability);
        }

    }

    /**
     * Received applications.
     */
    private static final class ApplicationDataImpl implements ApplicationData {

        /** {@link #applicationId()}. */
        private String appId;

        /** {@link #name()}. */
        private String displayName;

        /** {@link #isIdleScreen()}. */
        private boolean isIdleScreen;

        /** {@link #launchedFromCloud()}. */
        private boolean launchedFromCloud;

        /** {@link #namespaces()}. */
        private Collection<NamespaceData> namespaces;

        /** {@link #sessionId()}. */
        private String sessionId;

        /** {@link #statusText()}. */
        private String statusText;

        /** {@link #transportId()}. */
        private String transportId;

        /**
         * Constructor.
         */
        private ApplicationDataImpl() {
            /* empty, initialised by GSON. */
        }

        @Override
        public final String applicationId() {
            return appId;
        }

        @Override
        public final boolean isIdleScreen() {
            return isIdleScreen;
        }

        @Override
        public final boolean launchedFromCloud() {
            return launchedFromCloud;
        }

        @Override
        public final String name() {
            return displayName;
        }

        @Override
        public final Collection<Namespace> namespaces() {
            return Collections.unmodifiableCollection(namespaces);
        }

        @Override
        public final String sessionId() {
            return sessionId;
        }

        @Override
        public final String statusText() {
            return statusText;
        }

        @Override
        public final String transportId() {
            return transportId;
        }

    }

    /**
     * Received device volume.
     */
    private static final class CastDeviceVolumeData implements CastDeviceVolume {

        /** {@link #controlType()}. */
        private VolumeControlType controlType;

        /** {@link #level()}. */
        private double level;

        /** {@link #isMuted()}. */
        private boolean muted;

        /** {@link #stepInterval()}. */
        private double stepInterval;

        /**
         * Constructor.
         */
        private CastDeviceVolumeData() {
            /* empty, initialised by GSON. */
        }

        @Override
        public final VolumeControlType controlType() {
            return controlType;
        }

        @Override
        public final boolean isMuted() {
            return muted;
        }

        @Override
        public final double level() {
            return level;
        }

        @Override
        public final double stepInterval() {
            return stepInterval;
        }

    }

    /**
     * Get Status message payload.
     */
    private static final class GetStatus extends Payload {

        /** unique instance. */
        static final GetStatus INSTANCE = new GetStatus();

        /**
         * Constructor.
         */
        private GetStatus() {
            super("GET_STATUS", null);
        }

    }

    /**
     * Launch application request payload.
     */
    private static final class Launch extends Payload {

        /** application ID. */
        @SuppressWarnings("unused")
        private final String appId;

        /**
         * Constructor.
         *
         * @param anAppId application ID
         */
        Launch(final String anAppId) {
            super("LAUNCH", null);
            appId = anAppId;
        }

    }

    /**
     * Namespace.
     */
    private static final class NamespaceData implements Namespace {

        /** {@link #name()}. */
        private String name;

        /**
         * Constructor.
         */
        private NamespaceData() {
            /* empty, initialised by GSON. */
        }

        @Override
        public final String name() {
            return name;
        }

    }

    /**
     * Cast device status response payload.
     */
    private static final class ReceiverStatus extends Payload implements CastDeviceStatus {

        /** payload type. */
        static final String TYPE = "RECEIVER_STATUS";

        /** status. */
        private ReceiverStatusData status;

        /**
         * Constructor.
         */
        private ReceiverStatus() {
            /* empty, initialised by GSON. */
        }

        @Override
        public final List<ApplicationData> applications() {
            return status.applications();
        }

        @Override
        public final CastDeviceVolume volume() {
            return status.volume();
        }

    }

    /**
     * Receiver status data.
     */
    private static final class ReceiverStatusData {

        /** applications. */
        private List<ApplicationDataImpl> applications;

        /** volume. */
        private CastDeviceVolumeData volume;

        /**
         * Constructor.
         */
        private ReceiverStatusData() {
            /* empty, initialised by GSON. */
        }

        /**
         * @return applications
         */
        final List<ApplicationData> applications() {
            return applications == null ? Collections.emptyList() : Collections.unmodifiableList(applications);
        }

        /**
         * @return volume
         */
        final CastDeviceVolume volume() {
            return volume;
        }

    }

    /**
     * Set volume level request payload.
     */
    private static final class SetVolumeLevel extends Payload {

        /** volume level. */
        @SuppressWarnings("unused")
        private final VolumeLevel volume;

        /**
         * Constructor.
         *
         * @param level volume level
         */
        SetVolumeLevel(final double level) {
            super("SET_VOLUME", null);
            volume = new VolumeLevel(level);
        }

    }

    /**
     * Set volume muted request payload.
     */
    private static final class SetVolumeMuted extends Payload {

        /** volume muted?. */
        @SuppressWarnings("unused")
        private final VolumedMuted volume;

        /**
         * Constructor.
         *
         * @param isMuted volume muted?
         */
        SetVolumeMuted(final boolean isMuted) {
            super("SET_VOLUME", null);
            volume = new VolumedMuted(isMuted);
        }

    }

    /**
     * Stop application request payload.
     */
    private static final class Stop extends Payload {

        /** application session ID. */
        @SuppressWarnings("unused")
        private final String sessionId;

        /**
         * Constructor.
         *
         * @param aSessionId application session ID
         */
        Stop(final String aSessionId) {
            super("STOP", null);
            sessionId = aSessionId;
        }

    }

    /**
     * Toggle volume mute on/off.
     */
    private static final class VolumedMuted {

        /** volume muted?. */
        @SuppressWarnings("unused")
        private final boolean muted;

        /**
         * Constructor.
         *
         * @param isMuted volume muted?
         */
        VolumedMuted(final boolean isMuted) {
            muted = isMuted;
        }

    }

    /**
     * Volume level.
     */
    private static final class VolumeLevel {

        /** volume level. */
        @SuppressWarnings("unused")
        private final double level;

        /**
         * Constructor.
         *
         * @param aLevel volume level
         */
        VolumeLevel(final double aLevel) {
            level = aLevel;
        }

    }

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(ReceiverController.class.getName());

    /** receiver namespace. */
    static final String RECEIVER_NS = "urn:x-cast:com.google.cast.receiver";

    /** requestor. */
    private final Requestor requestor;

    /** listeners. */
    private final ConcurrentLinkedQueue<CastDeviceStatusListener> listeners;

    /**
     * Constructor.
     *
     * @param aRequestor requesto
     */
    ReceiverController(final Requestor aRequestor) {
        requestor = aRequestor;
        listeners = new ConcurrentLinkedQueue<>();
    }

    /**
     * Obtains an instance of {@code ReceiverStatus} from the given payload.
     *
     * @param resp response
     * @return a the parsed ReceiverStatus
     * @throws IOException if the response is an error
     */
    private static ReceiverStatus parseReceiverStatus(final CastMessage resp) throws IOException {
        return parse(resp, ReceiverStatus.TYPE, ReceiverStatus.class);
    }

    @Override
    public final void uncorrelatedResponseReceived(final CastMessage message) {
        notifyListeners(message, true);
    }

    @Override
    public final void unsolicitedMessageReceived(final CastMessage message) {
        notifyListeners(message, false);
    }

    /**
     * Adds the given listener to receive device status events.
     *
     * @param listener listener
     */
    final void addListener(final CastDeviceStatusListener listener) {
        listeners.add(listener);
    }

    /**
     * Request and returns the availability of the given applications.
     *
     * @param appIds ID of each application
     * @param timeout status timeout
     * @return the availability of the given applications, never null
     * @throws IOException in case of I/O error (including if connection has not be opened)
     * @throws TimeoutException if the timeout has elapsed before the availability of the given applications was
     *             received
     */
    final AppAvailabilities getAppAvailability(final Collection<String> appIds, final Duration timeout)
            throws IOException, TimeoutException {
        final CastMessage resp = requestor.sendUtf8Request(RECEIVER_NS, new AppAvailabilityReq(appIds), timeout);
        return parse(resp, AppAvailabilityResp.TYPE, AppAvailabilityResp.class);
    }

    /**
     * Requests and returns the status of the Cast device.
     *
     * @param timeout status timeout
     * @return the status of the Cast device, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the status was received
     */
    final CastDeviceStatus getReceiverStatus(final Duration timeout) throws IOException, TimeoutException {
        final CastMessage resp = requestor.sendUtf8Request(RECEIVER_NS, GetStatus.INSTANCE, timeout);
        return parseReceiverStatus(resp);
    }

    /**
     * Request the launch of the given application and returns the received status of the Cast device.
     *
     * @param appId application ID
     * @param timeout status timeout
     * @return the status of the Cast device, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the status was received
     */
    final CastDeviceStatus launch(final String appId, final Duration timeout)
            throws IOException, TimeoutException {
        final CastMessage resp = requestor.sendUtf8Request(RECEIVER_NS, new Launch(appId), timeout);
        return parseReceiverStatus(resp);
    }

    /**
     * Mutes the device.
     *
     * @param timeout timeout
     * @return the received response, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the default timeout has elapsed before the response was received
     */
    final CastDeviceStatus mute(final Duration timeout) throws IOException, TimeoutException {
        final CastMessage resp = requestor.sendUtf8Request(RECEIVER_NS, new SetVolumeMuted(true), timeout);
        return parseReceiverStatus(resp);
    }

    /**
     * Removes the given listener so that it no longer receives device status events.
     *
     * @param listener listener
     */
    final void removeListener(final CastDeviceStatusListener listener) {
        listeners.remove(listener);
    }

    /**
     * Sets the volume level of the device.
     *
     * @param level the volume level expressed as a double in the range [{@code 0.0}, {@code 1.0}]
     * @param timeout response timeout
     * @return the received response, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the response was received
     */
    final CastDeviceStatus setVolume(final double level, final Duration timeout)
            throws IOException, TimeoutException {
        final CastMessage resp = requestor.sendUtf8Request(RECEIVER_NS, new SetVolumeLevel(level), timeout);
        return parseReceiverStatus(resp);
    }

    /**
     * Request the running instance of the application identified by the given session to be stopped and returns
     * the received status of the Cast device.
     *
     * @param sessionId application session ID
     * @param timeout status timeout
     * @return the status of the Cast device, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the status was received
     */
    final CastDeviceStatus stopApp(final String sessionId, final Duration timeout)
            throws IOException, TimeoutException {
        final CastMessage resp = requestor.sendUtf8Request(RECEIVER_NS, new Stop(sessionId), timeout);
        return parseReceiverStatus(resp);
    }

    /**
     * Un-mutes the device.
     *
     * @param timeout timeout
     * @return the received response, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the default timeout has elapsed before the response was received
     */
    final CastDeviceStatus unmute(final Duration timeout) throws IOException, TimeoutException {
        final CastMessage resp = requestor.sendUtf8Request(RECEIVER_NS, new SetVolumeMuted(false), timeout);
        return parseReceiverStatus(resp);
    }

    /**
     * Notifies listeners.
     *
     * @param message received message
     * @param timeout true if and only if the message is a response to a request which had timeout
     */
    private void notifyListeners(final CastMessage message, final boolean timeout) {
        try {
            final ReceiverStatus rs = parseReceiverStatus(message);
            LOGGER.info(() -> "Received updated device status ");
            LOGGER.fine(() -> "Received updated device status [" + message.getPayloadUtf8() + "]");
            listeners.forEach(l -> l.deviceStatusUpdated(rs, timeout));
        } catch (final IOException e) {
            LOGGER.log(Level.FINE, e, () -> "Could not parse received receiver status");
        }
    }

}
