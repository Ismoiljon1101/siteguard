package com.siteguard.adapter.simulation;

import com.siteguard.domain.Acceleration;
import com.siteguard.domain.DeviceEvent;
import com.siteguard.domain.EventType;
import com.siteguard.domain.TelemetryReading;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pull-based synthetic source. No threads, wall clocks, or physical devices are involved. */
public final class SimulationSource {
    private static final String DEVICE_ID = "sim-helmet-001";
    private final List<EventType> pending = new ArrayList<>();
    private Scenario scenario = Scenario.NORMAL;
    private long run;
    private long sequence;
    private long tick;

    public synchronized void setScenario(Scenario scenario) {
        this.scenario = Objects.requireNonNull(scenario, "scenario");
    }

    public synchronized void triggerSos() { pending.add(EventType.SOS); }
    public synchronized void triggerMotion() { pending.add(EventType.MOTION); }

    /** Starts a new simulated session, with fresh source identities and no queued events. */
    public synchronized void reset() {
        run++;
        sequence = 0;
        tick = 0;
        scenario = Scenario.NORMAL;
        pending.clear();
    }

    /** Advances simulated time by one second and returns a reading and queued events. */
    public synchronized SimulationFrame next() {
        long uptime = tick * 1000;
        String bootId = "sim-session-" + run;
        Double temperature = scenario == Scenario.SENSOR_FAULT ? null
                : scenario == Scenario.ENVIRONMENT_WARNING ? 40.0 : 24.0 + (tick % 5) * 0.1;
        Double humidity = scenario == Scenario.SENSOR_FAULT ? null
                : scenario == Scenario.ENVIRONMENT_WARNING ? 85.0 : 50.0 + (tick % 4);
        Acceleration acceleration = scenario == Scenario.SENSOR_FAULT ? null
                : new Acceleration(0.1, 0.0, 9.8);
        TelemetryReading reading = new TelemetryReading(DEVICE_ID, bootId, sequence++, uptime,
                true, temperature, humidity, acceleration);
        List<DeviceEvent> events = new ArrayList<>();
        for (EventType type : pending) {
            events.add(new DeviceEvent(DEVICE_ID, bootId, sequence++, uptime, true, type));
        }
        pending.clear();
        tick++;
        return new SimulationFrame(reading, events);
    }
}
