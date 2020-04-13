/*
Copyright 2020-2020 Cedric Liegeois

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
package io.omam.wire.media;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.omam.wire.Payload;
import io.omam.wire.Volume;

/**
 * Payloads of requests send to the default media receiver application.
 */
@SuppressWarnings("javadoc")
final class Payloads {

    /**
     * Get Status request payload.
     */
    static final class GetStatus extends Payload {

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
     * LOAD request.
     */
    @SuppressWarnings("unused")
    static final class Load extends Payload {

        /** session ID. */
        private final String sessionId;

        /** media. */
        private final MediaData media;

        /** true if media should be played when loaded. */
        private final boolean autoplay;

        /** current time. */
        private final double currentTime;

        /** queue data, null if no queue. */
        private final QueueData queueData;

        /**
         * Constructor.
         *
         * @param aSessionId    session ID
         * @param aMedia        media
         * @param isAutoplay    true if media should be played when loaded
         * @param aCurrentTime  current time
         * @param someQueueData queue data, null if no queue
         */
        Load(final String aSessionId, final MediaData aMedia, final boolean isAutoplay, final double aCurrentTime,
                final QueueData someQueueData) {
            super("LOAD", null);
            sessionId = aSessionId;
            media = aMedia;
            autoplay = isAutoplay;
            currentTime = aCurrentTime;
            queueData = someQueueData;
        }
    }

    static final class MediaData implements Media {

        private final String contentId;

        private final StreamType streamType;

        private final Double duration;

        private final String contentType;

        MediaData(final String aContentId, final String aContentType, final StreamType aStreamType) {
            contentId = aContentId;
            duration = null;
            contentType = aContentType;
            streamType = aStreamType;
        }

        @Override
        public final String contentId() {
            return contentId;
        }

        @Override
        public final String contentType() {
            return contentType;
        }

        @Override
        public final Optional<Duration> duration() {
            return duration == null ? Optional.empty() : Optional.of(toDuration(duration));
        }

        @Override
        public final StreamType streamType() {
            return streamType;
        }
    }

    static final class MediaStatusData extends Payload implements MediaStatus {

        private double currentTime;

        private IdleReason idleReason;

        private List<QueueItemData> items;

        private MediaData media;

        private int mediaSessionId;

        private int playbackRate;

        private PlayerState playerState;

        private RepeatMode repeatMode;

        private VolumeData volume;

        private MediaStatusData() {

        }

        @Override
        public final double currentTime() {
            return currentTime;
        }

        @Override
        public final Optional<IdleReason> idleReason() {
            return Optional.ofNullable(idleReason);
        }

        @Override
        public final List<QueueItem> items() {
            return items == null ? Collections.emptyList() : Collections.unmodifiableList(items);
        }

        @Override
        public final Optional<Media> media() {
            return Optional.ofNullable(media);
        }

        @Override
        public final int mediaSessionId() {
            return mediaSessionId;
        }

        @Override
        public final int playbackRate() {
            return playbackRate;
        }

        @Override
        public final PlayerState playerState() {
            return playerState;
        }

        @Override
        public final RepeatMode repeatMode() {
            return repeatMode;
        }

        @Override
        public final Volume volume() {
            return volume;
        }
    }

    static final class MediaStatusResponse extends Payload {

        private List<MediaStatusData> status;

        private MediaStatusResponse() {
            // empty.
        }

        final Optional<MediaStatusData> status() {
            return status.isEmpty() ? Optional.empty() : Optional.of(status.get(0));
        }

    }

    static final class Pause extends MediaRequest {

        Pause(final int aMediaSessionId) {
            super("PAUSE", aMediaSessionId);
        }

    }

    static final class Play extends MediaRequest {

        Play(final int aMediaSessionId) {
            super("PLAY", aMediaSessionId);
        }

    }

    /**
     * Queue data as part of the LOAD request.
     */
    @SuppressWarnings("unused")
    static final class QueueData {

        /** list of queue items. */
        private final List<QueueItem> items;

        /** queue repeat mode. */
        private final RepeatMode repeatMode;

        /**
         * Constructor.
         *
         * @param someItems   list of queue items
         * @param aRepeatMode queue repeat mode
         */
        QueueData(final List<QueueItem> someItems, final RepeatMode aRepeatMode) {
            items = someItems;
            repeatMode = aRepeatMode;
        }

    }

    static final class QueueItemData implements QueueItem {

        private final boolean autoplay;

        private final MediaData media;

        private final double preloadTime;

        private final double startTime;

        QueueItemData(final boolean isAutoplay, final MediaData aMedia, final double aPreloadTime,
                final double aStartTime) {
            autoplay = isAutoplay;
            media = aMedia;
            preloadTime = aPreloadTime;
            startTime = aStartTime;
        }

        @Override
        public final boolean autoplay() {
            return autoplay;
        }

        @Override
        public final Media media() {
            return media;
        }

        @Override
        public final Duration preloadTime() {
            return toDuration(preloadTime);
        }

        @Override
        public final Duration startTime() {
            return toDuration(startTime);
        }
    }

    static final class QueueUpdate extends MediaRequest {

        @SuppressWarnings("unused")
        private final int jump;

        QueueUpdate(final int aMediaSessionId, final int aJump) {
            super("QUEUE_UPDATE", aMediaSessionId);
            jump = aJump;
        }

    }

    @SuppressWarnings("unused")
    static final class Seek extends MediaRequest {

        private final double currentTime;

        private final String resumeState;

        Seek(final int aMediaSessionId, final double aCurrentTime) {
            super("SEEK", aMediaSessionId);
            currentTime = aCurrentTime;
            resumeState = "PLAYBACK_START";
        }

    }

    static final class Stop extends MediaRequest {

        Stop(final int aMediaSessionId) {
            super("STOP", aMediaSessionId);
        }

    }

    private static abstract class MediaRequest extends Payload {

        @SuppressWarnings("unused")
        private final int mediaSessionId;

        protected MediaRequest(final String aType, final int aMediaSessionId) {
            super(aType, null);
            mediaSessionId = aMediaSessionId;
        }

    }

    /**
     * Media volume.
     */
    private static final class VolumeData implements Volume {

        /** {@link #level()}. */
        private double level;

        /** {@link #isMuted()}. */
        private boolean muted;

        /**
         * Constructor.
         */
        @SuppressWarnings("unused")
        VolumeData() {
            // empty.
        }

        @Override
        public boolean isMuted() {
            return muted;
        }

        @Override
        public double level() {
            return level;
        }

    }

    static Duration toDuration(final double seconds) {
        return Duration.ofMillis((long) (seconds * 1000));
    }
}