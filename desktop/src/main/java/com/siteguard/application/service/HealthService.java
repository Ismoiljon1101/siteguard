package com.siteguard.application.service;

import com.siteguard.application.port.Store;
import com.siteguard.domain.Device;
import com.siteguard.domain.HealthChange;
import com.siteguard.domain.HealthState;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Time-driven health. Silence cannot trigger ingest, so call {@link #sweep()} about once a second to record
 * STALE and OFFLINE transitions. Health never creates or resolves an incident. Blocking.
 */
public final class HealthService {
    private final Store store;
    private final Clock clock;

    public HealthService(Store store, Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.clock = Objects.requireNonNull(clock);
    }

    /** Evaluate every active device and record changes. Returns how many changed. */
    public int sweep() {
        Instant now = clock.instant();
        return store.inTransaction(r -> {
            int changed = 0;
            for (Device d : r.devices().listActive()) {
                Optional<HealthChange> before = r.health().latest(d.deviceId());
                HealthState after = HealthTracker.refresh(r, d, now);
                if (before.isEmpty() || before.get().state() != after) changed++;
            }
            return changed;
        });
    }

    /** Current health of every active device, computed now (not read from the change log). */
    public Map<String, HealthState> current() {
        Instant now = clock.instant();
        return store.read(r -> {
            Map<String, HealthState> out = new LinkedHashMap<>();
            for (Device d : r.devices().listActive()) out.put(d.deviceId(), HealthTracker.evaluate(r, d, now));
            return out;
        });
    }

    /** Recorded transitions for one device, newest first. */
    public List<HealthChange> history(String deviceId, int limit) {
        return store.read(r -> r.health().history(deviceId, limit));
    }
}
