package com.siteguard.adapter.sqlite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

class SqliteIncidentRepositoryTest {
    @TempDir Path directory;
    private IncidentRecord incident() {
        return new IncidentRecord("device/boot/1", "SOS", "HIGH", "device", "OPEN", "2026-01-01T00:00:00Z", "SIMULATION");
    }

    @Test void migrationRecoveryAndDuplicateProtection() throws Exception {
        Path path = directory.resolve("incidents.db");
        var first = new SqliteIncidentRepository(path);
        first.initialize();
        first.initialize();
        assertTrue(first.save(incident()));
        var restarted = new SqliteIncidentRepository(path);
        restarted.initialize();
        assertFalse(restarted.save(incident()));
        assertEquals(java.util.List.of(incident()), restarted.list());
        assertTrue(restarted.updateState(incident().sourceIdentity(), "ACKNOWLEDGED"));
        assertEquals("ACKNOWLEDGED", new SqliteIncidentRepository(path).list().get(0).state());
        assertFalse(restarted.updateState("missing", "ACKNOWLEDGED"));
    }

    @Test void actionFailureRollsBackStateUpdate() throws Exception {
        Path path = directory.resolve("incidents.db");
        var repository = new SqliteIncidentRepository(path);
        repository.initialize();
        repository.save(incident());
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
             var statement = connection.createStatement()) {
            statement.execute("CREATE TRIGGER reject_action BEFORE INSERT ON incident_actions BEGIN SELECT RAISE(ABORT, 'test failure'); END");
        }
        assertThrows(SQLException.class, () -> repository.updateState(incident().sourceIdentity(), "ACKNOWLEDGED"));
        assertEquals("OPEN", repository.list().get(0).state());
    }

    @Test void workflowPersistsOperatorNotesAndRejectsInvalidTransitions() throws Exception {
        Path path = directory.resolve("workflow.db");
        var repository = new SqliteIncidentRepository(path);
        repository.initialize();
        repository.save(incident());
        String identity = incident().sourceIdentity();
        assertFalse(repository.resolve(identity, "operator"));
        assertTrue(repository.acknowledge(identity, "Alice"));
        assertFalse(repository.acknowledge(identity, "Alice"));
        assertTrue(repository.addNote(identity, "Bob", "Checked device"));
        assertTrue(repository.resolve(identity, "Alice", "Issue addressed"));
        assertThrows(IllegalArgumentException.class, () -> repository.addNote(identity, "Bob", " "));
        var restarted = new SqliteIncidentRepository(path);
        assertEquals("RESOLVED", restarted.list().get(0).state());
        var actions = restarted.actions(identity);
        assertEquals(3, actions.size());
        assertEquals("Alice", actions.get(0).operator());
        assertEquals("NOTE", actions.get(1).type());
        assertEquals("Checked device", actions.get(1).note());
        assertEquals("RESOLVED", actions.get(2).type());
        assertEquals("Issue addressed", actions.get(2).note());
    }

}
