package com.siteguard.adapter.simulation;

import com.siteguard.domain.DeviceEvent;
import com.siteguard.domain.TelemetryReading;
import java.util.List;
import java.util.Objects;

public record SimulationFrame(TelemetryReading reading, List<DeviceEvent> events) {
    public SimulationFrame {
        Objects.requireNonNull(reading, "reading");
        events = List.copyOf(events);
    }
}
