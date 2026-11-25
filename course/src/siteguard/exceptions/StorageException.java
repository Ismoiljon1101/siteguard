package siteguard.exceptions;

/**
 * Thrown when incidents or readings cannot be saved or loaded.
 */
public class StorageException extends Exception {

    public StorageException(String message) {
        super(message);
    }
}
