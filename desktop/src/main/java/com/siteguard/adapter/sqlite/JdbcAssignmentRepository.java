package com.siteguard.adapter.sqlite;

import com.siteguard.application.port.ActiveAssignment;
import com.siteguard.application.port.AssignmentRepository;
import com.siteguard.domain.AssignmentZone;
import com.siteguard.domain.HelmetAssignment;
import com.siteguard.domain.Worker;
import com.siteguard.domain.Zone;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

final class JdbcAssignmentRepository implements AssignmentRepository {
    private final Connection c;

    JdbcAssignmentRepository(Connection c) { this.c = c; }

    private static final String COLS = "assignment_id, device_id, worker_id, assigned_by, starts_at, ends_at";
    private static final Jdbc.Mapper<HelmetAssignment> ASSIGNMENT = rs -> map(rs, 1);

    static HelmetAssignment map(java.sql.ResultSet rs, int o) throws java.sql.SQLException {
        return new HelmetAssignment(rs.getLong(o), rs.getString(o + 1), rs.getLong(o + 2), Jdbc.longOrNull(rs, o + 3),
                Jdbc.instant(rs.getString(o + 4)), Jdbc.instant(rs.getString(o + 5)));
    }

    @Override public HelmetAssignment start(String deviceId, long workerId, Long assignedBy, Instant at) {
        long id = Jdbc.insert(c, "INSERT INTO helmet_assignments(device_id, worker_id, assigned_by, starts_at) VALUES(?,?,?,?)",
                deviceId, workerId, assignedBy, at);
        return new HelmetAssignment(id, deviceId, workerId, assignedBy, at, null);
    }

    @Override public void end(long assignmentId, Instant at) {
        Jdbc.update(c, "UPDATE helmet_assignments SET ends_at = ? WHERE assignment_id = ? AND ends_at IS NULL", at, assignmentId);
    }

    @Override public Optional<HelmetAssignment> find(long assignmentId) {
        return Jdbc.one(c, "SELECT " + COLS + " FROM helmet_assignments WHERE assignment_id = ?", ASSIGNMENT, assignmentId);
    }

    @Override public Optional<HelmetAssignment> activeForDevice(String deviceId) {
        return Jdbc.one(c, "SELECT " + COLS + " FROM helmet_assignments WHERE device_id = ? AND ends_at IS NULL", ASSIGNMENT, deviceId);
    }

    @Override public Optional<HelmetAssignment> activeForWorker(long workerId) {
        return Jdbc.one(c, "SELECT " + COLS + " FROM helmet_assignments WHERE worker_id = ? AND ends_at IS NULL", ASSIGNMENT, workerId);
    }

    @Override public AssignmentZone setZone(long assignmentId, long zoneId, Long setBy, Instant at) {
        long id = Jdbc.insert(c, "INSERT INTO assignment_zones(assignment_id, zone_id, set_by, set_at) VALUES(?,?,?,?)",
                assignmentId, zoneId, setBy, at);
        return new AssignmentZone(id, assignmentId, zoneId, setBy, at);
    }

    @Override public Optional<AssignmentZone> currentZone(long assignmentId) {
        return Jdbc.one(c, "SELECT assignment_zone_id, assignment_id, zone_id, set_by, set_at FROM assignment_zones "
                + "WHERE assignment_id = ? ORDER BY assignment_zone_id DESC LIMIT 1",
                rs -> new AssignmentZone(rs.getLong(1), rs.getLong(2), rs.getLong(3), Jdbc.longOrNull(rs, 4), Jdbc.instant(rs.getString(5))),
                assignmentId);
    }

    @Override public List<HelmetAssignment> historyForWorker(long workerId) {
        return Jdbc.list(c, "SELECT " + COLS + " FROM helmet_assignments WHERE worker_id = ? ORDER BY assignment_id DESC", ASSIGNMENT, workerId);
    }

    @Override public List<ActiveAssignment> activeForSite(long siteId) {
        String sql = "SELECT a.assignment_id, a.device_id, a.worker_id, a.assigned_by, a.starts_at, a.ends_at, "
                + "w.worker_id, w.display_name, w.trade, w.created_at, "
                + "d.device_id, d.site_id, d.display_name, d.is_simulated, d.status, d.created_at, "
                + "z.zone_id, z.site_id, z.name, z.sort_order, az.set_at, o.display_name "
                + "FROM helmet_assignments a JOIN workers w ON w.worker_id = a.worker_id JOIN devices d ON d.device_id = a.device_id "
                + "LEFT JOIN assignment_zones az ON az.assignment_zone_id = "
                + "(SELECT MAX(assignment_zone_id) FROM assignment_zones WHERE assignment_id = a.assignment_id) "
                + "LEFT JOIN zones z ON z.zone_id = az.zone_id LEFT JOIN operators o ON o.operator_id = az.set_by "
                + "WHERE a.ends_at IS NULL AND d.site_id = ? ORDER BY w.display_name, a.assignment_id";
        return Jdbc.list(c, sql, rs -> {
            HelmetAssignment a = map(rs, 1);
            Worker w = new Worker(rs.getLong(7), rs.getString(8), rs.getString(9), Jdbc.instant(rs.getString(10)));
            var d = JdbcDeviceRepository.map(rs, 11);
            Long zoneId = Jdbc.longOrNull(rs, 17);
            Zone zone = zoneId == null ? null : new Zone(zoneId, rs.getLong(18), rs.getString(19), rs.getInt(20));
            return new ActiveAssignment(a, w, d, zone, Jdbc.instant(rs.getString(21)), rs.getString(22));
        }, siteId);
    }
}
