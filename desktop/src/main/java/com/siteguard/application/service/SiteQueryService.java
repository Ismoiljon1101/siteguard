package com.siteguard.application.service;

import com.siteguard.application.port.ActiveAssignment;
import com.siteguard.application.port.IncidentFilter;
import com.siteguard.application.port.IncidentView;
import com.siteguard.application.port.Repositories;
import com.siteguard.application.port.Store;
import com.siteguard.domain.Device;
import com.siteguard.domain.HealthState;
import com.siteguard.domain.HelmetAssignment;
import com.siteguard.domain.Incident;
import com.siteguard.domain.IncidentState;
import com.siteguard.domain.IncidentType;
import com.siteguard.domain.Operator;
import com.siteguard.domain.Site;
import com.siteguard.domain.StoredReading;
import com.siteguard.domain.Worker;
import com.siteguard.domain.Zone;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Read side for the dashboard. All blocking. Health is computed at call time from stored data and the clock,
 * so STALE and OFFLINE appear even when no sweep has run. Filtering, sorting and paging happen in memory over one
 * site (sized for hundreds of workers, not measured beyond the tests).
 */
public final class SiteQueryService {
    private final Store store;
    private final Clock clock;

    public SiteQueryService(Store store, Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.clock = Objects.requireNonNull(clock);
    }

    public enum WorkerSort { NEEDS_ATTENTION, NAME }

    /** zoneId filters to one zone; noZoneOnly filters to workers with no zone. Page is 0-based. */
    public record WorkerQuery(long siteId, Long zoneId, boolean noZoneOnly, String trade, String search,
                              Set<HealthState> health, WorkerSort sort, int page, int pageSize) {
        public WorkerQuery {
            health = health == null ? Set.of() : Set.copyOf(health);
            if (sort == null) sort = WorkerSort.NEEDS_ATTENTION;
            if (page < 0) page = 0;
            if (pageSize <= 0) pageSize = 25;
        }
        public static WorkerQuery firstPage(long siteId) {
            return new WorkerQuery(siteId, null, false, null, null, null, WorkerSort.NEEDS_ATTENTION, 0, 25);
        }
    }

    /** airTempC and humidityPct are null unless the helmet is ONLINE (never old values). Temperature is air near the helmet. */
    public record WorkerRow(long assignmentId, long workerId, String workerName, String trade, String deviceId,
                            String deviceLabel, boolean simulated, Long zoneId, String zoneName, Instant zoneSetAt,
                            String zoneSetBy, HealthState health, Double airTempC, Double humidityPct,
                            Duration updatedAge, String incidentIdentity, IncidentType incidentType,
                            IncidentState incidentState) { }

    public record WorkerPage(List<WorkerRow> rows, int total, int page, int pageSize) { }

    /** zoneId null means the "No zone" bucket. */
    public record ZoneCount(Long zoneId, String name, int workers) { }

    /**
     * statusCounts holds ONLINE, STALE, OFFLINE, SENSOR_FAULT and NO_DATA and sums to workersOnShift.
     * Zone counts also sum to workersOnShift. Unassigned helmets are counted separately.
     */
    public record SiteOverview(long siteId, String siteName, int workersOnShift, Map<HealthState, Integer> statusCounts,
                               int unassignedHelmets, List<ZoneCount> zones, int openSos, int openWarnings,
                               Duration oldestUnacknowledged, int liveHelmets, int simulatedHelmets) { }

    public record HelmetRow(Device device, String workerName, HealthState health, Instant lastSeen) { }

    public record AssignmentHistoryRow(HelmetAssignment assignment, String deviceLabel, String zoneName) { }

    public record WorkerDetail(Worker worker, WorkerRow current, List<AssignmentHistoryRow> history,
                               List<IncidentView> incidents, List<StoredReading> lastTenMinutes) { }

    // ---- reference data -------------------------------------------------------------------------------------

    /** Sites this operator may see. */
    public List<Site> sites(long operatorId) {
        return store.read(r -> {
            Operator op = Access.operator(r, operatorId);
            List<Site> all = r.sites().listSites();
            if (op.role().seesAllSites()) return all;
            Set<Long> allowed = r.operators().siteIds(operatorId);
            return all.stream().filter(s -> allowed.contains(s.siteId())).toList();
        });
    }

    public List<Zone> zones(long siteId) { return store.read(r -> r.sites().zones(siteId)); }

    /** Operator list for the login screen. No credential data. */
    public List<Operator> operators() { return store.read(r -> r.operators().list()); }

    public List<Worker> workers() { return store.read(r -> r.workers().list()); }

