package siteguard.exceptions;

/**
 * Thrown when a line from a helmet (or the simulator) cannot be understood.
 * Chapter 11: a checked exception made by extending the Exception class.
 */
public class InvalidMessageException extends Exception {

    public InvalidMessageException(String message) {
        super(message);
    }
}
