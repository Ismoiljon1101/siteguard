package com.siteguard.adapter.sqlite;

import com.siteguard.application.port.DeviceRepository;
import com.siteguard.domain.Device;
import com.siteguard.domain.DeviceStatus;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

final class JdbcDeviceRepository implements DeviceRepository {
    private final Connection c;

    JdbcDeviceRepository(Connection c) { this.c = c; }

    static final String COLS = "device_id, site_id, display_name, is_simulated, status, created_at";
    static Device map(java.sql.ResultSet rs, int o) throws java.sql.SQLException {
        return new Device(rs.getString(o), rs.getLong(o + 1), rs.getString(o + 2), rs.getInt(o + 3) == 1,
                DeviceStatus.valueOf(rs.getString(o + 4)), Jdbc.instant(rs.getString(o + 5)));
    }
    private static final Jdbc.Mapper<Device> DEVICE = rs -> map(rs, 1);

    @Override public Device register(String deviceId, long siteId, String displayName, boolean simulated, Instant at) {
        Jdbc.update(c, "INSERT INTO devices(" + COLS + ") VALUES(?,?,?,?,?,?)",
                deviceId, siteId, displayName, simulated, DeviceStatus.ACTIVE, at);
        return new Device(deviceId, siteId, displayName, simulated, DeviceStatus.ACTIVE, at);
    }

    @Override public Optional<Device> find(String deviceId) {
        return Jdbc.one(c, "SELECT " + COLS + " FROM devices WHERE device_id = ?", DEVICE, deviceId);
    }

    @Override public List<Device> list(long siteId) {
        return Jdbc.list(c, "SELECT " + COLS + " FROM devices WHERE site_id = ? ORDER BY device_id", DEVICE, siteId);
    }

    @Override public List<Device> listActive() {
        return Jdbc.list(c, "SELECT " + COLS + " FROM devices WHERE status = 'ACTIVE' ORDER BY device_id", DEVICE);
    }

    @Override public void retire(String deviceId) {
        Jdbc.update(c, "UPDATE devices SET status = 'RETIRED' WHERE device_id = ?", deviceId);
    }
}
