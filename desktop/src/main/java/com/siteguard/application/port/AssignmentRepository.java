package com.siteguard.application.port;

import com.siteguard.domain.AssignmentZone;
import com.siteguard.domain.HelmetAssignment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AssignmentRepository {
    HelmetAssignment start(String deviceId, long workerId, Long assignedBy, Instant at);
    void end(long assignmentId, Instant at);
    Optional<HelmetAssignment> find(long assignmentId);
    Optional<HelmetAssignment> activeForDevice(String deviceId);
    Optional<HelmetAssignment> activeForWorker(long workerId);
    AssignmentZone setZone(long assignmentId, long zoneId, Long setBy, Instant at);
    Optional<AssignmentZone> currentZone(long assignmentId);
    /** Newest first. */
    List<HelmetAssignment> historyForWorker(long workerId);
    List<ActiveAssignment> activeForSite(long siteId);
}
