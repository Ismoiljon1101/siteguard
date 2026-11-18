package com.siteguard.bootstrap;

import com.siteguard.adapter.simulation.FleetSimulator;
import com.siteguard.adapter.simulation.HelmetScenario;
import com.siteguard.application.service.HealthService;
import com.siteguard.application.service.IncidentService;
import com.siteguard.application.service.IngestService;
import com.siteguard.domain.DeviceEvent;
import com.siteguard.domain.EventType;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Drives the fleet simulator at 1 Hz on its own thread (never the Swing thread). Every frame goes through the same
 * IngestService a live source would use; then health is swept and SOS escalation updated. All data is SIMULATED.
 */
public final class SimulationRunner implements AutoCloseable {
    private final FleetSimulator simulator;
    private final IngestService ingest;
    private final HealthService health;
    private final IncidentService incidents;
    private final Supplier<String> runIds;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread t = new Thread(task, "siteguard-simulation");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> future;
    private volatile Throwable lastError;

    SimulationRunner(FleetSimulator simulator, IngestService ingest, HealthService health, IncidentService incidents,
                     Supplier<String> runIds) {
        this.simulator = simulator;
        this.ingest = ingest;
        this.health = health;
        this.incidents = incidents;
        this.runIds = runIds;
    }

    public synchronized void start() {
        if (future != null) return;
        future = executor.scheduleAtFixedRate(() -> {
            try { tick(); } catch (RuntimeException e) { lastError = e; }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (future != null) { future.cancel(false); future = null; }
    }

    public synchronized boolean running() { return future != null; }

    /** The last tick failure, or null. A failed tick does not stop the simulation. */
    public Throwable lastError() { return lastError; }

    /** One simulated second, synchronously. The scheduler calls this; tests and tools may call it directly. */
    public void tick() {
        for (FleetSimulator.Frame frame : simulator.tick()) {
            ingest.ingest(frame.reading());
            for (DeviceEvent event : frame.events()) ingest.ingest(event);
        }
        health.sweep();
        incidents.escalateOverdue();
    }

    /** Start a new simulated boot: new source identities, default scenarios. Stored history is kept. */
    public void reset() { simulator.reset(runIds.get()); }

    public List<String> helmets() { return simulator.devices(); }
    public HelmetScenario scenarioOf(String deviceId) { return simulator.scenarioOf(deviceId); }
    public void setScenario(String deviceId, HelmetScenario scenario) { simulator.setScenario(deviceId, scenario); }
    public void trigger(String deviceId, EventType type) { simulator.trigger(deviceId, type); }

    @Override public void close() {
        stop();
        executor.shutdownNow();
    }
}
