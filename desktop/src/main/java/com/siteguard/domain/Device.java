package com.siteguard.domain;

import java.time.Instant;

public record Device(String deviceId, long siteId, String displayName, boolean simulated,
                     DeviceStatus status, Instant createdAt) {
    public SourceMode sourceMode() { return simulated ? SourceMode.SIMULATED : SourceMode.LIVE; }
}
