package com.siteguard.domain;

import java.time.Instant;

/** Protocol event identity is device/boot/seq. */
public record SourceEvent(String eventId, String deviceId, String bootId, long seq, EventType type,
                          long uptimeMs, Instant receivedAt, Instant committedAt, SourceMode sourceMode) {
    public static String identity(String deviceId, String bootId, long seq) {
        return deviceId + "/" + bootId + "/" + seq;
    }
}
