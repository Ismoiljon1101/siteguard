package com.siteguard.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.siteguard.application.port.IncidentFilter;
import com.siteguard.application.port.IncidentView;
import com.siteguard.domain.DeviceEvent;
import com.siteguard.domain.EventType;
import com.siteguard.domain.HelmetAssignment;
import com.siteguard.domain.HealthState;
import com.siteguard.testsupport.Fixture;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AssignmentServiceTest {
    @TempDir Path dir;
    Fixture f;

    @BeforeEach void setUp() { f = Fixture.create(dir); }

    private long activeCountForDevice(String device) {
        return f.store.read(r -> r.assignments().activeForDevice(device).isPresent() ? 1L : 0L);
    }

    @Test void oneActiveAssignmentPerHelmetAndPerWorker() {
        HelmetAssignment a1 = f.assignments.assign(f.supervisorId, "H1", f.worker1, f.zone1);
        f.clock.advance(Duration.ofSeconds(1));
        // Same helmet to another worker: the first ends.
        HelmetAssignment a2 = f.assignments.assign(f.supervisorId, "H1", f.worker2, null);
        assertNotEquals(a1.assignmentId(), a2.assignmentId());
        assertEquals(a2.assignmentId(), f.store.read(r -> r.assignments().activeForDevice("H1")).orElseThrow().assignmentId());
        assertTrue(f.store.read(r -> r.assignments().activeForWorker(f.worker1)).isEmpty());
        f.clock.advance(Duration.ofSeconds(1));
        // Same worker to another helmet: the helmet H1 assignment ends.
        f.assignments.assign(f.supervisorId, "H2", f.worker2, f.zone2);
        assertTrue(f.store.read(r -> r.assignments().activeForDevice("H1")).isEmpty());
        assertEquals(1, activeCountForDevice("H2"));
        assertEquals(HealthState.UNASSIGNED, f.health.current().get("H1"));
    }

    @Test void databaseRejectsSecondActiveAssignmentPerHelmetOrWorker() throws Exception {
        f.assignments.assign(f.supervisorId, "H1", f.worker1, null);
        try (var c = f.database.open(true)) {
            assertThrows(SQLException.class, () -> c.createStatement().executeUpdate(
                    "INSERT INTO helmet_assignments(device_id, worker_id, starts_at) VALUES('H1', " + f.worker2 + ", '2026-01-01T00:00:00.000Z')"));
            assertThrows(SQLException.class, () -> c.createStatement().executeUpdate(
                    "INSERT INTO helmet_assignments(device_id, worker_id, starts_at) VALUES('H2', " + f.worker1 + ", '2026-01-01T00:00:00.000Z')"));
        }
    }

    @Test void reassignmentKeepsOldIncidentOnOriginalAssignment() {
        HelmetAssignment first = f.assignments.assign(f.supervisorId, "H1", f.worker1, f.zone1);
        f.clock.advance(Duration.ofSeconds(1));
        String id = f.ingest.ingest(new DeviceEvent("H1", "b1", 1, 1000, true, EventType.SOS)).incidentIdentity();
        f.clock.advance(Duration.ofSeconds(1));
        HelmetAssignment second = f.assignments.assign(f.supervisorId, "H1", f.worker2, f.zone2);
        IncidentView old = f.store.read(r -> r.incidents().view(id)).orElseThrow();
        assertEquals(first.assignmentId(), old.incident().assignmentId());
        assertEquals("W1", old.workerName());
        assertEquals("Z1", old.zoneName());
        f.clock.advance(Duration.ofSeconds(1));
        String id2 = f.ingest.ingest(new DeviceEvent("H1", "b1", 2, 2000, true, EventType.MOTION)).incidentIdentity();
        assertEquals(second.assignmentId(), f.store.read(r -> r.incidents().find(id2)).orElseThrow().assignmentId());
        // Row on the new worker does not show the old worker's SOS.
        var rows = f.queries.workers(f.supervisorId, SiteQueryService.WorkerQuery.firstPage(f.siteId)).rows();
        assertEquals(1, rows.size());
        assertEquals("W2", rows.get(0).workerName());
        assertEquals(id2, rows.get(0).incidentIdentity());
        // History of W1 keeps both its assignment and its incident.
        var detail = f.queries.workerDetail(f.supervisorId, f.worker1).orElseThrow();
        assertEquals(1, detail.incidents().size());
        assertEquals(1, detail.history().size());
        assertNotNull(detail.history().get(0).assignment().endsAt());
    }

    @Test void failedReassignmentRollsBackAndKeepsTheOldAssignment() {
        f.assignments.assign(f.supervisorId, "H1", f.worker1, f.zone1);
        assertThrows(NotFoundException.class, () -> f.assignments.assign(f.supervisorId, "H1", 9999, f.zone1));
        assertEquals(f.worker1, f.store.read(r -> r.assignments().activeForDevice("H1")).orElseThrow().workerId());
        // A storage-level failure after ending the old row (zone from another site slips past: force an FK failure).
        assertThrows(RuntimeException.class, () -> f.store.inTransaction(r -> {
            r.assignments().end(r.assignments().activeForDevice("H1").orElseThrow().assignmentId(), f.clock.instant());
            r.assignments().start("H1", 424242, null, f.clock.instant());
            return null;
        }));
        assertTrue(f.store.read(r -> r.assignments().activeForDevice("H1")).isPresent(), "ending the old row rolled back");
    }

    @Test void zoneMustBelongToTheHelmetSite() {
        assertThrows(ValidationException.class, () -> f.assignments.assign(f.supervisorId, "H1", f.worker1, f.otherZone));
        HelmetAssignment a = f.assignments.assign(f.supervisorId, "H1", f.worker1, null);
        assertThrows(ValidationException.class, () -> f.assignments.setZone(f.supervisorId, a.assignmentId(), f.otherZone));
        assertTrue(f.store.read(r -> r.assignments().currentZone(a.assignmentId())).isEmpty());
        f.assignments.setZone(f.supervisorId, a.assignmentId(), f.zone2);
        assertEquals(f.zone2, f.store.read(r -> r.assignments().currentZone(a.assignmentId())).orElseThrow().zoneId());
    }

    @Test void databaseTriggerAlsoBlocksCrossSiteZone() throws Exception {
        HelmetAssignment a = f.assignments.assign(f.supervisorId, "H1", f.worker1, null);
        try (var c = f.database.open(true)) {
            assertThrows(SQLException.class, () -> c.createStatement().executeUpdate(
                    "INSERT INTO assignment_zones(assignment_id, zone_id, set_at) VALUES(" + a.assignmentId() + ", " + f.otherZone + ", '2026-01-01T00:00:00.000Z')"));
            f.assignments.setZone(f.supervisorId, a.assignmentId(), f.zone1);
            assertThrows(SQLException.class, () -> c.createStatement().executeUpdate("DELETE FROM assignment_zones"));
            assertThrows(SQLException.class, () -> c.createStatement().executeUpdate("UPDATE assignment_zones SET zone_id = " + f.zone2));
        }
    }

    @Test void siteScopingBlocksOutsiders() {
        assertThrows(AuthorizationException.class, () -> f.assignments.assign(f.outsiderId, "H1", f.worker1, null));
        assertTrue(f.store.read(r -> r.assignments().activeForDevice("H1")).isEmpty());
        f.assignments.assign(f.directorId, "H1", f.worker1, null);
    }

    @Test void endAssignmentAndHistory() {
        HelmetAssignment a = f.assignments.assign(f.supervisorId, "H1", f.worker1, null);
        f.clock.advance(Duration.ofHours(1));
        f.assignments.endAssignment(f.supervisorId, a.assignmentId());
        assertThrows(InvalidTransitionException.class, () -> f.assignments.endAssignment(f.supervisorId, a.assignmentId()));
        List<HelmetAssignment> h = f.assignments.history(f.worker1);
        assertEquals(1, h.size());
        assertEquals(f.clock.instant(), h.get(0).endsAt());
        assertEquals(HealthState.UNASSIGNED, f.health.current().get("H1"));
    }
}
