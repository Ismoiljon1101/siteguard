package siteguard.storage;

import java.util.ArrayList;

import siteguard.exceptions.StorageException;
import siteguard.model.Incident;
import siteguard.model.Reading;

/**
 * Where incidents and readings are kept so they survive a restart.
 * Two classes implement it: FileIncidentStore (text files) and JdbcIncidentStore (SQLite).
 */
public interface IncidentStore {

    String getDescription();

    void saveNewIncident(Incident incident) throws StorageException;

    /** action is ACKNOWLEDGED, RESOLVED or NOTE. */
    void saveAction(int incidentId, String action, long timeMs, String note) throws StorageException;

    void saveReading(Reading reading) throws StorageException;

    ArrayList<Incident> loadIncidents() throws StorageException;
}
