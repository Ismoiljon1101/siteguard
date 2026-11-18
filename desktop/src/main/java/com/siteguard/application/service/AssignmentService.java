package com.siteguard.application.service;

import com.siteguard.application.port.Repositories;
import com.siteguard.application.port.Store;
import com.siteguard.domain.Device;
import com.siteguard.domain.DeviceStatus;
import com.siteguard.domain.HelmetAssignment;
import com.siteguard.domain.Operator;
import com.siteguard.domain.Zone;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Helmet and worker assignment. Reassigning ends the earlier assignments of that helmet and that worker and
 * starts the new one in a single transaction. Old incidents keep their original assignment_id. Blocking.
 */
public final class AssignmentService {
    private final Store store;
    private final Clock clock;

    public AssignmentService(Store store, Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.clock = Objects.requireNonNull(clock);
    }

    /** zoneId may be null (no zone yet). The zone must belong to the helmet site. */
    public HelmetAssignment assign(long operatorId, String deviceId, long workerId, Long zoneId) {
        Instant now = clock.instant();
        return store.inTransaction(r -> {
            Device device = r.devices().find(deviceId).orElseThrow(() -> new NotFoundException("Unknown helmet"));
            if (device.status() != DeviceStatus.ACTIVE) throw new ValidationException("Helmet is retired");
            Operator op = Access.operatorForSite(r, operatorId, device.siteId());
            r.workers().find(workerId).orElseThrow(() -> new NotFoundException("Unknown worker"));
            Zone zone = zoneId == null ? null : checkedZone(r, zoneId, device.siteId());

            Set<String> touched = new LinkedHashSet<>();
            touched.add(deviceId);
            r.assignments().activeForDevice(deviceId).ifPresent(a -> r.assignments().end(a.assignmentId(), now));
            r.assignments().activeForWorker(workerId).ifPresent(a -> {
                touched.add(a.deviceId());
                r.assignments().end(a.assignmentId(), now);
            });
            HelmetAssignment created = r.assignments().start(deviceId, workerId, op.operatorId(), now);
            if (zone != null) r.assignments().setZone(created.assignmentId(), zone.zoneId(), op.operatorId(), now);
            refreshHealth(r, touched, now);
            return created;
        });
    }

    public void endAssignment(long operatorId, long assignmentId) {
        Instant now = clock.instant();
        store.inTransaction(r -> {
            HelmetAssignment a = r.assignments().find(assignmentId).orElseThrow(() -> new NotFoundException("Unknown assignment"));
            if (!a.active()) throw new InvalidTransitionException("Assignment already ended");
            Device device = r.devices().find(a.deviceId()).orElseThrow();
            Access.operatorForSite(r, operatorId, device.siteId());
            r.assignments().end(assignmentId, now);
            refreshHealth(r, Set.of(a.deviceId()), now);
            return null;
        });
    }

    /** Append a new zone row for an active assignment. The history of zones is kept. */
    public void setZone(long operatorId, long assignmentId, long zoneId) {
        Instant now = clock.instant();
        store.inTransaction(r -> {
            HelmetAssignment a = r.assignments().find(assignmentId).orElseThrow(() -> new NotFoundException("Unknown assignment"));
            if (!a.active()) throw new InvalidTransitionException("Assignment already ended");
            Device device = r.devices().find(a.deviceId()).orElseThrow();
            Operator op = Access.operatorForSite(r, operatorId, device.siteId());
            checkedZone(r, zoneId, device.siteId());
            r.assignments().setZone(assignmentId, zoneId, op.operatorId(), now);
            return null;
        });
    }

    /** All assignments of a worker, newest first. */
    public List<HelmetAssignment> history(long workerId) {
        return store.read(r -> r.assignments().historyForWorker(workerId));
    }

    private static Zone checkedZone(Repositories r, long zoneId, long siteId) {
        Zone zone = r.sites().findZone(zoneId).orElseThrow(() -> new NotFoundException("Unknown zone"));
        if (zone.siteId() != siteId) throw new ValidationException("Zone belongs to a different site");
        return zone;
    }

    private static void refreshHealth(Repositories r, Set<String> deviceIds, Instant now) {
        for (String id : deviceIds) r.devices().find(id).ifPresent(d -> HealthTracker.refresh(r, d, now));
    }
}
