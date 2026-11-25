package siteguard.exceptions;

/**
 * Thrown when the operator tries an action that is not allowed,
 * for example resolving an incident that is already resolved.
 */
public class InvalidActionException extends Exception {

    public InvalidActionException(String message) {
        super(message);
    }
}
