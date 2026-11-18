package com.siteguard.domain;

/** OPEN to ACKNOWLEDGED to RESOLVED. No skipping, no reopening. */
public enum IncidentState {
    OPEN, ACKNOWLEDGED, RESOLVED;

    public boolean canMoveTo(IncidentState next) {
        return (this == OPEN && next == ACKNOWLEDGED) || (this == ACKNOWLEDGED && next == RESOLVED);
    }
}
