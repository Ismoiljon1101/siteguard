package com.siteguard.bootstrap;

import com.siteguard.adapter.simulation.Scenario;
import com.siteguard.adapter.simulation.SimulationFrame;
import com.siteguard.adapter.simulation.SimulationSource;
import com.siteguard.adapter.sqlite.IncidentRecord;
import com.siteguard.adapter.sqlite.SqliteIncidentRepository;
import com.siteguard.domain.Acceleration;
import com.siteguard.domain.DeviceEvent;
import com.siteguard.domain.TelemetryReading;
import com.siteguard.ui.DashboardController;
import com.siteguard.ui.DashboardSnapshot;
import com.siteguard.ui.IncidentRow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Prototype coordinator: all simulation and storage work stays off Swing's event thread. */
public final class SimulationController implements DashboardController {
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "siteguard-simulation");
        thread.setDaemon(true);
        return thread;
    });
    private final SimulationSource source = new SimulationSource();
    private final String session = UUID.randomUUID().toString();
    private final Map<String, IncidentRow> incidents = new LinkedHashMap<>();
    private SqliteIncidentRepository repository;
    private Consumer<DashboardSnapshot> updates;
    private TelemetryReading reading;
    private String storageStatus = "Starting";
    private String lastUpdate = "Waiting";
    private int hotSamples;
    private boolean warningActive;

    @Override public void start(Consumer<DashboardSnapshot> updates) {
        if (updates == null) throw new IllegalArgumentException("updates required");
        this.updates = updates;
        worker.execute(() -> {
            try {
                Path directory = Path.of(System.getProperty("user.dir"), "data");
                Files.createDirectories(directory);
                repository = new SqliteIncidentRepository(directory.resolve("siteguard.db"));
                repository.initialize();
                for (IncidentRecord incident : repository.list()) {
                    String detail = repository.actions(incident.sourceIdentity()).stream()
                            .filter(action -> action.note() != null && !action.note().isBlank())
                            .reduce((first, last) -> last).map(action -> action.note()).orElse("");
                    incidents.put(incident.sourceIdentity(), new IncidentRow(incident.sourceIdentity(),
                            incident.type(), incident.severity(), incident.state(), detail, incident.occurredAt()));
                }
                storageStatus = "SQLite connected";
            } catch (Exception exception) {
                repository = null;
                storageStatus = "Memory only — SQLite unavailable (" + exception.getClass().getSimpleName() + "); history is temporary";
            }
            tickSafely();
            worker.scheduleAtFixedRate(this::tickSafely, 1, 1, TimeUnit.SECONDS);
        });
    }

    private void tickSafely() {
        try { tick(); }
        catch (RuntimeException exception) {
            storageStatus = "Simulation error: " + exception.getClass().getSimpleName();
            publish();
        }
    }

    private void tick() {
        SimulationFrame frame = source.next();
        reading = frame.reading();
        lastUpdate = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        for (DeviceEvent event : frame.events()) {
            String id = event.deviceId() + "/" + session + "-" + event.bootId() + "/" + event.sequence();
            addIncident(id, event.type().name(), event.type().name().equals("SOS") ? "HIGH" : "WARNING", event.deviceId());
        }
        // Provisional demonstration threshold; configure and validate real rules in A00/A05.
        if (reading.temperatureC() != null && reading.temperatureC() >= 35.0) {
            hotSamples++;
            if (hotSamples >= 3 && !warningActive) {
                String id = reading.deviceId() + "/" + session + "-" + reading.bootId() + "/" + reading.sequence();
                warningActive = addIncident(id, "ENVIRONMENT", "WARNING", reading.deviceId());
            }
        } else {
            hotSamples = 0;
            warningActive = false;
        }
        publish();
    }

    private boolean addIncident(String id, String type, String severity, String deviceId) {
        if (incidents.containsKey(id)) return true;
        String time = Instant.now().toString();
        if (repository != null) {
            try {
                if (!repository.save(new IncidentRecord(id, type, severity, deviceId, "OPEN", time, "SIMULATED"))) return true;
            } catch (SQLException exception) {
                storageStatus = "Storage error — incident not saved";
                return false;
            }
        }
        incidents.put(id, new IncidentRow(id, type, severity, "OPEN", "", time));
        return true;
    }

    @Override public void triggerSos() { submit(source::triggerSos); }
    @Override public void triggerMotion() { submit(source::triggerMotion); }
    @Override public void setScenario(Scenario scenario) { submit(() -> source.setScenario(scenario)); }
    @Override public void acknowledge(String id, String operator) { submit(() -> change(id, operator, null, "ACKNOWLEDGED")); }
    @Override public void addNote(String id, String operator, String note) { submit(() -> change(id, operator, note, "NOTE")); }
    @Override public void resolve(String id, String operator, String note) { submit(() -> change(id, operator, note, "RESOLVED")); }

    private void change(String id, String operator, String note, String action) {
        IncidentRow incident = incidents.get(id);
        if (incident == null || operator == null || operator.isBlank()
                || (action.equals("ACKNOWLEDGED") && !incident.state().equals("OPEN"))
                || (action.equals("RESOLVED") && !incident.state().equals("ACKNOWLEDGED"))
                || ((action.equals("NOTE") || action.equals("RESOLVED")) && (note == null || note.isBlank()))) {
            storageStatus = "Action rejected — check state, operator and note";
            publish();
            return;
        }
        if (repository != null) {
            try {
                boolean saved = switch (action) {
                    case "ACKNOWLEDGED" -> repository.acknowledge(id, operator);
                    case "RESOLVED" -> repository.resolve(id, operator, note);
                    default -> repository.addNote(id, operator, note);
                };
                if (!saved) { storageStatus = "Action rejected by storage"; publish(); return; }
                storageStatus = "SQLite connected";
            } catch (SQLException exception) {
                storageStatus = "Storage error — action not saved";
                publish(); return;
            }
        } else {
            storageStatus = "Memory only — history will not survive restart";
        }
        String state = action.equals("NOTE") ? incident.state() : action;
        String detail = note == null || note.isBlank() ? incident.detail() : note;
        incidents.put(id, new IncidentRow(id, incident.type(), incident.severity(), state, detail, incident.occurredAt()));
        publish();
    }

    private void publish() {
        if (updates == null) return;
        Acceleration acceleration = reading == null ? null : reading.acceleration();
        List<IncidentRow> rows = new ArrayList<>(incidents.values());
        java.util.Collections.reverse(rows);
        updates.accept(new DashboardSnapshot(reading == null ? "sim-helmet-001" : reading.deviceId(),
                reading == null ? "STARTING" : reading.temperatureC() == null ? "SIMULATED / SENSOR FAULT" : "SIMULATED / ACTIVE",
                reading == null ? null : reading.temperatureC(), reading == null ? null : reading.humidityPct(),
                acceleration == null ? null : acceleration.x(), acceleration == null ? null : acceleration.y(),
                acceleration == null ? null : acceleration.z(), lastUpdate, storageStatus, rows));
    }

    private void submit(Runnable command) {
        try { worker.execute(() -> {
            try { command.run(); }
            catch (RuntimeException exception) {
                storageStatus = "Action error: " + exception.getClass().getSimpleName();
                publish();
            }
        }); }
        catch (RejectedExecutionException ignored) { /* Window is already closed. */ }
    }

    @Override public void close() { worker.shutdownNow(); }
}
