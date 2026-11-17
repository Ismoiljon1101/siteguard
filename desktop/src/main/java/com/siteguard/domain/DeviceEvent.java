package com.siteguard.domain;

public record DeviceEvent(String deviceId, String bootId, long sequence, long uptimeMs,
                          boolean simulated, EventType type) {
    public DeviceEvent {
        if (deviceId == null || deviceId.isBlank() || bootId == null || bootId.isBlank()
                || sequence < 0 || uptimeMs < 0 || type == null) {
            throw new IllegalArgumentException("Invalid event identity, uptime, or type");
        }
    }
}
