package com.siteguard.adapter.sqlite;

import com.siteguard.application.port.StorageException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** Applies numbered migrations in order. Each runs atomically; a failure leaves the previous version intact. */
public final class Migrator {
    private Migrator() { }

    public static int migrate(SqliteDatabase database) {
        return migrate(database, Migrations.all());
    }

    public static int migrate(SqliteDatabase database, List<Migration> migrations) {
        try {
            try (Connection c = database.open(true); Statement s = c.createStatement()) {
                s.execute("PRAGMA journal_mode = WAL");
            }
            int current = version(database);
            int latest = migrations.stream().mapToInt(Migration::version).max().orElse(0);
            if (current > latest) throw new StorageException("Unsupported database version: " + current);
            for (Migration migration : migrations) {
                if (migration.version() > current) {
                    apply(database, migration);
                    current = migration.version();
                }
            }
            return current;
        } catch (SQLException e) {
            throw new StorageException("Migration failed", e);
        }
    }

    public static int version(SqliteDatabase database) throws SQLException {
        try (Connection c = database.open(true); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("PRAGMA user_version")) {
            return r.next() ? r.getInt(1) : 0;
        }
    }

    private static void apply(SqliteDatabase database, Migration migration) throws SQLException {
        try (Connection c = database.open(false)) {
            c.setAutoCommit(false);
            try (Statement s = c.createStatement()) {
                migration.apply(c);
                s.execute("PRAGMA user_version = " + migration.version());
                try (ResultSet violations = s.executeQuery("PRAGMA foreign_key_check")) {
                    if (violations.next()) {
                        throw new SQLException("Foreign key violation after migration " + migration.version()
                                + " in table " + violations.getString(1));
                    }
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
