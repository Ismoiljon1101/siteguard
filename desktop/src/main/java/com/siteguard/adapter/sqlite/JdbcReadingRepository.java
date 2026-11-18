package com.siteguard.adapter.sqlite;

import com.siteguard.application.port.ReadingRepository;
import com.siteguard.domain.Acceleration;
import com.siteguard.domain.SourceEvent;
import com.siteguard.domain.SourceMode;
import com.siteguard.domain.StoredReading;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

final class JdbcReadingRepository implements ReadingRepository {
    private final Connection c;

    JdbcReadingRepository(Connection c) { this.c = c; }

    private static final String COLS = "r.reading_id, r.device_id, r.boot_id, r.seq, r.uptime_ms, r.received_at, "
            + "r.temperature_c, r.humidity_pct, r.accel_x_mps2, r.accel_y_mps2, r.accel_z_mps2, r.source_mode";

    private static final Jdbc.Mapper<StoredReading> READING = rs -> {
        Double x = Jdbc.doubleOrNull(rs, 9);
        Acceleration accel = x == null ? null : new Acceleration(x, rs.getDouble(10), rs.getDouble(11));
        return new StoredReading(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getLong(4), rs.getLong(5),
                Jdbc.instant(rs.getString(6)), Jdbc.doubleOrNull(rs, 7), Jdbc.doubleOrNull(rs, 8), accel,
                SourceMode.valueOf(rs.getString(12)));
    };

    @Override public boolean readingExists(String deviceId, String bootId, long seq) {
        return Jdbc.one(c, "SELECT 1 FROM readings WHERE device_id = ? AND boot_id = ? AND seq = ?",
                rs -> 1, deviceId, bootId, seq).isPresent();
    }

    @Override public boolean eventExists(String eventId) {
        return Jdbc.one(c, "SELECT 1 FROM source_events WHERE event_id = ?", rs -> 1, eventId).isPresent();
    }

    @Override public void touchSession(String deviceId, String bootId, Instant now) {
        Jdbc.update(c, "INSERT INTO sessions(device_id, boot_id, first_seen_at, last_seen_at) VALUES(?,?,?,?) "
                + "ON CONFLICT(device_id, boot_id) DO UPDATE SET last_seen_at = MAX(last_seen_at, excluded.last_seen_at)",
                deviceId, bootId, now, now);
    }

    @Override public boolean isCurrentBoot(String deviceId, String bootId) {
        return Jdbc.one(c, "SELECT boot_id FROM sessions WHERE device_id = ? ORDER BY first_seen_at DESC, rowid DESC LIMIT 1",
                rs -> rs.getString(1), deviceId).map(bootId::equals).orElse(false);
    }

    @Override public OptionalLong maxSeq(String deviceId, String bootId) {
        Long max = Jdbc.one(c, "SELECT MAX(seq) FROM readings WHERE device_id = ? AND boot_id = ?",
                rs -> Jdbc.longOrNull(rs, 1), deviceId, bootId).orElse(null);
        return max == null ? OptionalLong.empty() : OptionalLong.of(max);
    }

    @Override public Optional<Instant> lastStoredAt(String deviceId) {
        return Jdbc.one(c, "SELECT MAX(received_at) FROM readings WHERE device_id = ?",
                rs -> Jdbc.instant(rs.getString(1)), deviceId).filter(java.util.Objects::nonNull);
    }

    @Override public Optional<Instant> lastSeen(String deviceId) {
        return Jdbc.one(c, "SELECT MAX(last_seen_at) FROM sessions WHERE device_id = ?",
                rs -> Jdbc.instant(rs.getString(1)), deviceId).filter(java.util.Objects::nonNull);
    }

    @Override public StoredReading insert(StoredReading r) {
        Acceleration a = r.acceleration();
        long id = Jdbc.insert(c, "INSERT INTO readings(device_id, boot_id, seq, uptime_ms, received_at, temperature_c, humidity_pct, "
                + "accel_x_mps2, accel_y_mps2, accel_z_mps2, environment_status, motion_status, source_mode) "
                + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                r.deviceId(), r.bootId(), r.seq(), r.uptimeMs(), r.receivedAt(), r.temperatureC(), r.humidityPct(),
                a == null ? null : a.x(), a == null ? null : a.y(), a == null ? null : a.z(),
                r.environmentFault() ? "fault" : "ok", r.motionFault() ? "fault" : "ok", r.sourceMode());
        return new StoredReading(id, r.deviceId(), r.bootId(), r.seq(), r.uptimeMs(), r.receivedAt(),
                r.temperatureC(), r.humidityPct(), a, r.sourceMode());
    }

    @Override public boolean insertEvent(SourceEvent e) {
        return Jdbc.update(c, "INSERT INTO source_events(event_id, device_id, boot_id, seq, event_type, uptime_ms, received_at, committed_at, source_mode) "
                + "VALUES(?,?,?,?,?,?,?,?,?) ON CONFLICT(event_id) DO NOTHING",
                e.eventId(), e.deviceId(), e.bootId(), e.seq(), e.type(), e.uptimeMs(), e.receivedAt(), e.committedAt(), e.sourceMode()) == 1;
    }

    @Override public List<StoredReading> previous(String deviceId, String bootId, long beforeSeq, int limit) {
        return Jdbc.list(c, "SELECT " + COLS + " FROM readings r WHERE r.device_id = ? AND r.boot_id = ? AND r.seq < ? "
                + "ORDER BY r.seq DESC LIMIT ?", READING, deviceId, bootId, beforeSeq, limit);
    }

    @Override public Optional<StoredReading> latest(String deviceId) {
        return Jdbc.one(c, "SELECT " + COLS + " FROM readings r JOIN sessions s ON s.device_id = r.device_id AND s.boot_id = r.boot_id "
                + "WHERE r.device_id = ? ORDER BY s.first_seen_at DESC, s.rowid DESC, r.seq DESC LIMIT 1", READING, deviceId);
    }

    @Override public List<StoredReading> range(String deviceId, Instant from, Instant to) {
        return Jdbc.list(c, "SELECT " + COLS + " FROM readings r WHERE r.device_id = ? AND r.received_at >= ? AND r.received_at <= ? "
                + "ORDER BY r.received_at, r.reading_id", READING, deviceId, from, to);
    }
}
