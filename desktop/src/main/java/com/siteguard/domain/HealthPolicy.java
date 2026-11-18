package com.siteguard.domain;

import java.time.Duration;
import java.time.Instant;

/** Pure health rules. Stale after 5 s (D8). OFFLINE is a PLACEHOLDER: no data for 30 s (see PROPOSALS_FOR_TL). */
public final class HealthPolicy {
    public static final Duration STALE_AFTER = Duration.ofSeconds(5);
    public static final Duration OFFLINE_AFTER = Duration.ofSeconds(30);

    private HealthPolicy() { }

    public static HealthState evaluate(boolean assigned, Instant assignmentStart, Instant lastSeen,
                                       boolean sensorFault, Instant now) {
        if (!assigned) return HealthState.UNASSIGNED;
        if (lastSeen == null || (assignmentStart != null && lastSeen.isBefore(assignmentStart))) return HealthState.NO_DATA;
        Duration age = Duration.between(lastSeen, now);
        if (age.compareTo(OFFLINE_AFTER) >= 0) return HealthState.OFFLINE;
        if (age.compareTo(STALE_AFTER) >= 0) return HealthState.STALE;
        return sensorFault ? HealthState.SENSOR_FAULT : HealthState.ONLINE;
    }
}
