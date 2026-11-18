package com.siteguard.application.port;

import com.siteguard.domain.SourceEvent;
import com.siteguard.domain.StoredReading;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

public interface ReadingRepository {
    boolean readingExists(String deviceId, String bootId, long seq);
    boolean eventExists(String eventId);
    /** Create the session if needed and move last_seen_at forward, never backward. */
    void touchSession(String deviceId, String bootId, Instant now);
    /** True when the boot is the newest session of the device (latest first_seen_at). */
    boolean isCurrentBoot(String deviceId, String bootId);
    OptionalLong maxSeq(String deviceId, String bootId);
    Optional<Instant> lastStoredAt(String deviceId);
    Optional<Instant> lastSeen(String deviceId);
    StoredReading insert(StoredReading reading);
    boolean insertEvent(SourceEvent event);
    /** Readings of this boot with seq below beforeSeq, newest first. */
    List<StoredReading> previous(String deviceId, String bootId, long beforeSeq, int limit);
    /** Current reading: newest boot, highest seq. Late arrivals never move it backward. */
    Optional<StoredReading> latest(String deviceId);
    /** Oldest first. */
    List<StoredReading> range(String deviceId, Instant from, Instant to);
}
