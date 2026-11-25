package siteguard.model;

import java.util.ArrayList;

import siteguard.exceptions.InvalidActionException;

/**
 * An incident the operator must deal with.
 * State moves NEW -> ACKNOWLEDGED -> RESOLVED (NEW -> RESOLVED is also allowed).
 * Every action is added to the history, so earlier actions are never lost.
 */
public class Incident {

    public static final String NEW = "NEW";
    public static final String ACKNOWLEDGED = "ACKNOWLEDGED";
    public static final String RESOLVED = "RESOLVED";

    public static final String HIGH = "HIGH";
    public static final String MEDIUM = "MEDIUM";
    public static final String LOW = "LOW";

    private int id;
    private String deviceId;
    private String sourceId;
    private String cause;
    private String severity;
    private long createdAtMs;
    private boolean simulated;
    private String state;
    private ArrayList<String> history = new ArrayList<String>();

    public Incident(int id, String deviceId, String sourceId, String cause, String severity,
                    long createdAtMs, boolean simulated) {
        this.id = id;
        this.deviceId = deviceId;
        this.sourceId = sourceId;
        this.cause = cause;
        this.severity = severity;
        this.createdAtMs = createdAtMs;
        this.simulated = simulated;
        this.state = NEW;
        history.add(TimeText.format(createdAtMs) + " | CREATED | " + cause);
    }

    public void acknowledge(long timeMs, String note) throws InvalidActionException {
        if (!state.equals(NEW)) {
            throw new InvalidActionException("Incident " + id + " is " + state + " and cannot be acknowledged");
        }
        state = ACKNOWLEDGED;
        addHistory(timeMs, "ACKNOWLEDGED", note);
    }

    public void resolve(long timeMs, String note) throws InvalidActionException {
        if (state.equals(RESOLVED)) {
            throw new InvalidActionException("Incident " + id + " is already resolved");
        }
        if (note == null || note.trim().isEmpty()) {
            throw new InvalidActionException("A note is required to resolve an incident");
        }
        state = RESOLVED;
        addHistory(timeMs, "RESOLVED", note);
    }

    public void addNote(long timeMs, String note) throws InvalidActionException {
        if (note == null || note.trim().isEmpty()) {
            throw new InvalidActionException("The note is empty");
        }
        addHistory(timeMs, "NOTE", note);
    }

    private void addHistory(long timeMs, String action, String note) {
        String text = (note == null) ? "" : note.trim();
        history.add(TimeText.format(timeMs) + " | " + action + " | " + text);
    }

    public boolean isOpen() {
        return !state.equals(RESOLVED);
    }

    /** One line used by the incident list in the GUI. */
    public String summary() {
        String label = simulated ? " [SIMULATED]" : "";
        return "#" + id + "  " + severity + "  " + state + "  " + deviceId + "  "
                + cause + "  (" + TimeText.format(createdAtMs) + ")" + label;
    }

    public int getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public String getSourceId() { return sourceId; }
    public String getCause() { return cause; }
    public String getSeverity() { return severity; }
    public long getCreatedAtMs() { return createdAtMs; }
    public boolean isSimulated() { return simulated; }
    public String getState() { return state; }
    public ArrayList<String> getHistory() { return history; }
}
