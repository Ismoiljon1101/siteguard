package com.siteguard.adapter.sqlite;

import static org.junit.jupiter.api.Assertions.*;

import com.siteguard.application.port.StorageException;
import com.siteguard.domain.ActionType;
import com.siteguard.domain.EscalationLevel;
import com.siteguard.domain.Incident;
import com.siteguard.domain.IncidentAction;
import com.siteguard.domain.IncidentState;
import com.siteguard.domain.IncidentType;
import com.siteguard.domain.ResolutionCategory;
import com.siteguard.domain.Role;
import com.siteguard.domain.Severity;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigrationTest {
    @TempDir Path dir;
    static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    /** Build a file exactly as the v2 prototype left it. */
    private Path prototypeV2() throws Exception {
        Path file = dir.resolve("proto.db");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath()); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE incidents (source_identity TEXT PRIMARY KEY, type TEXT NOT NULL, severity TEXT NOT NULL, device_id TEXT NOT NULL, state TEXT NOT NULL, occurred_at TEXT NOT NULL, source_mode TEXT NOT NULL)");
            s.executeUpdate("CREATE TABLE incident_actions (id INTEGER PRIMARY KEY, source_identity TEXT NOT NULL REFERENCES incidents(source_identity), state TEXT NOT NULL, recorded_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            s.executeUpdate("ALTER TABLE incident_actions ADD COLUMN operator TEXT NOT NULL DEFAULT ''");
            s.executeUpdate("ALTER TABLE incident_actions ADD COLUMN note TEXT");
            s.executeUpdate("INSERT INTO incidents VALUES('sim-helmet-001/sim-session-1/3','SOS','HIGH','sim-helmet-001','ACKNOWLEDGED','2026-01-01T00:00:00Z','SIMULATED')");
            s.executeUpdate("INSERT INTO incident_actions(source_identity,state,operator,note) VALUES('sim-helmet-001/sim-session-1/3','ACKNOWLEDGED','Alice','seen')");
            s.execute("PRAGMA user_version = 2");
        }
        return file;
    }

    private List<String> columns(Connection c, String table) throws SQLException {
        List<String> out = new ArrayList<>();
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (r.next()) out.add(r.getString("name"));
        }
        return out;
    }

    @Test void migratesAV2FileAndKeepsItsData() throws Exception {
        Path file = prototypeV2();
        SqliteDatabase db = new SqliteDatabase(file);
        assertEquals(4, Migrator.migrate(db));
        assertEquals(4, Migrator.version(db));
        try (Connection c = db.open(true); Statement s = c.createStatement()) {
            assertTrue(columns(c, "incidents").containsAll(List.of("source_identity", "type", "severity", "device_id", "state",
                    "occurred_at", "source_mode", "assignment_id", "config_version", "trigger_reading_id", "escalation_level")));
            assertTrue(columns(c, "incident_actions").containsAll(List.of("operator", "note", "operator_id", "resolution_category")));
            assertTrue(columns(c, "operators").containsAll(List.of("password_hash", "salt", "iterations", "created_at", "failed_attempts", "locked_until")));
            try (ResultSet r = s.executeQuery("SELECT state, escalation_level, device_id FROM incidents")) {
                assertTrue(r.next());
                assertEquals("ACKNOWLEDGED", r.getString(1));
                assertEquals(0, r.getInt(2));
                assertEquals("sim-helmet-001", r.getString(3));
            }
            try (ResultSet r = s.executeQuery("SELECT operator, note FROM incident_actions")) {
                assertTrue(r.next());
                assertEquals("Alice", r.getString(1));
                assertEquals("seen", r.getString(2));
            }
            try (ResultSet r = s.executeQuery("SELECT COUNT(*) FROM devices WHERE device_id = 'sim-helmet-001' AND is_simulated = 1")) {
                r.next();
                assertEquals(1, r.getInt(1), "unregistered prototype helmet adopted under the legacy site");
            }
            try (ResultSet r = s.executeQuery("PRAGMA foreign_key_check")) {
                assertFalse(r.next(), "no foreign key violations after migration");
            }
            try (ResultSet r = s.executeQuery("SELECT temp_warn_c, temp_consecutive_samples, motion_threshold_mps2 FROM config_versions WHERE config_version = 1")) {
                assertTrue(r.next());
                assertEquals(35.0, r.getDouble(1));
                assertEquals(3, r.getInt(2));
                r.getDouble(3);
                assertTrue(r.wasNull(), "motion threshold stays NULL until measured");
            }
        }
        // The action table still accepts new-style rows afterwards and the old repository still works.
        var legacy = new SqliteIncidentRepository(file);
        legacy.initialize();
        assertEquals(1, legacy.list().size());
        assertTrue(legacy.addNote("sim-helmet-001/sim-session-1/3", "Bob", "later note"));
    }

    @Test void migrationIsIdempotentAndFreshDatabaseReachesV4() throws Exception {
        SqliteDatabase db = new SqliteDatabase(dir.resolve("fresh.db"));
        assertEquals(4, Migrator.migrate(db));
        assertEquals(4, Migrator.migrate(db));
        assertEquals(4, Migrator.version(db));
    }

    @Test void failedMigrationRollsBackCompletely() throws Exception {
        SqliteDatabase db = new SqliteDatabase(dir.resolve("rollback.db"));
        Migrator.migrate(db);
        List<Migration> withBroken = new ArrayList<>(Migrations.all());
        withBroken.add(new Migration() {
            public int version() { return 5; }
            public String description() { return "broken"; }
            public void apply(Connection c) throws SQLException {
                try (Statement s = c.createStatement()) {
                    s.executeUpdate("CREATE TABLE half_done (x INTEGER)");
                    s.executeUpdate("ALTER TABLE no_such_table ADD COLUMN y TEXT");
                }
            }
        });
        assertThrows(StorageException.class, () -> Migrator.migrate(db, withBroken));
        assertEquals(4, Migrator.version(db), "version unchanged");
        try (Connection c = db.open(true); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE name = 'half_done'")) {
            r.next();
            assertEquals(0, r.getInt(1), "partial DDL rolled back");
        }
    }

    @Test void databaseFromANewerVersionIsRejected() throws Exception {
        Path file = dir.resolve("future.db");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath()); Statement s = c.createStatement()) {
            s.execute("PRAGMA user_version = 99");
        }
        assertThrows(StorageException.class, () -> Migrator.migrate(new SqliteDatabase(file)));
    }

    private SqliteStore seeded(SqliteDatabase db) {
        SqliteStore store = SqliteStore.open(db);
        store.inTransaction(r -> {
            var site = r.sites().createSite("S", NOW);
            r.devices().register("D1", site.siteId(), "D1", true, NOW);
            r.operators().create("op", "Op", Role.SUPERVISOR, NOW);
            r.incidents().insertIfAbsent(new Incident("D1/b/1", IncidentType.SOS, Severity.HIGH, "D1", IncidentState.OPEN, NOW,
                    "SIMULATED", null, null, null, EscalationLevel.NONE));
            r.incidents().appendAction(new IncidentAction(0, "D1/b/1", ActionType.OPENED, "system", null, null, null, NOW));
            return null;
        });
        return store;
    }

    @Test void appendOnlyTriggersRejectUpdateAndDelete() throws Exception {
        SqliteDatabase db = new SqliteDatabase(dir.resolve("append.db"));
        seeded(db);
        try (Connection c = db.open(true); Statement s = c.createStatement()) {
            SQLException update = assertThrows(SQLException.class, () -> s.executeUpdate("UPDATE incident_actions SET note = 'edited'"));
            assertTrue(update.getMessage().contains("append-only"));
            SQLException delete = assertThrows(SQLException.class, () -> s.executeUpdate("DELETE FROM incident_actions"));
            assertTrue(delete.getMessage().contains("append-only"));
            try (ResultSet r = s.executeQuery("SELECT COUNT(*) FROM incident_actions")) { r.next(); assertEquals(1, r.getInt(1)); }
        }
    }

    @Test void resolvingActionNeedsAValidCategoryAtDatabaseLevel() throws Exception {
        SqliteDatabase db = new SqliteDatabase(dir.resolve("category.db"));
        seeded(db);
        try (Connection c = db.open(true); Statement s = c.createStatement()) {
            assertThrows(SQLException.class, () -> s.executeUpdate(
                    "INSERT INTO incident_actions(source_identity,state,operator,operator_id,note,recorded_at) VALUES('D1/b/1','RESOLVED','Op',1,'n','x')"));
            assertThrows(SQLException.class, () -> s.executeUpdate(
                    "INSERT INTO incident_actions(source_identity,state,operator,operator_id,note,resolution_category,recorded_at) VALUES('D1/b/1','RESOLVED','Op',1,'n','NONSENSE','x')"));
            assertThrows(SQLException.class, () -> s.executeUpdate(
                    "INSERT INTO incident_actions(source_identity,state,operator,operator_id,note,resolution_category,recorded_at) VALUES('D1/b/1','NOTE','Op',1,'n','TEST','x')"));
            s.executeUpdate("INSERT INTO incident_actions(source_identity,state,operator,operator_id,note,resolution_category,recorded_at) VALUES('D1/b/1','RESOLVED','Op',1,'n','TEST','x')");
        }
    }

    @Test void incidentStateTriggersBlockSkippingAndReopening() throws Exception {
        SqliteDatabase db = new SqliteDatabase(dir.resolve("state.db"));
        seeded(db);
        try (Connection c = db.open(true); Statement s = c.createStatement()) {
            assertThrows(SQLException.class, () -> s.executeUpdate("UPDATE incidents SET state = 'RESOLVED'"));
            s.executeUpdate("UPDATE incidents SET state = 'ACKNOWLEDGED'");
            s.executeUpdate("UPDATE incidents SET state = 'RESOLVED'");
            assertThrows(SQLException.class, () -> s.executeUpdate("UPDATE incidents SET state = 'OPEN'"));
        }
    }

    @Test void foreignKeysAreEnforced() throws Exception {
        SqliteDatabase db = new SqliteDatabase(dir.resolve("fk.db"));
        seeded(db);
        try (Connection c = db.open(true); Statement s = c.createStatement()) {
            assertThrows(SQLException.class, () -> s.executeUpdate(
                    "INSERT INTO incidents(source_identity,type,severity,device_id,state,occurred_at,source_mode) VALUES('x','SOS','HIGH','NO-SUCH','OPEN','t','SIMULATED')"));
            assertThrows(SQLException.class, () -> s.executeUpdate(
                    "INSERT INTO readings(device_id,boot_id,seq,uptime_ms,received_at,environment_status,motion_status,source_mode) VALUES('D1','nosession',1,0,'t','ok','ok','SIMULATED')"));
        }
    }

    @Test void duplicateIncidentIdentityIsIgnoredByRepository() {
        SqliteDatabase db = new SqliteDatabase(dir.resolve("dup.db"));
        SqliteStore store = seeded(db);
        boolean again = store.inTransaction(r -> r.incidents().insertIfAbsent(new Incident("D1/b/1", IncidentType.SOS, Severity.HIGH, "D1",
                IncidentState.OPEN, NOW, "SIMULATED", null, null, null, EscalationLevel.NONE)));
        assertFalse(again);
        assertEquals(1, store.read(r -> r.incidents().search(com.siteguard.application.port.IncidentFilter.all())).size());
    }

    @Test void transactionRollsBackEverythingOnFailure() {
        SqliteDatabase db = new SqliteDatabase(dir.resolve("tx.db"));
        SqliteStore store = seeded(db);
        assertThrows(IllegalStateException.class, () -> store.inTransaction(r -> {
            r.workers().create("Temp", null, NOW);
            r.incidents().transition("D1/b/1", IncidentState.OPEN, IncidentState.ACKNOWLEDGED);
            throw new IllegalStateException("boom");
        }));
        assertTrue(store.read(r -> r.workers().list()).isEmpty());
        assertEquals(IncidentState.OPEN, store.read(r -> r.incidents().find("D1/b/1")).orElseThrow().state());
    }
}
