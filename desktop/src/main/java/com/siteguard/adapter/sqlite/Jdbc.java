package com.siteguard.adapter.sqlite;

import com.siteguard.application.port.StorageException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Small JDBC helpers. Timestamps are UTC text with millisecond precision, so text order is time order. */
final class Jdbc {
    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private Jdbc() { }

    @FunctionalInterface
    interface Mapper<T> { T map(ResultSet rs) throws SQLException; }

    static String ts(Instant instant) { return instant == null ? null : TS.format(instant); }

    /** Lenient parse: ISO instants, and the SQLite CURRENT_TIMESTAMP form used by the prototype. */
    static Instant instant(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.indexOf('T') < 0 && t.indexOf(' ') > 0) t = t.replace(' ', 'T') + "Z";
        return Instant.parse(t);
    }

    static Long longOrNull(ResultSet rs, int index) throws SQLException {
        long v = rs.getLong(index);
        return rs.wasNull() ? null : v;
    }

    static Double doubleOrNull(ResultSet rs, int index) throws SQLException {
        double v = rs.getDouble(index);
        return rs.wasNull() ? null : v;
    }

    private static void bind(PreparedStatement statement, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            Object a = args[i];
            int index = i + 1;
            if (a == null) statement.setNull(index, Types.NULL);
            else if (a instanceof Instant instant) statement.setString(index, ts(instant));
            else if (a instanceof Enum<?> e) statement.setString(index, e.name());
            else if (a instanceof Boolean b) statement.setInt(index, b ? 1 : 0);
            else statement.setObject(index, a);
        }
    }

    static <T> List<T> list(Connection c, String sql, Mapper<T> mapper, Object... args) {
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            bind(statement, args);
            try (ResultSet rows = statement.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rows.next()) out.add(mapper.map(rows));
                return out;
            }
        } catch (SQLException e) {
            throw new StorageException("Database query failed", e);
        }
    }

    static <T> Optional<T> one(Connection c, String sql, Mapper<T> mapper, Object... args) {
        List<T> rows = list(c, sql, mapper, args);
        return rows.isEmpty() ? Optional.empty() : Optional.ofNullable(rows.get(0));
    }

    static int update(Connection c, String sql, Object... args) {
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            bind(statement, args);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Database update failed", e);
        }
    }

    /** Insert and return the generated rowid. */
    static long insert(Connection c, String sql, Object... args) {
        update(c, sql, args);
        try (Statement statement = c.createStatement(); ResultSet rows = statement.executeQuery("SELECT last_insert_rowid()")) {
            rows.next();
            return rows.getLong(1);
        } catch (SQLException e) {
            throw new StorageException("Database query failed", e);
        }
    }
}
