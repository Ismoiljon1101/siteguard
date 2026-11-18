package com.siteguard.adapter.sqlite;

import com.siteguard.application.port.IncidentFilter;
import com.siteguard.application.port.IncidentRepository;
import com.siteguard.application.port.IncidentView;
import com.siteguard.domain.ActionType;
import com.siteguard.domain.EscalationLevel;
import com.siteguard.domain.Incident;
import com.siteguard.domain.IncidentAction;
import com.siteguard.domain.IncidentState;
import com.siteguard.domain.IncidentType;
import com.siteguard.domain.ResolutionCategory;
import com.siteguard.domain.Severity;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class JdbcIncidentRepository implements IncidentRepository {
    private final Connection c;

    JdbcIncidentRepository(Connection c) { this.c = c; }

    private static final String INCIDENT_COLS = "i.source_identity, i.type, i.severity, i.device_id, i.state, i.occurred_at, "
            + "i.source_mode, i.assignment_id, i.config_version, i.trigger_reading_id, i.escalation_level";

    /** Zone in force when the incident occurred: the latest zone row set at or before that time. */
    private static final String ZONE_ID_AT_INCIDENT = "(SELECT az.zone_id FROM assignment_zones az WHERE az.assignment_id = i.assignment_id "
            + "AND az.set_at <= i.occurred_at ORDER BY az.assignment_zone_id DESC LIMIT 1)";

    private static Incident incident(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Incident(rs.getString(1), IncidentType.valueOf(rs.getString(2)), Severity.valueOf(rs.getString(3)),
                rs.getString(4), IncidentState.valueOf(rs.getString(5)), Jdbc.instant(rs.getString(6)), rs.getString(7),
                Jdbc.longOrNull(rs, 8), Jdbc.longOrNull(rs, 9), Jdbc.longOrNull(rs, 10), EscalationLevel.fromLevel(rs.getInt(11)));
    }

    private static final String VIEW_SQL = "SELECT " + INCIDENT_COLS + ", d.site_id, d.display_name, w.worker_id, w.display_name, "
            + "(SELECT z.name FROM zones z WHERE z.zone_id = " + ZONE_ID_AT_INCIDENT + ") "
            + "FROM incidents i JOIN devices d ON d.device_id = i.device_id "
            + "LEFT JOIN helmet_assignments a ON a.assignment_id = i.assignment_id LEFT JOIN workers w ON w.worker_id = a.worker_id ";

    private static final Jdbc.Mapper<IncidentView> VIEW = rs -> new IncidentView(incident(rs), rs.getLong(12), rs.getString(13),
            Jdbc.longOrNull(rs, 14), rs.getString(15), rs.getString(16));

    @Override public boolean insertIfAbsent(Incident i) {
        return Jdbc.update(c, "INSERT INTO incidents(source_identity, type, severity, device_id, state, occurred_at, source_mode, "
                + "assignment_id, config_version, trigger_reading_id, escalation_level) VALUES(?,?,?,?,?,?,?,?,?,?,?) "
                + "ON CONFLICT(source_identity) DO NOTHING",
                i.sourceIdentity(), i.type(), i.severity(), i.deviceId(), i.state(), i.occurredAt(), i.sourceMode(),
                i.assignmentId(), i.configVersion(), i.triggerReadingId(), i.escalation().level()) == 1;
    }

    @Override public Optional<Incident> find(String id) {
        return Jdbc.one(c, "SELECT " + INCIDENT_COLS + " FROM incidents i WHERE i.source_identity = ?", JdbcIncidentRepository::incident, id);
    }

    @Override public Optional<IncidentView> view(String id) {
        return Jdbc.one(c, VIEW_SQL + "WHERE i.source_identity = ?", VIEW, id);
    }

    @Override public boolean hasUnresolved(String deviceId, IncidentType type) {
        return Jdbc.one(c, "SELECT 1 FROM incidents WHERE device_id = ? AND type = ? AND state <> 'RESOLVED' LIMIT 1",
                rs -> 1, deviceId, type).isPresent();
    }

    @Override public boolean transition(String id, IncidentState from, IncidentState to) {
        return Jdbc.update(c, "UPDATE incidents SET state = ? WHERE source_identity = ? AND state = ?", to, id, from) == 1;
    }

    @Override public long appendAction(IncidentAction a) {
        return Jdbc.insert(c, "INSERT INTO incident_actions(source_identity, state, operator, operator_id, note, resolution_category, recorded_at) "
                + "VALUES(?,?,?,?,?,?,?)",
                a.sourceIdentity(), a.action(), a.operatorName(), a.operatorId(), a.note(), a.category(), a.recordedAt());
    }

    @Override public List<IncidentAction> actions(String id) {
        return Jdbc.list(c, "SELECT id, source_identity, state, operator, operator_id, note, resolution_category, recorded_at "
                + "FROM incident_actions WHERE source_identity = ? ORDER BY id", rs -> {
            ActionType type;
            try { type = ActionType.valueOf(rs.getString(3)); } catch (IllegalArgumentException e) { type = ActionType.NOTE; }
            String cat = rs.getString(7);
            return new IncidentAction(rs.getLong(1), rs.getString(2), type, rs.getString(4), Jdbc.longOrNull(rs, 5), rs.getString(6),
                    cat == null ? null : ResolutionCategory.valueOf(cat), Jdbc.instant(rs.getString(8)));
        }, id);
    }

    @Override public List<IncidentView> search(IncidentFilter f) {
        StringBuilder sql = new StringBuilder(VIEW_SQL).append("WHERE 1 = 1 ");
        List<Object> args = new ArrayList<>();
        if (f.siteId() != null) { sql.append("AND d.site_id = ? "); args.add(f.siteId()); }
        if (!f.states().isEmpty()) {
            sql.append("AND i.state IN (");
            boolean first = true;
            for (IncidentState s : f.states()) { sql.append(first ? "?" : ",?"); first = false; args.add(s); }
            sql.append(") ");
        }
        if (f.type() != null) { sql.append("AND i.type = ? "); args.add(f.type()); }
        if (f.zoneId() != null) { sql.append("AND ").append(ZONE_ID_AT_INCIDENT).append(" = ? "); args.add(f.zoneId()); }
        if (f.workerId() != null) { sql.append("AND a.worker_id = ? "); args.add(f.workerId()); }
        if (f.deviceId() != null) { sql.append("AND i.device_id = ? "); args.add(f.deviceId()); }
        if (f.from() != null) { sql.append("AND i.occurred_at >= ? "); args.add(f.from()); }
        if (f.to() != null) { sql.append("AND i.occurred_at < ? "); args.add(f.to()); }
        sql.append("ORDER BY i.occurred_at DESC, i.source_identity LIMIT ? OFFSET ?");
        args.add(f.limit());
        args.add(f.offset());
        return Jdbc.list(c, sql.toString(), VIEW, args.toArray());
    }

    @Override public List<Incident> openSos() {
        return Jdbc.list(c, "SELECT " + INCIDENT_COLS + " FROM incidents i WHERE i.type = 'SOS' AND i.state = 'OPEN' ORDER BY i.occurred_at",
                JdbcIncidentRepository::incident);
    }

    @Override public void setEscalation(String id, EscalationLevel level) {
        Jdbc.update(c, "UPDATE incidents SET escalation_level = ? WHERE source_identity = ?", level.level(), id);
    }
}
