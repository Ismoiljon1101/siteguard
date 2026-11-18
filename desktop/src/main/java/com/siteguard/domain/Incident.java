package com.siteguard.domain;

import java.time.Instant;

/** Durable incident. assignmentId is the assignment active when it was recorded and never changes. */
public record Incident(String sourceIdentity, IncidentType type, Severity severity, String deviceId,
                       IncidentState state, Instant occurredAt, String sourceMode, Long assignmentId,
                       Long configVersion, Long triggerReadingId, EscalationLevel escalation) { }
