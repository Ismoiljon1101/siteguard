package com.siteguard.domain;

/** Sensor values use Celsius, percent relative humidity and metres per second squared.
 * Null values indicate a sensor fault; they are never stale replacements. */
public record TelemetryReading(String deviceId, String bootId, long sequence, long uptimeMs,
                               boolean simulated, Double temperatureC, Double humidityPct,
                               Acceleration acceleration) {
    public TelemetryReading {
        if (deviceId == null || deviceId.isBlank() || bootId == null || bootId.isBlank()
                || sequence < 0 || uptimeMs < 0) {
            throw new IllegalArgumentException("Invalid source identity or uptime");
        }
        if ((temperatureC == null) != (humidityPct == null)) {
            throw new IllegalArgumentException("Environmental readings must be present or absent together");
        }
        if (temperatureC != null && (!Double.isFinite(temperatureC) || !Double.isFinite(humidityPct)
                || humidityPct < 0 || humidityPct > 100)) {
            throw new IllegalArgumentException("Invalid environmental readings");
        }
    }
}
