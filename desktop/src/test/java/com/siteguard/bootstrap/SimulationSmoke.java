package com.siteguard.bootstrap;

import com.siteguard.adapter.simulation.Scenario;
import com.siteguard.ui.DashboardSnapshot;
import com.siteguard.ui.IncidentRow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/** Offline integration smoke test; the normal JUnit suite tests individual adapters. */
public final class SimulationSmoke {
    private static final LinkedBlockingQueue<DashboardSnapshot> snapshots = new LinkedBlockingQueue<>();

    private static DashboardSnapshot await(Predicate<DashboardSnapshot> condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        while (System.nanoTime() < deadline) {
            DashboardSnapshot next = snapshots.poll(500, TimeUnit.MILLISECONDS);
            if (next != null && condition.test(next)) return next;
        }
        throw new AssertionError("Timed out waiting for expected simulation state");
    }

    private static boolean hasState(DashboardSnapshot snapshot, String id, String state) {
        return snapshot.incidents().stream().anyMatch(row -> row.id().equals(id) && row.state().equals(state));
    }

    public static void main(String[] args) throws Exception {
        SimulationController controller = new SimulationController();
        try {
            controller.start(snapshots::offer);
            await(s -> s.deviceStatus().contains("SIMULATED") && s.temperatureC() != null);
            controller.triggerSos();
            DashboardSnapshot sos = await(s -> s.incidents().stream().anyMatch(row -> row.type().equals("SOS")));
            IncidentRow incident = sos.incidents().stream().filter(row -> row.type().equals("SOS")).findFirst().orElseThrow();
            if (!incident.state().equals("OPEN")) throw new AssertionError("SOS should start open");
            controller.acknowledge(incident.id(), "Test operator");
            await(s -> hasState(s, incident.id(), "ACKNOWLEDGED"));
            controller.resolve(incident.id(), "Test operator", "Demo completed");
            DashboardSnapshot resolved = await(s -> hasState(s, incident.id(), "RESOLVED"));
            if (resolved.incidents().stream().noneMatch(row -> row.id().equals(incident.id()) && row.detail().equals("Demo completed")))
                throw new AssertionError("Resolution note missing");
            controller.acknowledge(incident.id(), "Test operator");
            await(s -> s.storageStatus().contains("Action rejected") && hasState(s, incident.id(), "RESOLVED"));
            controller.setScenario(Scenario.ENVIRONMENT_WARNING);
            await(s -> s.incidents().stream().anyMatch(row -> row.type().equals("ENVIRONMENT")));
            controller.setScenario(Scenario.SENSOR_FAULT);
            await(s -> s.deviceStatus().contains("FAULT") && s.temperatureC() == null && s.humidityPct() == null);
            System.out.println("PASS: simulation, SOS, acknowledgement, resolution, invalid transition, warning and fault");
        } finally {
            controller.close();
        }
    }
}
