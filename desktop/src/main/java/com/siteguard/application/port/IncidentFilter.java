package com.siteguard.application.port;

import com.siteguard.domain.IncidentState;
import com.siteguard.domain.IncidentType;
import java.time.Instant;
import java.util.Set;

/** All fields optional (null or empty means any). Newest incident first. */
public record IncidentFilter(Long siteId, Set<IncidentState> states, IncidentType type, Long zoneId,
                             Long workerId, String deviceId, Instant from, Instant to, int limit, int offset) {
    public IncidentFilter {
        states = states == null ? Set.of() : Set.copyOf(states);
        if (limit <= 0) limit = 100;
        if (offset < 0) offset = 0;
    }

    public static IncidentFilter openForSite(long siteId) {
        return new IncidentFilter(siteId, Set.of(IncidentState.OPEN, IncidentState.ACKNOWLEDGED),
                null, null, null, null, null, null, 1000, 0);
    }

    public static IncidentFilter all() {
        return new IncidentFilter(null, null, null, null, null, null, null, null, 100, 0);
    }
}
