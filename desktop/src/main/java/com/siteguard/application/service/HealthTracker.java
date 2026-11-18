package com.siteguard.application.service;

import com.siteguard.application.port.Repositories;
import com.siteguard.domain.Device;
import com.siteguard.domain.HealthChange;
import com.siteguard.domain.HealthPolicy;
import com.siteguard.domain.HealthState;
import com.siteguard.domain.HelmetAssignment;
import com.siteguard.domain.StoredReading;
import java.time.Instant;
import java.util.Optional;

/** Evaluates device health and records a HEALTH_CHANGE row on transitions only. Never touches incidents. */
final class HealthTracker {
    private HealthTracker() { }

    static HealthState evaluate(Repositories r, Device device, Instant now) {
        Optional<HelmetAssignment> assignment = r.assignments().activeForDevice(device.deviceId());
        Optional<StoredReading> latest = r.readings().latest(device.deviceId());
        Instant lastSeen = r.readings().lastSeen(device.deviceId()).orElse(null);
        boolean fault = latest.map(x -> x.environmentFault() || x.motionFault()).orElse(false);
        return HealthPolicy.evaluate(assignment.isPresent(), assignment.map(HelmetAssignment::startsAt).orElse(null),
                lastSeen, fault, now);
    }

    static HealthState refresh(Repositories r, Device device, Instant now) {
        HealthState state = evaluate(r, device, now);
        Optional<HealthChange> last = r.health().latest(device.deviceId());
        if (last.isEmpty() || last.get().state() != state) {
            r.health().record(new HealthChange(0, device.deviceId(), state, now, device.sourceMode()));
        }
        return state;
    }
}
