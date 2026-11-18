package com.siteguard.adapter.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * The numbered schema history. Versions 1 and 2 are the prototype (incidents, incident_actions).
 * Version 3 is the Part 2 data model. Version 4 adds operator credentials (docs/CHANGE_REQUEST_CREDENTIALS.md).
 */
public final class Migrations {
    static final String LEGACY_SITE = "Prototype (legacy)";

    private Migrations() { }

    public static List<Migration> all() {
        return List.of(new V1(), new V2(), new V3(), new V4());
    }

    private static void run(Connection c, String... statements) throws SQLException {
        try (Statement s = c.createStatement()) {
            for (String sql : statements) s.executeUpdate(sql);
        }
    }

    /** Make sure a device row exists for prototype data that names a helmet nobody registered. */
    static void ensureLegacyDevice(Connection c, String deviceId) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.executeUpdate("INSERT OR IGNORE INTO sites(name, created_at) VALUES('" + LEGACY_SITE
                    + "', strftime('%Y-%m-%dT%H:%M:%fZ','now'))");
        }
        try (var ps = c.prepareStatement("INSERT OR IGNORE INTO devices(device_id, site_id, display_name, is_simulated, status, created_at) "
                + "SELECT ?, site_id, ?, 1, 'ACTIVE', strftime('%Y-%m-%dT%H:%M:%fZ','now') FROM sites WHERE name = ?")) {
            ps.setString(1, deviceId);
            ps.setString(2, deviceId);
            ps.setString(3, LEGACY_SITE);
            ps.executeUpdate();
        }
    }

    private static final class V1 implements Migration {
        public int version() { return 1; }
        public String description() { return "prototype incidents and incident_actions"; }
        public void apply(Connection c) throws SQLException {
            run(c,
                "CREATE TABLE incidents (source_identity TEXT PRIMARY KEY, type TEXT NOT NULL, severity TEXT NOT NULL, "
                    + "device_id TEXT NOT NULL, state TEXT NOT NULL, occurred_at TEXT NOT NULL, source_mode TEXT NOT NULL)",
                "CREATE TABLE incident_actions (id INTEGER PRIMARY KEY, source_identity TEXT NOT NULL REFERENCES incidents(source_identity), "
                    + "state TEXT NOT NULL, recorded_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        }
    }

    private static final class V2 implements Migration {
        public int version() { return 2; }
        public String description() { return "prototype operator and note on actions"; }
        public void apply(Connection c) throws SQLException {
            run(c,
                "ALTER TABLE incident_actions ADD COLUMN operator TEXT NOT NULL DEFAULT ''",
                "ALTER TABLE incident_actions ADD COLUMN note TEXT");
        }
    }

    private static final class V3 implements Migration {
        public int version() { return 3; }
        public String description() { return "site, zone, operator, worker, device, assignment, reading, health, config; incident links"; }

        public void apply(Connection c) throws SQLException {
            run(c,
                "CREATE TABLE sites (site_id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE, created_at TEXT NOT NULL)",
                "CREATE TABLE zones (zone_id INTEGER PRIMARY KEY, site_id INTEGER NOT NULL REFERENCES sites(site_id), "
                    + "name TEXT NOT NULL, sort_order INTEGER NOT NULL DEFAULT 0, UNIQUE (site_id, name))",
                "CREATE TABLE operators (operator_id INTEGER PRIMARY KEY, login_id TEXT NOT NULL UNIQUE, display_name TEXT NOT NULL, "
                    + "role TEXT NOT NULL CHECK (role IN ('SAFETY_DIRECTOR','SITE_MANAGER','SUPERVISOR')))",
                "CREATE TABLE operator_sites (operator_id INTEGER NOT NULL REFERENCES operators(operator_id), "
                    + "site_id INTEGER NOT NULL REFERENCES sites(site_id), PRIMARY KEY (operator_id, site_id))",
                "CREATE TABLE workers (worker_id INTEGER PRIMARY KEY, display_name TEXT NOT NULL, trade TEXT, created_at TEXT NOT NULL)",
                "CREATE TABLE devices (device_id TEXT PRIMARY KEY, site_id INTEGER NOT NULL REFERENCES sites(site_id), "
                    + "display_name TEXT NOT NULL, is_simulated INTEGER NOT NULL CHECK (is_simulated IN (0,1)), "
                    + "status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','RETIRED')), created_at TEXT NOT NULL)",
                "CREATE TABLE helmet_assignments (assignment_id INTEGER PRIMARY KEY, device_id TEXT NOT NULL REFERENCES devices(device_id), "
                    + "worker_id INTEGER NOT NULL REFERENCES workers(worker_id), assigned_by INTEGER REFERENCES operators(operator_id), "
                    + "starts_at TEXT NOT NULL, ends_at TEXT, CHECK (ends_at IS NULL OR ends_at >= starts_at))",
                "CREATE UNIQUE INDEX ux_assignment_active_device ON helmet_assignments(device_id) WHERE ends_at IS NULL",
                "CREATE UNIQUE INDEX ux_assignment_active_worker ON helmet_assignments(worker_id) WHERE ends_at IS NULL",
                "CREATE TABLE assignment_zones (assignment_zone_id INTEGER PRIMARY KEY, "
                    + "assignment_id INTEGER NOT NULL REFERENCES helmet_assignments(assignment_id), "
                    + "zone_id INTEGER NOT NULL REFERENCES zones(zone_id), set_by INTEGER REFERENCES operators(operator_id), set_at TEXT NOT NULL)",
                "CREATE INDEX ix_assignment_zones_assignment ON assignment_zones(assignment_id, assignment_zone_id)",
                "CREATE TRIGGER assignment_zones_same_site BEFORE INSERT ON assignment_zones "
                    + "WHEN (SELECT site_id FROM zones WHERE zone_id = NEW.zone_id) IS NOT "
                    + "(SELECT d.site_id FROM helmet_assignments a JOIN devices d ON d.device_id = a.device_id WHERE a.assignment_id = NEW.assignment_id) "
                    + "BEGIN SELECT RAISE(ABORT, 'zone belongs to a different site'); END",
                "CREATE TRIGGER assignment_zones_no_update BEFORE UPDATE ON assignment_zones BEGIN SELECT RAISE(ABORT, 'assignment_zones is append-only'); END",
                "CREATE TRIGGER assignment_zones_no_delete BEFORE DELETE ON assignment_zones BEGIN SELECT RAISE(ABORT, 'assignment_zones is append-only'); END",
                "CREATE TABLE sessions (device_id TEXT NOT NULL REFERENCES devices(device_id), boot_id TEXT NOT NULL, firmware_version TEXT, "
                    + "first_seen_at TEXT NOT NULL, last_seen_at TEXT NOT NULL, PRIMARY KEY (device_id, boot_id))",
                "CREATE TABLE readings (reading_id INTEGER PRIMARY KEY, device_id TEXT NOT NULL, boot_id TEXT NOT NULL, seq INTEGER NOT NULL, "
                    + "uptime_ms INTEGER NOT NULL, received_at TEXT NOT NULL, temperature_c REAL, humidity_pct REAL, "
                    + "accel_x_mps2 REAL, accel_y_mps2 REAL, accel_z_mps2 REAL, "
                    + "environment_status TEXT NOT NULL CHECK (environment_status IN ('ok','fault')), "
                    + "motion_status TEXT NOT NULL CHECK (motion_status IN ('ok','fault')), "
                    + "source_mode TEXT NOT NULL CHECK (source_mode IN ('LIVE','SIMULATED','REPLAY')), "
                    + "UNIQUE (device_id, boot_id, seq), FOREIGN KEY (device_id, boot_id) REFERENCES sessions(device_id, boot_id), "
                    + "CHECK ((environment_status = 'fault') = (temperature_c IS NULL AND humidity_pct IS NULL)), "
                    + "CHECK (humidity_pct IS NULL OR (humidity_pct >= 0 AND humidity_pct <= 100)))",
                "CREATE INDEX ix_readings_device_time ON readings(device_id, received_at)",
                "CREATE TABLE source_events (event_id TEXT PRIMARY KEY, device_id TEXT NOT NULL, boot_id TEXT NOT NULL, seq INTEGER NOT NULL, "
                    + "event_type TEXT NOT NULL CHECK (event_type IN ('SOS','MOTION')), uptime_ms INTEGER NOT NULL, received_at TEXT NOT NULL, "
                    + "committed_at TEXT NOT NULL, source_mode TEXT NOT NULL CHECK (source_mode IN ('LIVE','SIMULATED','REPLAY')), "
                    + "UNIQUE (device_id, boot_id, seq), FOREIGN KEY (device_id, boot_id) REFERENCES sessions(device_id, boot_id))",
                "CREATE TABLE health_changes (health_change_id INTEGER PRIMARY KEY, device_id TEXT NOT NULL REFERENCES devices(device_id), "
                    + "state TEXT NOT NULL CHECK (state IN ('ONLINE','STALE','OFFLINE','SENSOR_FAULT','NO_DATA','UNASSIGNED')), "
                    + "changed_at TEXT NOT NULL, source_mode TEXT NOT NULL CHECK (source_mode IN ('LIVE','SIMULATED','REPLAY')))",
                "CREATE INDEX ix_health_changes_device ON health_changes(device_id, health_change_id)",
                "CREATE TABLE config_versions (config_version INTEGER PRIMARY KEY, temp_warn_c REAL NOT NULL, "
                    + "temp_consecutive_samples INTEGER NOT NULL CHECK (temp_consecutive_samples >= 1), motion_threshold_mps2 REAL, "
                    + "created_by INTEGER REFERENCES operators(operator_id), created_at TEXT NOT NULL)",
                // Placeholders per D9 and D10. The motion threshold stays NULL until A08 bench measurement.
                "INSERT INTO config_versions(config_version, temp_warn_c, temp_consecutive_samples, motion_threshold_mps2, created_by, created_at) "
                    + "VALUES (1, 35.0, 3, NULL, NULL, strftime('%Y-%m-%dT%H:%M:%fZ','now'))");

            // Adopt prototype incidents whose helmet was never registered, so the new device foreign key holds.
            try (Statement s = c.createStatement();
                 var rows = s.executeQuery("SELECT DISTINCT device_id FROM incidents")) {
                java.util.List<String> ids = new java.util.ArrayList<>();
                while (rows.next()) ids.add(rows.getString(1));
                for (String id : ids) ensureLegacyDevice(c, id);
            }

            // Rebuild incidents to add the device foreign key and the new link columns (SQLite cannot ALTER a foreign key in).
            run(c,
                "CREATE TABLE incidents_new (source_identity TEXT PRIMARY KEY, "
                    + "type TEXT NOT NULL CHECK (type IN ('SOS','MOTION','ENVIRONMENT')), "
                    + "severity TEXT NOT NULL CHECK (severity IN ('HIGH','WARNING')), "
                    + "device_id TEXT NOT NULL REFERENCES devices(device_id), "
                    + "state TEXT NOT NULL CHECK (state IN ('OPEN','ACKNOWLEDGED','RESOLVED')), "
                    + "occurred_at TEXT NOT NULL, source_mode TEXT NOT NULL, "
                    + "assignment_id INTEGER REFERENCES helmet_assignments(assignment_id), "
                    + "config_version INTEGER REFERENCES config_versions(config_version), "
                    + "trigger_reading_id INTEGER REFERENCES readings(reading_id), "
                    + "escalation_level INTEGER NOT NULL DEFAULT 0 CHECK (escalation_level IN (0,1,2)))",
                "INSERT INTO incidents_new(source_identity, type, severity, device_id, state, occurred_at, source_mode) "
                    + "SELECT source_identity, type, severity, device_id, state, occurred_at, source_mode FROM incidents",
                "DROP TABLE incidents",
                "ALTER TABLE incidents_new RENAME TO incidents",
                "CREATE INDEX ix_incidents_device_state ON incidents(device_id, state)",
                "CREATE INDEX ix_incidents_assignment ON incidents(assignment_id)",
                "CREATE TRIGGER incidents_no_reopen BEFORE UPDATE OF state ON incidents "
                    + "WHEN OLD.state = 'RESOLVED' AND NEW.state <> 'RESOLVED' "
                    + "BEGIN SELECT RAISE(ABORT, 'resolved incidents cannot be reopened'); END",
                "CREATE TRIGGER incidents_no_skip BEFORE UPDATE OF state ON incidents "
                    + "WHEN OLD.state = 'OPEN' AND NEW.state = 'RESOLVED' "
                    + "BEGIN SELECT RAISE(ABORT, 'incident must be acknowledged before it is resolved'); END",
                "ALTER TABLE incident_actions ADD COLUMN operator_id INTEGER REFERENCES operators(operator_id)",
                "ALTER TABLE incident_actions ADD COLUMN resolution_category TEXT",
                "CREATE INDEX ix_incident_actions_identity ON incident_actions(source_identity, id)",
                "CREATE TRIGGER incident_actions_no_update BEFORE UPDATE ON incident_actions BEGIN SELECT RAISE(ABORT, 'incident_actions is append-only'); END",
                "CREATE TRIGGER incident_actions_no_delete BEFORE DELETE ON incident_actions BEGIN SELECT RAISE(ABORT, 'incident_actions is append-only'); END",
                // The category is mandatory on resolving rows written through the new services (operator_id set);
                // prototype rows (no operator_id) are tolerated so the old repository keeps working.
                "CREATE TRIGGER incident_actions_category BEFORE INSERT ON incident_actions "
                    + "WHEN (NEW.resolution_category IS NOT NULL AND NEW.resolution_category NOT IN "
                    + "('FALSE_ALARM','TEST','ASSISTANCE_GIVEN','NO_ACTION_NEEDED','OTHER')) "
                    + "OR (NEW.resolution_category IS NOT NULL AND NEW.state <> 'RESOLVED') "
                    + "OR (NEW.state = 'RESOLVED' AND NEW.operator_id IS NOT NULL AND NEW.resolution_category IS NULL) "
                    + "BEGIN SELECT RAISE(ABORT, 'invalid or missing resolution category'); END");
        }
    }

    private static final class V4 implements Migration {
        public int version() { return 4; }
        public String description() { return "operator credentials (PBKDF2) and login throttle"; }
        public void apply(Connection c) throws SQLException {
            run(c,
                "ALTER TABLE operators ADD COLUMN password_hash TEXT",
                "ALTER TABLE operators ADD COLUMN salt TEXT",
                "ALTER TABLE operators ADD COLUMN iterations INTEGER",
                "ALTER TABLE operators ADD COLUMN created_at TEXT",
                "ALTER TABLE operators ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0",
                "ALTER TABLE operators ADD COLUMN locked_until TEXT");
        }
    }
}
