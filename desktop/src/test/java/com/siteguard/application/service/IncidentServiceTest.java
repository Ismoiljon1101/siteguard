package com.siteguard.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.siteguard.application.port.IncidentFilter;
import com.siteguard.domain.ActionType;
import com.siteguard.domain.DeviceEvent;
import com.siteguard.domain.EscalationLevel;
import com.siteguard.domain.EventType;
import com.siteguard.domain.IncidentAction;
import com.siteguard.domain.IncidentState;
import com.siteguard.domain.ResolutionCategory;
import com.siteguard.testsupport.Fixture;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IncidentServiceTest {
    @TempDir Path dir;
    Fixture f;
    String id;

    @BeforeEach void setUp() {
        f = Fixture.create(dir);
        f.assignments.assign(f.supervisorId, "H1", f.worker1, f.zone1);
        f.clock.advance(Duration.ofSeconds(1));
        id = f.ingest.ingest(new DeviceEvent("H1", "b1", 1, 1000, true, EventType.SOS)).incidentIdentity();
    }

    private IncidentState state() { return f.store.read(r -> r.incidents().find(id)).orElseThrow().state(); }
    private List<IncidentAction> actions() { return f.store.read(r -> r.incidents().actions(id)); }

    @Test void fullWorkflowAppendsEveryStepWithOperator() {
        f.incidents.acknowledge(id, f.supervisorId);
        f.incidents.addNote(id, f.managerId, "Sent a colleague");
        f.incidents.resolve(id, f.supervisorId, ResolutionCategory.ASSISTANCE_GIVEN, "Worker helped");
        assertEquals(IncidentState.RESOLVED, state());
        List<IncidentAction> a = actions();
        assertEquals(List.of(ActionType.OPENED, ActionType.ACKNOWLEDGED, ActionType.NOTE, ActionType.RESOLVED),
                a.stream().map(IncidentAction::action).toList());
        assertEquals("Test Supervisor", a.get(1).operatorName());
        assertEquals(f.supervisorId, a.get(1).operatorId());
        assertEquals("Test Manager", a.get(2).operatorName());
        assertEquals(ResolutionCategory.ASSISTANCE_GIVEN, a.get(3).category());
        assertEquals("Worker helped", a.get(3).note());
    }

    @Test void resolveWithoutCategoryOrNoteIsRejectedAndChangesNothing() {
        f.incidents.acknowledge(id, f.supervisorId);
        assertThrows(ValidationException.class, () -> f.incidents.resolve(id, f.supervisorId, null, "note"));
        assertThrows(ValidationException.class, () -> f.incidents.resolve(id, f.supervisorId, ResolutionCategory.TEST, " "));
        assertThrows(ValidationException.class, () -> f.incidents.resolve(id, f.supervisorId, ResolutionCategory.TEST, null));
        assertEquals(IncidentState.ACKNOWLEDGED, state());
        assertEquals(2, actions().size());
    }

    @Test void cannotSkipAcknowledgementOrReopenOrAcknowledgeTwice() {
        assertThrows(InvalidTransitionException.class, () -> f.incidents.resolve(id, f.supervisorId, ResolutionCategory.TEST, "n"));
        assertEquals(IncidentState.OPEN, state());
        f.incidents.acknowledge(id, f.supervisorId);
        assertThrows(InvalidTransitionException.class, () -> f.incidents.acknowledge(id, f.supervisorId));
        f.incidents.resolve(id, f.supervisorId, ResolutionCategory.FALSE_ALARM, "mistake");
        assertThrows(InvalidTransitionException.class, () -> f.incidents.acknowledge(id, f.supervisorId));
        assertThrows(InvalidTransitionException.class, () -> f.incidents.resolve(id, f.supervisorId, ResolutionCategory.TEST, "again"));
        assertEquals(IncidentState.RESOLVED, state());
        assertEquals(3, actions().size());
    }

    @Test void blankNoteRejected() {
        assertThrows(ValidationException.class, () -> f.incidents.addNote(id, f.supervisorId, "  "));
    }

    @Test void unknownIncidentAndUnknownOperator() {
        assertThrows(NotFoundException.class, () -> f.incidents.acknowledge("nope", f.supervisorId));
        assertThrows(ValidationException.class, () -> f.incidents.acknowledge(id, 9999));
    }

    @Test void operatorWithoutSiteAccessIsRejectedDirectorAllowed() {
        assertThrows(AuthorizationException.class, () -> f.incidents.acknowledge(id, f.outsiderId));
        assertEquals(IncidentState.OPEN, state());
        f.incidents.acknowledge(id, f.directorId);
        assertEquals(IncidentState.ACKNOWLEDGED, state());
    }

    @Test void listingScopedBySite() {
        assertEquals(1, f.incidents.list(f.supervisorId, IncidentFilter.openForSite(f.siteId)).size());
        assertThrows(AuthorizationException.class, () -> f.incidents.list(f.outsiderId, IncidentFilter.openForSite(f.siteId)));
        assertEquals(0, f.incidents.list(f.outsiderId, IncidentFilter.openForSite(f.otherSiteId)).size());
        assertThrows(ValidationException.class, () -> f.incidents.list(f.supervisorId, IncidentFilter.all()));
        assertEquals(1, f.incidents.list(f.directorId, IncidentFilter.all()).size());
    }

    @Test void detailIncludesPlaybackAndUnacknowledgedAge() {
        f.clock.advance(Duration.ofSeconds(10));
        var detail = f.incidents.detail(f.supervisorId, id).orElseThrow();
        assertEquals(Duration.ofSeconds(10), detail.unacknowledgedFor());
        assertEquals("W1", detail.view().workerName());
        assertEquals("Z1", detail.view().zoneName());
        f.incidents.acknowledge(id, f.supervisorId);
        assertNull(f.incidents.detail(f.supervisorId, id).orElseThrow().unacknowledgedFor());
    }

    @Test void escalationBoundariesAt30And120Seconds() {
        assertEquals(0, f.incidents.escalateOverdue());
        f.clock.advance(Duration.ofMillis(29999));
        assertEquals(0, f.incidents.escalateOverdue());
        f.clock.advance(Duration.ofMillis(1));
        assertEquals(1, f.incidents.escalateOverdue());
        assertEquals(EscalationLevel.LEVEL_1, f.store.read(r -> r.incidents().find(id)).orElseThrow().escalation());
        assertEquals(0, f.incidents.escalateOverdue());
        f.clock.advance(Duration.ofSeconds(90));
        assertEquals(1, f.incidents.escalateOverdue());
        assertEquals(EscalationLevel.LEVEL_2, f.store.read(r -> r.incidents().find(id)).orElseThrow().escalation());
        assertEquals(IncidentState.OPEN, state(), "escalation never changes state");
    }

    @Test void acknowledgedSosDoesNotEscalate() {
        f.incidents.acknowledge(id, f.supervisorId);
        f.clock.advance(Duration.ofSeconds(500));
        assertEquals(0, f.incidents.escalateOverdue());
    }
}
