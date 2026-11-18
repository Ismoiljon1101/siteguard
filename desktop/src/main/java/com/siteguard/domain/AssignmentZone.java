package com.siteguard.domain;

import java.time.Instant;

public record AssignmentZone(long assignmentZoneId, long assignmentId, long zoneId, Long setBy, Instant setAt) { }
