package com.siteguard.application.service;

import com.siteguard.application.port.ReadingRepository;
import com.siteguard.application.port.Repositories;
import com.siteguard.application.port.Store;
import com.siteguard.domain.ConfigVersion;
import com.siteguard.domain.Device;
import com.siteguard.domain.DeviceEvent;
import com.siteguard.domain.DeviceStatus;
import com.siteguard.domain.EnvironmentRule;
import com.siteguard.domain.EscalationLevel;
import com.siteguard.domain.HelmetAssignment;
import com.siteguard.domain.Incident;
import com.siteguard.domain.IncidentState;
import com.siteguard.domain.IncidentType;
import com.siteguard.domain.SourceEvent;
import com.siteguard.domain.StoredReading;
import com.siteguard.domain.TelemetryReading;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * The single processing path for readings and events from any source (simulation now, serial or MQTT later).
 * Validate (done by the domain records), store at most 1 Hz, update health, run rules. Blocking.
 * Each call is one transaction: an event and its incident commit together or not at all.
 */
public final class IngestService {
    private final Store store;
    private final Clock clock;

    public IngestService(Store store, Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.clock = Objects.requireNonNull(clock);
    }

    public IngestResult ingest(TelemetryReading reading) {
        Objects.requireNonNull(reading, "reading");
        Instant now = clock.instant();
        return store.inTransaction(r -> ingestReading(r, reading, now));
    }

    public IngestResult ingest(DeviceEvent event) {
        Objects.requireNonNull(event, "event");
        Instant now = clock.instant();
        return store.inTransaction(r -> ingestEvent(r, event, now));
    }

    private static Optional<IngestResult> rejectDevice(Repositories r, String deviceId, boolean simulated, Device[] out) {
        Optional<Device> found = r.devices().find(deviceId);
        if (found.isEmpty() || found.get().status() != DeviceStatus.ACTIVE) {
            return Optional.of(new IngestResult(IngestResult.Status.UNKNOWN_DEVICE, null));
        }
        if (found.get().simulated() != simulated) {
            return Optional.of(new IngestResult(IngestResult.Status.SOURCE_MISMATCH, null));
        }
        out[0] = found.get();
        return Optional.empty();
    }

    private IngestResult ingestReading(Repositories r, TelemetryReading t, Instant now) {
        Device[] holder = new Device[1];
        Optional<IngestResult> rejected = rejectDevice(r, t.deviceId(), t.simulated(), holder);
        if (rejected.isPresent()) return rejected.get();
        Device device = holder[0];
        ReadingRepository readings = r.readings();
        if (readings.readingExists(t.deviceId(), t.bootId(), t.sequence())) {
            return new IngestResult(IngestResult.Status.DUPLICATE, null);
        }
        readings.touchSession(t.deviceId(), t.bootId(), now);

        Optional<Instant> lastStored = readings.lastStoredAt(t.deviceId());
        if (lastStored.isPresent() && lastStored.get().getEpochSecond() == now.getEpochSecond()) {
            HealthTracker.refresh(r, device, now);
            return new IngestResult(IngestResult.Status.RATE_LIMITED, null);
        }

        boolean current = readings.isCurrentBoot(t.deviceId(), t.bootId());
        OptionalLong maxSeq = readings.maxSeq(t.deviceId(), t.bootId());
        boolean inOrder = current && (maxSeq.isEmpty() || t.sequence() > maxSeq.getAsLong());

        StoredReading stored = readings.insert(new StoredReading(0, t.deviceId(), t.bootId(), t.sequence(), t.uptimeMs(), now,
                t.temperatureC(), t.humidityPct(), t.acceleration(), device.sourceMode()));

        String opened = null;
        if (inOrder && !stored.environmentFault()) {
            opened = evaluateEnvironment(r, device, stored, now);
        }
        HealthTracker.refresh(r, device, now);
        return new IngestResult(inOrder ? IngestResult.Status.STORED : IngestResult.Status.STORED_OUT_OF_ORDER, opened);
    }

    /** Environment rule: limit reached for N consecutive stored samples; one incident while an earlier one is unresolved. */
    private String evaluateEnvironment(Repositories r, Device device, StoredReading stored, Instant now) {
        ConfigVersion config = r.config().current();
        List<Double> temps = new ArrayList<>();
        temps.add(stored.temperatureC());
        for (StoredReading p : r.readings().previous(device.deviceId(), stored.bootId(), stored.seq(),
                config.tempConsecutiveSamples() - 1)) {
            temps.add(p.temperatureC());
        }
        if (!EnvironmentRule.violated(config, temps)) return null;
        if (r.incidents().hasUnresolved(device.deviceId(), IncidentType.ENVIRONMENT)) return null;
        String identity = device.deviceId() + "/" + stored.bootId() + "/ENVIRONMENT/" + stored.seq();
        Long assignmentId = r.assignments().activeForDevice(device.deviceId()).map(HelmetAssignment::assignmentId).orElse(null);
        Incident incident = new Incident(identity, IncidentType.ENVIRONMENT, IncidentType.ENVIRONMENT.severity(),
                device.deviceId(), IncidentState.OPEN, now, device.sourceMode().name(), assignmentId,
                config.configVersion(), stored.readingId(), EscalationLevel.NONE);
        return IncidentOps.open(r, incident, now) ? identity : null;
    }

    private IngestResult ingestEvent(Repositories r, DeviceEvent e, Instant now) {
        Device[] holder = new Device[1];
        Optional<IngestResult> rejected = rejectDevice(r, e.deviceId(), e.simulated(), holder);
        if (rejected.isPresent()) return rejected.get();
        Device device = holder[0];
        String identity = SourceEvent.identity(e.deviceId(), e.bootId(), e.sequence());
        if (r.readings().eventExists(identity)) {
            return new IngestResult(IngestResult.Status.DUPLICATE, r.incidents().find(identity).isPresent() ? identity : null);
        }
        r.readings().touchSession(e.deviceId(), e.bootId(), now);
        r.readings().insertEvent(new SourceEvent(identity, e.deviceId(), e.bootId(), e.sequence(), e.type(), e.uptimeMs(),
                now, now, device.sourceMode()));
        IncidentType type = IncidentType.fromEvent(e.type());
        Long assignmentId = r.assignments().activeForDevice(device.deviceId()).map(HelmetAssignment::assignmentId).orElse(null);
        // MOTION: the config threshold is NULL until measured (D10), so every motion event is treated as an event.
        Incident incident = new Incident(identity, type, type.severity(), device.deviceId(), IncidentState.OPEN, now,
                device.sourceMode().name(), assignmentId, null, null, EscalationLevel.NONE);
        IncidentOps.open(r, incident, now);
        HealthTracker.refresh(r, device, now);
        return new IngestResult(IngestResult.Status.STORED, identity);
    }
}
