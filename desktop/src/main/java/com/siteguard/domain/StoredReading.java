package com.siteguard.domain;

import java.time.Instant;

/** Null temperature/humidity mean environment fault; null acceleration means motion fault. */
public record StoredReading(long readingId, String deviceId, String bootId, long seq, long uptimeMs,
                            Instant receivedAt, Double temperatureC, Double humidityPct,
                            Acceleration acceleration, SourceMode sourceMode) {
    public boolean environmentFault() { return temperatureC == null; }
    public boolean motionFault() { return acceleration == null; }
}
