package com.siteguard.adapter.sqlite;

import com.siteguard.application.port.ConfigRepository;
import com.siteguard.domain.ConfigVersion;
import java.sql.Connection;
import java.time.Instant;

final class JdbcConfigRepository implements ConfigRepository {
    private final Connection c;

    JdbcConfigRepository(Connection c) { this.c = c; }

    @Override public ConfigVersion current() {
        return Jdbc.one(c, "SELECT config_version, temp_warn_c, temp_consecutive_samples, motion_threshold_mps2, created_by, created_at "
                + "FROM config_versions ORDER BY config_version DESC LIMIT 1",
                rs -> new ConfigVersion(rs.getLong(1), rs.getDouble(2), rs.getInt(3), Jdbc.doubleOrNull(rs, 4),
                        Jdbc.longOrNull(rs, 5), Jdbc.instant(rs.getString(6)))).orElseThrow(
                () -> new com.siteguard.application.port.StorageException("No config version row"));
    }

    @Override public ConfigVersion create(double tempWarnC, int samples, Double motionThreshold, Long createdBy, Instant at) {
        long id = Jdbc.insert(c, "INSERT INTO config_versions(temp_warn_c, temp_consecutive_samples, motion_threshold_mps2, created_by, created_at) "
                + "VALUES(?,?,?,?,?)", tempWarnC, samples, motionThreshold, createdBy, at);
        return new ConfigVersion(id, tempWarnC, samples, motionThreshold, createdBy, at);
    }
}
