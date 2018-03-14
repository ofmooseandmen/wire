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

import static io.omam.wire.Payloads.build;
import static io.omam.wire.Payloads.parse;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import io.omam.wire.Application.Namespace;
import io.omam.wire.CastChannel.CastMessage;
import io.omam.wire.Payloads.Message;

/**
 * {@code ReceiverController} implements the {@code urn:x-cast:com.google.cast.receiver} namespace/protocol to
 * control applications.
 */
final class ReceiverController implements ChannelListener {

    /**
     * Received device volume.
     */
    static final class CastDeviceVolumeData implements CastDeviceVolume {

        /** {@link #controlType()}. */
        private final VolumeControlType controlType;

        /** {@link #level()}. */
        private final double level;

        /** {@link #isMuted()}. */
        private final boolean muted;

        /** {@link #stepInterval()}. */
        private final double stepInterval;

        /**
         * Constructor.
         *
         * @param aControlType control type
         * @param aLevel level
         * @param isMuted muted?
         * @param aStepInterval volume step interval
         */
        CastDeviceVolumeData(final VolumeControlType aControlType, final double aLevel, final boolean isMuted,
                final double aStepInterval) {
            controlType = aControlType;
            level = aLevel;
            muted = isMuted;
            stepInterval = aStepInterval;
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
     * Namespace.
     */
    static final class NamespaceData implements Namespace {

        /** {@link #name()}. */
        private String name;

        @Override
        public final String name() {
            return name;
        }

    }

    /**
     * Cast device status response.
     */
    static final class ReceiverStatus extends Message implements CastDeviceStatus {

        /** status. */
        private final ReceiverStatusData status;

        /**
         * Constructor.
         *
         * @param someApplications applications
         * @param aVolume volume
         */
        ReceiverStatus(final List<ApplicationData> someApplications, final CastDeviceVolumeData aVolume) {
            super("GET_STATUS");
            status = new ReceiverStatusData(someApplications, aVolume);
        }

        @Override
        public final List<Application> applications() {
            return status.applications();
        }

        @Override
        public final CastDeviceVolume volume() {
            return status.volume();
        }

    }

    /**
     * Set volume level request.
     */
    static final class SetVolumeLevel extends Message {

        /** volume level. */
        private final VolumeLevel volume;

        /**
         * Constructor.
         *
         * @param level volume level
         */
        SetVolumeLevel(final double level) {
            super("SET_VOLUME");
            volume = new VolumeLevel(level);
        }

        /**
         * @return volume level
         */
        final double level() {
            return volume.level();
        }

    }

    /**
     * Set volume muted request.
     */
    static final class SetVolumeMuted extends Message {

        /** volume muted?. */
        private final VolumedMuted volume;

        /**
         * Constructor.
         *
         * @param isMuted volume muted?
         */
        SetVolumeMuted(final boolean isMuted) {
            super("SET_VOLUME");
            volume = new VolumedMuted(isMuted);
        }

        /**
         * @return volume muted?.
         */
        final boolean isMuted() {
            return volume.isMuted();
        }

    }

    /**
     * Application(s) availability request.
     */
    private static final class AppAvailabilityReq extends Message {

        /** ID of each application. */
        @SuppressWarnings("unused")
        private final Collection<String> appId;

        /**
         * Constructor.
         *
         * @param someAppId ID of each application
         */
        AppAvailabilityReq(final Collection<String> someAppId) {
            super("GET_APP_AVAILABILITY");
            appId = someAppId;
        }

    }

    /**
     * Application(s) availability response.
     */
    private static final class AppAvailabilityResp extends Message implements AppAvailabilities {

        /** application availability indexed by application name. */
        private Map<String, AppAvailability> availability;

        @Override
        public final Map<String, AppAvailability> availabilities() {
            return availability == null ? Collections.emptyMap() : Collections.unmodifiableMap(availability);
        }

    }

    /**
     * Received applications.
     */
    private static final class ApplicationData implements Application {

        /** {@link #id()}. */
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

        @Override
        public final String id() {
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
     * Launch application request.
     */
    private static final class Launch extends Message {

        /** application ID. */
        @SuppressWarnings("unused")
        private final String appId;

        /**
         * Constructor.
         *
         * @param anAppId application ID
         */
        Launch(final String anAppId) {
            super("LAUNCH");
            appId = anAppId;
        }

    }

    /**
     * Receiver status data.
     */
    private static final class ReceiverStatusData {

        /** applications. */
        private final List<ApplicationData> applications;

        /** volume. */
        private final CastDeviceVolumeData volume;

        /**
         * Constructor.
         *
         * @param someApplications applications
         * @param aVolume volume
         */
        ReceiverStatusData(final List<ApplicationData> someApplications, final CastDeviceVolumeData aVolume) {
            applications = someApplications;
            volume = aVolume;
        }

        /**
         * @return applications
         */
        final List<Application> applications() {
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
     * Stop application request.
     */
    private static final class Stop extends Message {

        /** application session ID. */
        @SuppressWarnings("unused")
        private final String sessionId;

        /**
         * Constructor.
         *
         * @param aSessionId application session ID
         */
        Stop(final String aSessionId) {
            super("STOP");
            sessionId = aSessionId;
        }

    }

    /**
     * Toggle volume mute on/off.
     */
    private static final class VolumedMuted {

        /** volume muted?. */
        private final boolean muted;

        /**
         * Constructor.
         *
         * @param isMuted volume muted?
         */
        VolumedMuted(final boolean isMuted) {
            muted = isMuted;
        }

        /**
         * @return volume muted?.
         */
        boolean isMuted() {
            return muted;
        }

    }

    /**
     * Volume level.
     */
    private static final class VolumeLevel {

        /** volume level. */
        private final double level;

        /**
         * Constructor.
         *
         * @param aLevel volume level
         */
        VolumeLevel(final double aLevel) {
            level = aLevel;
        }

        /**
         * @return volume level
         */
        final double level() {
            return level;
        }

    }

    /** possible errors. */
    private static final Collection<String> ERRORS = Arrays.asList("INVALID_REQUEST", "LAUNCH_ERROR");

    /** logger. */
    private static final Logger LOGGER = Logger.getLogger(ReceiverController.class.getName());

    /** receiver namespace. */
    static final String RECEIVER_NS = "urn:x-cast:com.google.cast.receiver";

    /** communication channel. */
    private final CastV2Channel channel;

    /** listeners. */
    private final List<CastDeviceStatusListener> listeners;

    /**
     * Constructor.
     *
     * @param aChannel communication channel
     */
    ReceiverController(final CastV2Channel aChannel) {
        channel = aChannel;
        listeners = new CopyOnWriteArrayList<>();
        channel.addListener(this, RECEIVER_NS);
    }

    /**
     * Returns a new message representing a receiver {@code STOP} request.
     *
     * @param sessionId the application session ID
     * @return a new message representing a receiver {@code STOP} request
     */
    static CastMessage stop(final String sessionId) {
        return build(RECEIVER_NS, new Stop(sessionId));
    }

    /**
     * Returns a new message representing a receiver {@code GET_APP_AVAILABILITY} request.
     *
     * @param appId ID of each application
     * @return a new message representing a receiver {@code GET_APP_AVAILABILITY} request
     */
    private static CastMessage getAppAvailability(final Collection<String> appId) {
        return build(RECEIVER_NS, new AppAvailabilityReq(appId));
    }

    /**
     * Returns a new message representing a receiver {@code GET_STATUS} request.
     *
     * @return a new message representing a receiver {@code GET_STATUS} request
     */
    private static CastMessage getReceiverStatus() {
        return build(RECEIVER_NS, new Message("GET_STATUS"));
    }

    /**
     * Returns a new message representing a receiver {@code LAUNCH} request.
     *
     * @param appId the application ID
     * @return a new message representing a receiver {@code LAUNCH} request
     */
    private static CastMessage launch(final String appId) {
        return build(RECEIVER_NS, new Launch(appId));
    }

    /**
     * Returns a new message representing a receiver {@code SET_VOLUME} level request.
     *
     * @param level volume level
     * @return a new message representing a receiver {@code SET_VOLUME} level request
     */
    private static CastMessage setVolumeLevel(final double level) {
        return build(RECEIVER_NS, new SetVolumeLevel(level));
    }

    /**
     * Returns a new message representing a receiver {@code SET_VOLUME} muted request.
     *
     * @param muted volume muted?
     * @return a new message representing a receiver {@code SET_VOLUME} muted request
     */
    private static CastMessage setVolumeMuted(final boolean muted) {
        return build(RECEIVER_NS, new SetVolumeMuted(muted));
    }

    @Override
    public final void messageReceived(final CastMessage message) {
        final Optional<ReceiverStatus> rs = parse(message, ReceiverStatus.class);
        if (rs.isPresent() && !rs.get().requestId().isPresent()) {
            LOGGER.fine(() -> "Received new device status [" + rs.get() + "]");
            listeners.forEach(l -> l.status(rs.get()));
        }
    }

    @Override
    public final void socketError() {
        // ignore, handled by connection.
    }

    /**
     * Adds the given listener to receive device status events.
     *
     * @param listener listener, not null
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
    final AppAvailabilities appAvailability(final Collection<String> appIds, final Duration timeout)
            throws IOException, TimeoutException {
        final CastMessage resp = new StandardRequestor(channel).request(getAppAvailability(appIds), timeout);
        return parse(resp, AppAvailabilityResp.class)
            .orElseThrow(() -> new IOException("Could not parse received response"));
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
        final CastMessage resp = new StandardRequestor(channel).request(launch(appId), timeout);
        return makeResponse(resp, ReceiverStatus.class);
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
        final CastMessage resp = new StandardRequestor(channel).request(setVolumeMuted(true), timeout);
        return makeResponse(resp, ReceiverStatus.class);
    }

    /**
     * Requests and returns the status of the Cast device.
     *
     * @param timeout status timeout
     * @return the status of the Cast device, never null
     * @throws IOException if the received response is an error or cannot be parsed
     * @throws TimeoutException if the timeout has elapsed before the status was received
     */
    final CastDeviceStatus receiverStatus(final Duration timeout) throws IOException, TimeoutException {
        final CastMessage resp = new StandardRequestor(channel).request(getReceiverStatus(), timeout);
        return makeResponse(resp, ReceiverStatus.class);
    }

    /**
     * Removes the given listener so that it no longer receives device status events.
     *
     * @param listener listener, not null
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
        final CastMessage resp = new StandardRequestor(channel).request(setVolumeLevel(level), timeout);
        return makeResponse(resp, ReceiverStatus.class);
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
        final CastMessage resp = new StandardRequestor(channel).request(stop(sessionId), timeout);
        return makeResponse(resp, ReceiverStatus.class);
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
        final CastMessage resp = new StandardRequestor(channel).request(setVolumeMuted(false), timeout);
        return makeResponse(resp, ReceiverStatus.class);
    }

    /**
     * Builds a new response of the given type or throws an {@link IOException} in case of erro.
     *
     * @param <T> response type
     * @param resp unparsed response
     * @param clazz response class
     * @return a new response of the given type
     * @throws IOException if the unparsed response is an error
     */
    private <T extends Message> T makeResponse(final CastMessage resp, final Class<T> clazz) throws IOException {
        final String type = parse(resp, Message.class).map(Message::type).orElseThrow(
                () -> new IOException("Could not parse received response type"));
        if (ERRORS.contains(type)) {
            throw new IOException(type);
        }
        return parse(resp, clazz).orElseThrow(() -> new IOException("Could not parse received response"));
    }

}