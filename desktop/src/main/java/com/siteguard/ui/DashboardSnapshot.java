package com.siteguard.ui;

import java.util.List;

/** Immutable display data. A null measurement means unavailable, never zero. */
public record DashboardSnapshot(String deviceId, String deviceStatus, Double temperatureC,
        Double humidityPct, Double accelerationX, Double accelerationY, Double accelerationZ,
        String lastUpdate, String storageStatus, List<IncidentRow> incidents) {
    public DashboardSnapshot {
        incidents = List.copyOf(incidents);
    }
}
