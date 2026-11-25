package siteguard.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import siteguard.exceptions.StorageException;
import siteguard.model.Incident;
import siteguard.model.Reading;

/**
 * Saves incidents in an SQLite database through JDBC.
 * It needs the SQLite JDBC driver jar (lib/sqlite-jdbc.jar) on the class path.
 * If the jar is missing, StoreFactory uses FileIncidentStore instead.
 */
public class JdbcIncidentStore implements IncidentStore {

    private String url;

    public JdbcIncidentStore(File folder) throws StorageException {
        if (!folder.exists() && !folder.mkdirs()) {
            throw new StorageException("Cannot create folder " + folder.getAbsolutePath());
        }
        url = "jdbc:sqlite:" + new File(folder, "siteguard.db").getPath();
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new StorageException("SQLite JDBC driver is not on the class path");
        }
        Connection con = null;
        try {
            con = DriverManager.getConnection(url);
            Statement st = con.createStatement();
            st.executeUpdate("CREATE TABLE IF NOT EXISTS incident (id INTEGER PRIMARY KEY, device_id TEXT NOT NULL,"
                    + " source_id TEXT NOT NULL UNIQUE, cause TEXT NOT NULL, severity TEXT NOT NULL,"
                    + " created_at_ms INTEGER NOT NULL, simulated INTEGER NOT NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS incident_action (action_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " incident_id INTEGER NOT NULL REFERENCES incident(id), action TEXT NOT NULL,"
                    + " time_ms INTEGER NOT NULL, note TEXT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS reading (received_ms INTEGER, device_id TEXT, boot_id TEXT,"
                    + " seq INTEGER, simulated INTEGER, temperature_c REAL, humidity_pct REAL, accel REAL,"
                    + " env_status TEXT, motion_status TEXT)");
            st.close();
        } catch (SQLException e) {
            throw new StorageException("Cannot open database: " + e.getMessage());
        } finally {
            close(con);
        }
    }

    public String getDescription() {
        return "SQLite database " + url.substring("jdbc:sqlite:".length());
    }

    public void saveNewIncident(Incident i) throws StorageException {
        Connection con = null;
        try {
            con = DriverManager.getConnection(url);
            PreparedStatement ps = con.prepareStatement("INSERT INTO incident VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, i.getId());
            ps.setString(2, i.getDeviceId());
            ps.setString(3, i.getSourceId());
            ps.setString(4, i.getCause());
            ps.setString(5, i.getSeverity());
            ps.setLong(6, i.getCreatedAtMs());
            ps.setInt(7, i.isSimulated() ? 1 : 0);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            throw new StorageException("Cannot save incident " + i.getId() + ": " + e.getMessage());
        } finally {
            close(con);
        }
    }

    public void saveAction(int incidentId, String action, long timeMs, String note) throws StorageException {
        Connection con = null;
        try {
            con = DriverManager.getConnection(url);
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO incident_action (incident_id, action, time_ms, note) VALUES (?, ?, ?, ?)");
            ps.setInt(1, incidentId);
            ps.setString(2, action);
            ps.setLong(3, timeMs);
            ps.setString(4, note);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            throw new StorageException("Cannot save action: " + e.getMessage());
        } finally {
            close(con);
        }
    }

    public void saveReading(Reading r) throws StorageException {
        Connection con = null;
        try {
            con = DriverManager.getConnection(url);
            PreparedStatement ps = con.prepareStatement("INSERT INTO reading VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, r.getReceivedAtMs());
            ps.setString(2, r.getDeviceId());
            ps.setString(3, r.getBootId());
            ps.setInt(4, r.getSeq());
            ps.setInt(5, r.isSimulated() ? 1 : 0);
            if (r.hasEnvironment()) {
                ps.setDouble(6, r.getTemperatureC());
                ps.setDouble(7, r.getHumidityPct());
            } else {
                ps.setNull(6, java.sql.Types.REAL);
                ps.setNull(7, java.sql.Types.REAL);
            }
            if (r.hasMotion()) {
                ps.setDouble(8, r.getAccelMagnitude());
            } else {
                ps.setNull(8, java.sql.Types.REAL);
            }
            ps.setString(9, r.getEnvironmentStatus());
            ps.setString(10, r.getMotionStatus());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            throw new StorageException("Cannot save reading: " + e.getMessage());
        } finally {
            close(con);
        }
    }

    public ArrayList<Incident> loadIncidents() throws StorageException {
        ArrayList<Incident> list = new ArrayList<Incident>();
        Connection con = null;
        try {
            con = DriverManager.getConnection(url);
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM incident ORDER BY id");
            while (rs.next()) {
                list.add(new Incident(rs.getInt("id"), rs.getString("device_id"), rs.getString("source_id"),
                        rs.getString("cause"), rs.getString("severity"), rs.getLong("created_at_ms"),
                        rs.getInt("simulated") == 1));
            }
            rs.close();
            rs = st.executeQuery("SELECT * FROM incident_action ORDER BY action_id");
            while (rs.next()) {
                Incident incident = FileIncidentStore.find(list, rs.getInt("incident_id"));
                if (incident != null) {
                    FileIncidentStore.applyAction(incident, rs.getString("action"), rs.getLong("time_ms"),
                            rs.getString("note"));
                }
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            throw new StorageException("Cannot load incidents: " + e.getMessage());
        } finally {
            close(con);
        }
        return list;
    }

    private void close(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                System.out.println("Could not close database: " + e.getMessage());
            }
        }
    }
}