    public List<HelmetRow> helmets(long operatorId, long siteId) {
        Instant now = clock.instant();
        return store.read(r -> {
            Access.operatorForSite(r, operatorId, siteId);
            List<HelmetRow> out = new ArrayList<>();
            for (Device d : r.devices().list(siteId)) {
                Optional<HelmetAssignment> a = r.assignments().activeForDevice(d.deviceId());
                String worker = a.flatMap(x -> r.workers().find(x.workerId())).map(Worker::displayName).orElse(null);
                out.add(new HelmetRow(d, worker, HealthTracker.evaluate(r, d, now), r.readings().lastSeen(d.deviceId()).orElse(null)));
            }
            return out;
        });
    }

    // ---- overview and worker table ---------------------------------------------------------------------------

    public SiteOverview overview(long operatorId, long siteId) {
        Instant now = clock.instant();
        return store.read(r -> {
            Access.operatorForSite(r, operatorId, siteId);
            Site site = r.sites().findSite(siteId).orElseThrow(() -> new NotFoundException("Unknown site"));
            List<WorkerRow> rows = buildRows(r, siteId, now);
            Map<HealthState, Integer> counts = new EnumMap<>(HealthState.class);
            for (HealthState s : new HealthState[] {HealthState.ONLINE, HealthState.STALE, HealthState.OFFLINE,
                    HealthState.SENSOR_FAULT, HealthState.NO_DATA}) counts.put(s, 0);
            Map<Long, Integer> byZone = new HashMap<>();
            int noZone = 0;
            for (WorkerRow row : rows) {
                counts.merge(row.health(), 1, Integer::sum);
                if (row.zoneId() == null) noZone++; else byZone.merge(row.zoneId(), 1, Integer::sum);
            }
            List<ZoneCount> zones = new ArrayList<>();
            for (Zone z : r.sites().zones(siteId)) zones.add(new ZoneCount(z.zoneId(), z.name(), byZone.getOrDefault(z.zoneId(), 0)));
            if (noZone > 0) zones.add(new ZoneCount(null, "No zone", noZone));

            int unassigned = 0, live = 0, sim = 0;
            for (Device d : r.devices().list(siteId)) {
                if (d.status() != com.siteguard.domain.DeviceStatus.ACTIVE) continue;
                if (r.assignments().activeForDevice(d.deviceId()).isEmpty()) unassigned++;
                if (d.simulated()) sim++; else live++;
            }
            int openSos = 0, openWarnings = 0;
            Instant oldest = null;
            for (IncidentView v : r.incidents().search(IncidentFilter.openForSite(siteId))) {
                Incident i = v.incident();
                if (i.type() == IncidentType.SOS) openSos++; else openWarnings++;
                if (i.state() == IncidentState.OPEN && (oldest == null || i.occurredAt().isBefore(oldest))) oldest = i.occurredAt();
            }
            return new SiteOverview(siteId, site.name(), rows.size(), counts, unassigned, zones, openSos, openWarnings,
                    oldest == null ? null : Duration.between(oldest, now), live, sim);
        });
    }

