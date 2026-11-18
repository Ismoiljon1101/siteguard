package com.siteguard.adapter.sqlite;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** JDBC storage. Call these blocking methods from a worker, never Swing's event thread. */
public final class SqliteIncidentRepository {
    private final String url;
    private final SqliteDatabase database;

    public SqliteIncidentRepository(Path database) {
        url = "jdbc:sqlite:" + Objects.requireNonNull(database).toAbsolutePath();
        this.database = new SqliteDatabase(database);
    }

    private Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
        return connection;
    }

    /** Apply all numbered migrations (see Migrations). Rejects databases created by a newer version. */
    public void initialize() throws SQLException {
        try {
            Migrator.migrate(database);
        } catch (com.siteguard.application.port.StorageException e) {
            if (e.getCause() instanceof SQLException sql) throw sql;
            throw new SQLException(e.getMessage(), e);
        }
    }

    /** Insert once per source identity. Returns false when the identity already exists. */
    public boolean save(IncidentRecord incident) throws SQLException {
        Objects.requireNonNull(incident);
        try (Connection connection = open()) {
            Migrations.ensureLegacyDevice(connection, incident.deviceId());
            return insertIncident(connection, incident);
        }
    }

    private boolean insertIncident(Connection connection, IncidentRecord incident) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO incidents(source_identity,type,severity,device_id,state,occurred_at,source_mode) VALUES(?,?,?,?,?,?,?) ON CONFLICT(source_identity) DO NOTHING")) {
            statement.setString(1, incident.sourceIdentity());
            statement.setString(2, incident.type());
            statement.setString(3, incident.severity());
            statement.setString(4, incident.deviceId());
            statement.setString(5, incident.state());
            statement.setString(6, incident.occurredAt());
            statement.setString(7, incident.sourceMode());
            return statement.executeUpdate() == 1;
        }
    }

    public List<IncidentRecord> list() throws SQLException {
        List<IncidentRecord> incidents = new ArrayList<>();
        try (Connection connection = open(); Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT source_identity,type,severity,device_id,state,occurred_at,source_mode FROM incidents ORDER BY occurred_at DESC, source_identity")) {
            while (rows.next()) incidents.add(new IncidentRecord(rows.getString(1), rows.getString(2),
                    rows.getString(3), rows.getString(4), rows.getString(5), rows.getString(6), rows.getString(7)));
        }
        return List.copyOf(incidents);
    }

    /** Update state and append its history atomically. Returns false for a missing identity. */
    public boolean updateState(String sourceIdentity, String state) throws SQLException {
        if (sourceIdentity == null || sourceIdentity.isBlank() || state == null || state.isBlank())
            throw new IllegalArgumentException("Identity and state must not be blank");
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                int changed;
                try (PreparedStatement update = connection.prepareStatement("UPDATE incidents SET state = ? WHERE source_identity = ?")) {
                    update.setString(1, state);
                    update.setString(2, sourceIdentity);
                    changed = update.executeUpdate();
                }
                if (changed != 0) {
                    try (PreparedStatement action = connection.prepareStatement("INSERT INTO incident_actions(source_identity,state) VALUES(?,?)")) {
                        action.setString(1, sourceIdentity);
                        action.setString(2, state);
                        action.executeUpdate();
                    }
                }
                connection.commit();
                return changed != 0;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public boolean acknowledge(String identity, String operator) throws SQLException {
        return appendAction(identity, operator, null, "ACKNOWLEDGED", "OPEN");
    }

    public boolean resolve(String identity, String operator) throws SQLException {
        return appendAction(identity, operator, null, "RESOLVED", "ACKNOWLEDGED");
    }

    public boolean resolve(String identity, String operator, String note) throws SQLException {
        if (note == null || note.isBlank()) throw new IllegalArgumentException("Note must not be blank");
        return appendAction(identity, operator, note, "RESOLVED", "ACKNOWLEDGED");
    }

    public boolean addNote(String identity, String operator, String note) throws SQLException {
        if (note == null || note.isBlank()) throw new IllegalArgumentException("Note must not be blank");
        return appendAction(identity, operator, note, "NOTE", null);
    }

    private boolean appendAction(String identity, String operator, String note, String action, String requiredState)
            throws SQLException {
        if (identity == null || identity.isBlank() || operator == null || operator.isBlank())
            throw new IllegalArgumentException("Identity and operator must not be blank");
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                int changed;
                // A conditional write locks the row before the history entry, and prevents races.
                String sql = requiredState == null
                        ? "UPDATE incidents SET state = state WHERE source_identity = ?"
                        : "UPDATE incidents SET state = ? WHERE source_identity = ? AND state = ?";
                try (PreparedStatement update = connection.prepareStatement(sql)) {
                    if (requiredState == null) update.setString(1, identity);
                    else {
                        update.setString(1, action);
                        update.setString(2, identity);
                        update.setString(3, requiredState);
                    }
                    changed = update.executeUpdate();
                }
                if (changed != 0) {
                    try (PreparedStatement insert = connection.prepareStatement(
                            "INSERT INTO incident_actions(source_identity,state,operator,note) VALUES(?,?,?,?)")) {
                        insert.setString(1, identity);
                        insert.setString(2, action);
                        insert.setString(3, operator);
                        insert.setString(4, note);
                        insert.executeUpdate();
                    }
                }
                connection.commit();
                return changed != 0;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public List<IncidentAction> actions(String identity) throws SQLException {
        if (identity == null || identity.isBlank()) throw new IllegalArgumentException("Identity must not be blank");
        List<IncidentAction> actions = new ArrayList<>();
        try (Connection connection = open(); PreparedStatement query = connection.prepareStatement(
                "SELECT source_identity,state,operator,note,recorded_at FROM incident_actions WHERE source_identity = ? ORDER BY id")) {
            query.setString(1, identity);
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) actions.add(new IncidentAction(rows.getString(1), rows.getString(2),
                        rows.getString(3), rows.getString(4), rows.getString(5)));
            }
        }
        return List.copyOf(actions);
    }

}
