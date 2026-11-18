package com.siteguard.adapter.sqlite;

import com.siteguard.application.port.StorageException;
import com.siteguard.application.port.Store;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** Unit of work over SQLite. One connection per call; transactions commit on return and roll back on any exception. */
public final class SqliteStore implements Store {
    private final SqliteDatabase database;

    public SqliteStore(SqliteDatabase database) { this.database = Objects.requireNonNull(database); }

    /** Apply all migrations, then return a store. */
    public static SqliteStore open(SqliteDatabase database) {
        Migrator.migrate(database);
        return new SqliteStore(database);
    }

    @Override public <T> T inTransaction(Work<T> work) {
        try (Connection c = database.open(true)) {
            c.setAutoCommit(false);
            try {
                T result = work.run(new JdbcRepositories(c));
                c.commit();
                return result;
            } catch (RuntimeException | Error e) {
                try { c.rollback(); } catch (SQLException suppressed) { e.addSuppressed(suppressed); }
                throw e;
            }
        } catch (SQLException e) {
            throw new StorageException("Database error", e);
        }
    }

    @Override public <T> T read(Work<T> work) {
        try (Connection c = database.open(true)) {
            return work.run(new JdbcRepositories(c));
        } catch (SQLException e) {
            throw new StorageException("Database error", e);
        }
    }
}
