package com.siteguard.adapter.simulation;

import com.siteguard.domain.Acceleration;
import com.siteguard.domain.DeviceEvent;
import com.siteguard.domain.EventType;
import com.siteguard.domain.TelemetryReading;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic multi-helmet synthetic source. Pull-based: no threads, clocks or hardware. Each tick() is one simulated
 * second. Output goes through the same IngestService path a live source would use. Everything is marked simulated.
 */
public final class FleetSimulator {
    public record Frame(String deviceId, TelemetryReading reading, List<DeviceEvent> events) { }

    /** Decides whether a scripted demo event may fire for a helmet. False suppresses it for that tick. */
    @FunctionalInterface
    public interface ScriptGate { boolean allow(String deviceId, EventType type); }

    private static final class State {
        final int index;
        HelmetScenario scenario;
        long scenarioTick;
        long sequence;
        long frames;
        final List<EventType> pending = new ArrayList<>();
        State(int index, HelmetScenario scenario) { this.index = index; this.scenario = scenario; }
    }

    private final Map<String, State> states = new LinkedHashMap<>();
    private String runId;
    private final ScriptGate gate;

    public FleetSimulator(List<String> deviceIds, String runId) {
        this(deviceIds, runId, (deviceId, type) -> true);
    }

    public FleetSimulator(List<String> deviceIds, String runId, ScriptGate gate) {
        int i = 0;
        for (String id : deviceIds) states.put(id, new State(i++, demoScenario(id)));
        this.runId = Objects.requireNonNull(runId);
        this.gate = Objects.requireNonNull(gate);
    }

    /** Default demo script by helmet number: SIM-007 warning, 008 SOS, 009 motion, 010 sensor fault, 011 stopped, others normal. */
    public static HelmetScenario demoScenario(String deviceId) {
        int n = -1;
        int dash = deviceId.lastIndexOf('-');
        if (dash >= 0) {
            try { n = Integer.parseInt(deviceId.substring(dash + 1)); } catch (NumberFormatException ignored) { n = -1; }
        }
        return switch (n) {
            case 7 -> HelmetScenario.ENVIRONMENT_WARNING;
            case 8 -> HelmetScenario.SOS;
            case 9 -> HelmetScenario.MOTION;
            case 10 -> HelmetScenario.SENSOR_FAULT;
            case 11 -> HelmetScenario.STOPPED;
            default -> HelmetScenario.NORMAL;
        };
    }

    public synchronized List<String> devices() { return List.copyOf(states.keySet()); }

    public synchronized HelmetScenario scenarioOf(String deviceId) { return state(deviceId).scenario; }

    /** Restarts the scenario clock for that helmet. */
    public synchronized void setScenario(String deviceId, HelmetScenario scenario) {
        State s = state(deviceId);
        s.scenario = Objects.requireNonNull(scenario);
        s.scenarioTick = 0;
    }

    public synchronized void trigger(String deviceId, EventType type) { state(deviceId).pending.add(Objects.requireNonNull(type)); }

    /** New simulated boot for every helmet: fresh identities, default scenarios, no queued events. */
    public synchronized void reset(String newRunId) {
        runId = Objects.requireNonNull(newRunId);
        for (Map.Entry<String, State> e : states.entrySet()) {
            State s = e.getValue();
            s.scenario = demoScenario(e.getKey());
            s.scenarioTick = 0;
            s.sequence = 0;
            s.frames = 0;
            s.pending.clear();
        }
    }

    /** Advance every helmet by one simulated second. Stopped helmets emit nothing. */
    public synchronized List<Frame> tick() {
        List<Frame> out = new ArrayList<>();
        String boot = "sim-" + runId;
        for (Map.Entry<String, State> e : states.entrySet()) {
            String id = e.getKey();
            State s = e.getValue();
            if (s.scenario == HelmetScenario.STOPPED && s.scenarioTick >= 3) {
                s.scenarioTick++;
                continue;
            }
            long uptime = s.frames * 1000;
            Double temperature;
            Double humidity;
            switch (s.scenario) {
                case SENSOR_FAULT -> { temperature = null; humidity = null; }
                case ENVIRONMENT_WARNING -> { temperature = 36.0 + (s.frames % 3) * 0.1; humidity = 80.0; }
                default -> { temperature = 24.0 + (s.frames % 5) * 0.1 + s.index * 0.1; humidity = 50.0 + (s.frames % 4); }
            }
            TelemetryReading reading = new TelemetryReading(id, boot, s.sequence++, uptime, true, temperature, humidity,
                    new Acceleration(0.1, 0.0, 9.8));
            List<EventType> types = new ArrayList<>();
            if (s.scenario == HelmetScenario.SOS && s.scenarioTick == 5 && gate.allow(id, EventType.SOS)) types.add(EventType.SOS);
            if (s.scenario == HelmetScenario.MOTION && s.scenarioTick == 8 && gate.allow(id, EventType.MOTION)) types.add(EventType.MOTION);
            types.addAll(s.pending);
            s.pending.clear();
            List<DeviceEvent> events = new ArrayList<>();
            for (EventType type : types) events.add(new DeviceEvent(id, boot, s.sequence++, uptime, true, type));
            s.scenarioTick++;
            s.frames++;
            out.add(new Frame(id, reading, events));
        }
        return out;
    }

    private State state(String deviceId) {
        State s = states.get(deviceId);
        if (s == null) throw new IllegalArgumentException("Unknown simulated helmet " + deviceId);
        return s;
    }
}
