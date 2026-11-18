package com.siteguard.domain;

import java.time.Instant;

/** endsAt is null while the assignment is active. assignedBy is null for system/seed rows. */
public record HelmetAssignment(long assignmentId, String deviceId, long workerId, Long assignedBy,
                               Instant startsAt, Instant endsAt) {
    public boolean active() { return endsAt == null; }
}
