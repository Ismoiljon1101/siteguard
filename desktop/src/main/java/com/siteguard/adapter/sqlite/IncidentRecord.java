package com.siteguard.adapter.sqlite;

/** Durable incident snapshot. Source identity must be stable across retries. */
public record IncidentRecord(String sourceIdentity, String type, String severity,
                             String deviceId, String state, String occurredAt,
                             String sourceMode) {
    public IncidentRecord {
        if (sourceIdentity == null || sourceIdentity.isBlank() || type == null || type.isBlank()
                || severity == null || severity.isBlank() || deviceId == null || deviceId.isBlank()
                || state == null || state.isBlank() || occurredAt == null || occurredAt.isBlank()
                || sourceMode == null || sourceMode.isBlank()) {
            throw new IllegalArgumentException("Incident fields must not be blank");
        }
    }
}
