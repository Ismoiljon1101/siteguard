package com.siteguard.domain;

import java.time.Instant;

/** Append-only history row. operatorId is null for system actions. */
public record IncidentAction(long id, String sourceIdentity, ActionType action, String operatorName,
                             Long operatorId, String note, ResolutionCategory category, Instant recordedAt) { }
