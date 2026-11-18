package com.siteguard.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.siteguard.application.port.IncidentFilter;
import com.siteguard.application.port.IncidentView;
import com.siteguard.domain.DeviceEvent;
import com.siteguard.domain.EventType;
import com.siteguard.domain.HealthState;
import com.siteguard.domain.IncidentState;
import com.siteguard.domain.IncidentType;
import com.siteguard.domain.Severity;
import com.siteguard.domain.TelemetryReading;
import com.siteguard.testsupport.Fixture;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IngestRulesTest {
    @TempDir Path dir;
    Fixture f;

    @BeforeEach void setUp() { f = Fixture.create(dir); }

    private List<IncidentView> incidents() {
        return f.store.read(r -> r.incidents().search(IncidentFilter.all()));
    }

    private void assign(String device, long worker) {
        f.assignments.assign(f.supervisorId, device, worker, f.zone1);
    }

    @Test void twoHotSamplesDoNotTriggerThreeDoes() {
        assign("H1", f.worker1);
        f.feed("H1", 1, 36.0);
        f.feed("H1", 2, 36.0);
        assertTrue(incidents().isEmpty(), "2 samples must not create an incident");
        IngestResult third = f.feed("H1", 3, 36.0);
        assertNotNull(third.incidentIdentity());
        assertEquals(1, incidents().size());
        IncidentView v = incidents().get(0);
        assertEquals(IncidentType.ENVIRONMENT, v.incident().type());
        assertEquals(Severity.WARNING, v.incident().severity());
        assertEquals(1L, v.incident().configVersion());
        assertNotNull(v.incident().triggerReadingId());
        assertNotNull(v.incident().assignmentId());
        assertEquals("H1/b1/ENVIRONMENT/3", v.incident().sourceIdentity());
    }

    @Test void sustainedViolationCreatesNoSpamWhileUnresolved() {
        assign("H1", f.worker1);
        for (int i = 1; i <= 10; i++) f.feed("H1", i, 40.0);
        assertEquals(1, incidents().size());
    }

    @Test void boundary34_9DoesNotTriggerAnd35_0Does() {
        assign("H1", f.worker1);
        assign("H2", f.worker2);
        for (int i = 1; i <= 5; i++) f.feed("H1", i, 34.9);
        assertTrue(incidents().isEmpty());
        for (int i = 1; i <= 3; i++) f.feed("H2", i, 35.0);
        assertEquals(1, incidents().size());
        assertEquals("H2", incidents().get(0).incident().deviceId());
    }

    @Test void coolSampleOrSensorFaultBreaksTheRun() {
        assign("H1", f.worker1);
        f.feed("H1", 1, 36.0);
        f.feed("H1", 2, 36.0);
        f.feed("H1", 3, 30.0);
        f.feed("H1", 4, 36.0);
        f.feed("H1", 5, 36.0);
        assertTrue(incidents().isEmpty());
        f.feed("H1", 6, null);
        f.feed("H1", 7, 36.0);
        f.feed("H1", 8, 36.0);
        assertTrue(incidents().isEmpty(), "a fault sample breaks consecutive samples");
        f.feed("H1", 9, 36.0);
        assertEquals(1, incidents().size());
    }

    @Test void newIncidentAllowedAfterResolutionIfStillHot() {
        assign("H1", f.worker1);
        for (int i = 1; i <= 3; i++) f.feed("H1", i, 40.0);
        String id = incidents().get(0).incident().sourceIdentity();
        f.incidents.acknowledge(id, f.supervisorId);
        f.incidents.resolve(id, f.supervisorId, com.siteguard.domain.ResolutionCategory.TEST, "checked");
        f.feed("H1", 4, 40.0);
        assertEquals(2, incidents().size());
    }

    @Test void conditionClearedNoticeWhenSampleBelowLimitArrives() {
        assign("H1", f.worker1);
        for (int i = 1; i <= 3; i++) f.feed("H1", i, 40.0);
        String id = incidents().get(0).incident().sourceIdentity();
        assertFalse(f.incidents.detail(f.supervisorId, id).orElseThrow().environmentConditionCleared());
        f.feed("H1", 4, 34.9);
        assertTrue(f.incidents.detail(f.supervisorId, id).orElseThrow().environmentConditionCleared());
        assertEquals(IncidentState.OPEN, incidents().get(0).incident().state(), "recovery never resolves an incident");
    }

    @Test void sosEventCreatesOneHighIncidentAndDuplicateIsIgnored() {
        assign("H1", f.worker1);
        f.clock.advance(Duration.ofSeconds(1));
        DeviceEvent sos = new DeviceEvent("H1", "b1", 12, 12420, true, EventType.SOS);
        IngestResult first = f.ingest.ingest(sos);
        assertEquals(IngestResult.Status.STORED, first.status());
        assertTrue(first.receiptStored());
        IngestResult again = f.ingest.ingest(sos);
        assertEquals(IngestResult.Status.DUPLICATE, again.status());
        assertTrue(again.receiptStored());
        assertEquals("H1/b1/12", again.incidentIdentity());
        List<IncidentView> all = incidents();
        assertEquals(1, all.size());
        assertEquals(Severity.HIGH, all.get(0).incident().severity());
        assertEquals(IncidentState.OPEN, all.get(0).incident().state());
        var actions = f.store.read(r -> r.incidents().actions("H1/b1/12"));
        assertEquals(1, actions.size());
        assertEquals("system", actions.get(0).operatorName());
    }

    @Test void motionEventIsAWarningWhileThresholdIsNull() {
        assign("H1", f.worker1);
        f.ingest.ingest(new DeviceEvent("H1", "b1", 3, 3000, true, EventType.MOTION));
        IncidentView v = incidents().get(0);
        assertEquals(IncidentType.MOTION, v.incident().type());
        assertEquals(Severity.WARNING, v.incident().severity());
        assertNull(f.store.read(r -> r.config().current()).motionThresholdMps2());
    }

    @Test void atMostOneStoredReadingPerSecond() {
        assign("H1", f.worker1);
        f.clock.advance(Duration.ofSeconds(1));
        assertEquals(IngestResult.Status.STORED, f.ingest.ingest(f.reading("H1", "b1", 1, 25.0)).status());
        f.clock.advance(Duration.ofMillis(400));
        assertEquals(IngestResult.Status.RATE_LIMITED, f.ingest.ingest(f.reading("H1", "b1", 2, 25.0)).status());
        f.clock.advance(Duration.ofMillis(600));
        assertEquals(IngestResult.Status.STORED, f.ingest.ingest(f.reading("H1", "b1", 3, 25.0)).status());
        int count = f.store.read(r -> r.readings().range("H1", f.clock.instant().minusSeconds(60), f.clock.instant()).size());
        assertEquals(2, count);
    }

    @Test void duplicateReadingChangesNothing() {
        assign("H1", f.worker1);
        f.feed("H1", 1, 25.0);
        f.clock.advance(Duration.ofSeconds(2));
        assertEquals(IngestResult.Status.DUPLICATE, f.ingest.ingest(f.reading("H1", "b1", 1, 25.0)).status());
        int stored = f.store.read(r -> r.readings().range("H1", Fixture.START, f.clock.instant()).size());
        assertEquals(1, stored);
    }

    @Test void outOfOrderReadingIsStoredButNeverMovesCurrentBackward() {
        assign("H1", f.worker1);
        f.feed("H1", 5, 25.0);
        IngestResult late = f.feed("H1", 3, 40.0);
        assertEquals(IngestResult.Status.STORED_OUT_OF_ORDER, late.status());
        var latest = f.store.read(r -> r.readings().latest("H1")).orElseThrow();
        assertEquals(5, latest.seq());
        assertEquals(25.0, latest.temperatureC());
        // Older boot arriving after a reboot is also out of order.
        f.clock.advance(Duration.ofSeconds(1));
        assertEquals(IngestResult.Status.STORED, f.ingest.ingest(f.reading("H1", "b2", 0, 26.0)).status());
        f.clock.advance(Duration.ofSeconds(1));
        assertEquals(IngestResult.Status.STORED_OUT_OF_ORDER, f.ingest.ingest(f.reading("H1", "b1", 99, 40.0)).status());
        assertEquals("b2", f.store.read(r -> r.readings().latest("H1")).orElseThrow().bootId());
    }

    @Test void outOfOrderHotSamplesDoNotRunRules() {
        assign("H1", f.worker1);
        f.feed("H1", 10, 25.0);
        f.feed("H1", 4, 40.0);
        f.feed("H1", 3, 40.0);
        f.feed("H1", 2, 40.0);
        assertTrue(incidents().isEmpty());
    }

    @Test void unknownDeviceAndSourceMismatchAreRejected() {
        f.clock.advance(Duration.ofSeconds(1));
        assertEquals(IngestResult.Status.UNKNOWN_DEVICE, f.ingest.ingest(f.reading("NOPE", "b1", 1, 25.0)).status());
        TelemetryReading live = new TelemetryReading("H1", "b1", 1, 1000, false, 25.0, 50.0, null);
        assertEquals(IngestResult.Status.SOURCE_MISMATCH, f.ingest.ingest(live).status());
        assertTrue(f.store.read(r -> r.readings().latest("H1")).isEmpty());
    }

    @Test void staleAtExactlyFiveSecondsOnlineBefore() {
        assign("H1", f.worker1);
        f.feed("H1", 1, 25.0);
        f.clock.advance(Duration.ofMillis(4999));
        assertEquals(HealthState.ONLINE, f.health.current().get("H1"));
        f.clock.advance(Duration.ofMillis(1));
        assertEquals(HealthState.STALE, f.health.current().get("H1"));
    }

    @Test void offlinePlaceholderAtThirtySecondsAndRecovery() {
        assign("H1", f.worker1);
        f.feed("H1", 1, 25.0);
        f.clock.advance(Duration.ofMillis(29999));
        assertEquals(HealthState.STALE, f.health.current().get("H1"));
        f.clock.advance(Duration.ofMillis(1));
        assertEquals(HealthState.OFFLINE, f.health.current().get("H1"));
        f.feed("H1", 2, 25.0);
        assertEquals(HealthState.ONLINE, f.health.current().get("H1"));
    }

    @Test void healthChangesAreRecordedOnTransitionsAndNeverCreateIncidents() {
        assign("H1", f.worker1);
        f.health.sweep();
        f.feed("H1", 1, 25.0);
        f.clock.advance(Duration.ofSeconds(6));
        f.health.sweep();
        f.health.sweep();
        f.clock.advance(Duration.ofSeconds(30));
        f.health.sweep();
        f.feed("H1", 2, 25.0);
        List<HealthState> states = f.health.history("H1", 20).stream().map(c -> c.state()).toList();
        assertEquals(List.of(HealthState.ONLINE, HealthState.OFFLINE, HealthState.STALE, HealthState.ONLINE, HealthState.NO_DATA), states);
        assertTrue(incidents().isEmpty());
    }

    @Test void noDataYetUntilFirstReadingAfterAssignment() {
        f.feed("H1", 1, 25.0);
        f.clock.advance(Duration.ofSeconds(1));
        assign("H1", f.worker1);
        assertEquals(HealthState.NO_DATA, f.health.current().get("H1"));
        assertEquals(HealthState.UNASSIGNED, f.health.current().get("H2"));
        f.feed("H1", 2, 25.0);
        assertEquals(HealthState.ONLINE, f.health.current().get("H1"));
    }

    @Test void sensorFaultShowsNullValuesNeverOldOnes() {
        assign("H1", f.worker1);
        f.feed("H1", 1, 27.4);
        var online = f.queries.workers(f.supervisorId, SiteQueryService.WorkerQuery.firstPage(f.siteId)).rows().get(0);
        assertEquals(27.4, online.airTempC());
        f.feed("H1", 2, null);
        var row = f.queries.workers(f.supervisorId, SiteQueryService.WorkerQuery.firstPage(f.siteId)).rows().get(0);
        assertEquals(HealthState.SENSOR_FAULT, row.health());
        assertNull(row.airTempC());
        assertNull(row.humidityPct());
        assertTrue(incidents().isEmpty(), "sensor fault is health, not an incident");
    }

    @Test void restartRecoveryKeepsIncidentsAndRejectsReplay() {
        assign("H1", f.worker1);
        DeviceEvent sos = new DeviceEvent("H1", "b1", 7, 7000, true, EventType.SOS);
        f.clock.advance(Duration.ofSeconds(1));
        f.ingest.ingest(sos);
        Fixture again = f.restart();
        assertEquals(1, again.store.read(r -> r.incidents().search(IncidentFilter.all())).size());
        assertEquals(IngestResult.Status.DUPLICATE, again.ingest.ingest(sos).status());
        assertEquals(1, again.store.read(r -> r.incidents().search(IncidentFilter.all())).size());
    }
}