    public WorkerPage workers(long operatorId, WorkerQuery q) {
        Instant now = clock.instant();
        return store.read(r -> {
            Access.operatorForSite(r, operatorId, q.siteId());
            List<WorkerRow> rows = new ArrayList<>(buildRows(r, q.siteId(), now));
            String needle = q.search() == null ? "" : q.search().strip().toLowerCase(Locale.ROOT);
            rows.removeIf(row -> (q.zoneId() != null && !q.zoneId().equals(row.zoneId()))
                    || (q.noZoneOnly() && row.zoneId() != null)
                    || (q.trade() != null && !q.trade().isBlank() && !q.trade().equalsIgnoreCase(row.trade()))
                    || (!q.health().isEmpty() && !q.health().contains(row.health()))
                    || (!needle.isEmpty() && !(row.workerName().toLowerCase(Locale.ROOT).contains(needle)
                            || row.deviceLabel().toLowerCase(Locale.ROOT).contains(needle)
                            || row.deviceId().toLowerCase(Locale.ROOT).contains(needle))));
            Comparator<WorkerRow> byName = Comparator.comparing(WorkerRow::workerName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparingLong(WorkerRow::workerId);
            rows.sort(q.sort() == WorkerSort.NAME ? byName
                    : Comparator.comparingInt(SiteQueryService::attentionRank).thenComparing(byName));
            int from = Math.min(q.page() * q.pageSize(), rows.size());
            int to = Math.min(from + q.pageSize(), rows.size());
            return new WorkerPage(List.copyOf(rows.subList(from, to)), rows.size(), q.page(), q.pageSize());
        });
    }

    public Optional<WorkerDetail> workerDetail(long operatorId, long workerId) {
        Instant now = clock.instant();
        return store.read(r -> {
            Optional<Worker> worker = r.workers().find(workerId);
            if (worker.isEmpty()) return Optional.empty();
            List<HelmetAssignment> history = r.assignments().historyForWorker(workerId);
            if (!history.isEmpty()) {
                Device d = r.devices().find(history.get(0).deviceId()).orElseThrow();
                Access.operatorForSite(r, operatorId, d.siteId());
            }
            WorkerRow current = null;
            List<StoredReading> recent = List.of();
            Optional<HelmetAssignment> active = r.assignments().activeForWorker(workerId);
            if (active.isPresent()) {
                Device d = r.devices().find(active.get().deviceId()).orElseThrow();
                for (WorkerRow row : buildRows(r, d.siteId(), now)) if (row.workerId() == workerId) current = row;
                recent = r.readings().range(d.deviceId(), now.minus(Duration.ofMinutes(10)), now);
            }
            List<AssignmentHistoryRow> rows = new ArrayList<>();
            for (HelmetAssignment a : history) {
                String label = r.devices().find(a.deviceId()).map(Device::displayName).orElse(a.deviceId());
                String zone = r.assignments().currentZone(a.assignmentId())
                        .flatMap(z -> r.sites().findZone(z.zoneId())).map(Zone::name).orElse(null);
                rows.add(new AssignmentHistoryRow(a, label, zone));
            }
            IncidentFilter filter = new IncidentFilter(null, null, null, null, workerId, null, null, null, 200, 0);
            return Optional.of(new WorkerDetail(worker.get(), current, rows, r.incidents().search(filter), recent));
        });
    }

    // ---- internals --------------------------------------------------------------------------------------------

    /** needs-attention order: unresolved SOS, then OFFLINE/STALE/SENSOR_FAULT, then unresolved warnings, then the rest. */
    static int attentionRank(WorkerRow row) {
        boolean sos = row.incidentType() == IncidentType.SOS;
        if (sos) return 0;
        HealthState h = row.health();
        if (h == HealthState.OFFLINE || h == HealthState.STALE || h == HealthState.SENSOR_FAULT) return 1;
        if (row.incidentIdentity() != null) return 2;
        return 3;
    }

    private static List<WorkerRow> buildRows(Repositories r, long siteId, Instant now) {
        // Incidents belong to the assignment active when they were recorded, so a reassigned helmet does not
        // show its previous worker's incident on the new worker's row.
        Map<Long, IncidentView> byAssignment = new HashMap<>();
        for (IncidentView v : r.incidents().search(IncidentFilter.openForSite(siteId))) {
            Long assignmentId = v.incident().assignmentId();
            if (assignmentId == null) continue;
            IncidentView existing = byAssignment.get(assignmentId);
            // SOS outranks warnings for the one incident shown on the row.
            if (existing == null || (v.incident().type() == IncidentType.SOS && existing.incident().type() != IncidentType.SOS)) {
                byAssignment.put(assignmentId, v);
            }
        }
        List<WorkerRow> rows = new ArrayList<>();
        for (ActiveAssignment a : r.assignments().activeForSite(siteId)) {
            Device d = a.device();
            HealthState health = HealthTracker.evaluate(r, d, now);
            Optional<Instant> lastSeen = r.readings().lastSeen(d.deviceId());
            Double temp = null, hum = null;
            if (health == HealthState.ONLINE) {
                Optional<StoredReading> latest = r.readings().latest(d.deviceId());
                if (latest.isPresent()) { temp = latest.get().temperatureC(); hum = latest.get().humidityPct(); }
            }
            Duration age = lastSeen.filter(t -> !t.isBefore(a.assignment().startsAt()))
                    .map(t -> Duration.between(t, now)).orElse(null);
            IncidentView inc = byAssignment.get(a.assignment().assignmentId());
            rows.add(new WorkerRow(a.assignment().assignmentId(), a.worker().workerId(), a.worker().displayName(),
                    a.worker().trade(), d.deviceId(), d.displayName(), d.simulated(),
                    a.zone() == null ? null : a.zone().zoneId(), a.zone() == null ? null : a.zone().name(),
                    a.zoneSetAt(), a.zoneSetBy(), health, temp, hum, age,
                    inc == null ? null : inc.incident().sourceIdentity(), inc == null ? null : inc.incident().type(),
                    inc == null ? null : inc.incident().state()));
        }
        return rows;
    }
}
