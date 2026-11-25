package siteguard.storage;

import java.io.File;

import siteguard.exceptions.StorageException;

/**
 * Chooses the storage: SQLite when the JDBC driver is available, text files otherwise.
 */
public class StoreFactory {

    public static IncidentStore open(File folder) throws StorageException {
        try {
            return new JdbcIncidentStore(folder);
        } catch (StorageException e) {
            System.out.println("SQLite not used (" + e.getMessage() + "), using text files.");
            return new FileIncidentStore(folder);
        }
    }
}
