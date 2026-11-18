package com.siteguard.adapter.sqlite;

import com.siteguard.application.port.HealthRepository;
import com.siteguard.domain.HealthChange;
import com.siteguard.domain.HealthState;
import com.siteguard.domain.SourceMode;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

final class JdbcHealthRepository implements HealthRepository {
    private final Connection c;

    JdbcHealthRepository(Connection c) { this.c = c; }

    private static final Jdbc.Mapper<HealthChange> CHANGE = rs -> new HealthChange(rs.getLong(1), rs.getString(2),
            HealthState.valueOf(rs.getString(3)), Jdbc.instant(rs.getString(4)), SourceMode.valueOf(rs.getString(5)));
    private static final String COLS = "health_change_id, device_id, state, changed_at, source_mode";

    @Override public void record(HealthChange h) {
        Jdbc.update(c, "INSERT INTO health_changes(device_id, state, changed_at, source_mode) VALUES(?,?,?,?)",
                h.deviceId(), h.state(), h.changedAt(), h.sourceMode());
    }

    @Override public Optional<HealthChange> latest(String deviceId) {
        return Jdbc.one(c, "SELECT " + COLS + " FROM health_changes WHERE device_id = ? ORDER BY health_change_id DESC LIMIT 1", CHANGE, deviceId);
    }

    @Override public List<HealthChange> history(String deviceId, int limit) {
        return Jdbc.list(c, "SELECT " + COLS + " FROM health_changes WHERE device_id = ? ORDER BY health_change_id DESC LIMIT ?", CHANGE, deviceId, limit);
    }
}
