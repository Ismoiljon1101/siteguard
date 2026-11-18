package com.siteguard.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.siteguard.adapter.simulation.FleetSimulator;
import com.siteguard.application.port.IncidentFilter;
import com.siteguard.application.port.IncidentView;
import com.siteguard.domain.IncidentState;
import com.siteguard.domain.IncidentType;
import com.siteguard.domain.ResolutionCategory;
import com.siteguard.testsupport.Fixture;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScriptedDemoIncidentTest {
    private static final String HELMET = "SIM-008";
    @TempDir Path dir;
    Fixture f;

    @BeforeEach void setUp() {
        f = Fixture.create(dir);
        f.store.inTransaction(r -> r.devices().register(HELMET, f.siteId, HELMET, true, f.clock.instant()));
    }

    /** One simulated app launch on the same database: the same wiring as AppContext, then six simulated seconds. */
    private void launch(Fixture g, String runId) {
        FleetSimulator.ScriptGate gate = (deviceId, type) ->
                !g.store.read(r -> r.incidents().hasUnresolved(deviceId, IncidentType.fromEvent(type)));
        SimulationRunner runner = new SimulationRunner(new FleetSimulator(List.of(HELMET), runId, gate),
                g.ingest, g.health, g.incidents, () -> runId);
        for (int i = 0; i < 6; i++) {
            g.clock.advance(Duration.ofSeconds(1));
            runner.tick();
        }
        runner.close();
    }

    private List<IncidentView> openSos(Fixture g) {
        return g.store.read(r -> r.incidents().search(new IncidentFilter(null, Set.of(IncidentState.OPEN),
                IncidentType.SOS, null, null, HELMET, null, null, 100, 0)));
    }

    @Test void scriptedSosIsNotRepeatedWhileUnresolvedAndReturnsAfterResolution() {
        launch(f, "run1");
        launch(f.restart(), "run2");
        assertEquals(1, openSos(f).size());

        IncidentView sos = openSos(f).get(0);
        f.incidents.acknowledge(sos.incident().sourceIdentity(), f.supervisorId);
        f.incidents.resolve(sos.incident().sourceIdentity(), f.supervisorId, ResolutionCategory.TEST, "demo done");

        launch(f.restart(), "run3");
        assertEquals(1, openSos(f).size());
        assertEquals(2, f.store.read(r -> r.incidents().search(new IncidentFilter(null, null,
                IncidentType.SOS, null, null, HELMET, null, null, 100, 0))).size());
    }
}
