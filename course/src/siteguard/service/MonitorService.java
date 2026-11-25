package siteguard.service;

import java.util.ArrayList;

import siteguard.exceptions.InvalidActionException;
import siteguard.exceptions.InvalidMessageException;
import siteguard.exceptions.StorageException;
import siteguard.model.HelmetEvent;
import siteguard.model.Incident;
import siteguard.model.Message;
import siteguard.model.Reading;
import siteguard.model.TimeText;
import siteguard.rules.HumidityRule;
import siteguard.rules.Rule;
import siteguard.rules.SensorFaultRule;
import siteguard.rules.TemperatureRule;
import siteguard.rules.Thresholds;
import siteguard.source.DataSource;
import siteguard.source.LineListener;
import siteguard.storage.IncidentStore;

/**
 * The main logic of SiteGuard. It does not use Swing, so it can be tested on its own.
 *
 * For every line: parse -> reject duplicates -> update the helmet's status
 * -> check the rules -> create and save incidents -> send a receipt.
 */
public class MonitorService implements LineListener {

    public static final int MAX_SEEN_IDS = 5000;
    public static final int MAX_LOG_LINES = 300;
    public static final int SAVE_EVERY_NTH_READING = 5;

    private Thresholds thresholds = new Thresholds();
    private Rule[] rules = {new TemperatureRule(), new HumidityRule(), new SensorFaultRule()};
    private MessageParser parser = new MessageParser();
    private IncidentStore store;

    private ArrayList<DeviceStatus> devices = new ArrayList<DeviceStatus>();
    private ArrayList<Incident> incidents;
    private ArrayList<String> seenIds = new ArrayList<String>();
    private ArrayList<String> log = new ArrayList<String>();
    private MonitorListener listener;

    private int nextIncidentId = 1;
    private int readingCount;
    private int invalidCount;
    private int duplicateCount;
    private String storageProblem;

    public MonitorService(IncidentStore store) throws StorageException {
        this.store = store;
        incidents = store.loadIncidents();
        for (Incident i : incidents) {
            remember(i.getSourceId());
            if (i.getId() >= nextIncidentId) {
                nextIncidentId = i.getId() + 1;
            }
        }
        addLog("Loaded " + incidents.size() + " incidents from " + store.getDescription(),
                System.currentTimeMillis());
    }

    public void setListener(MonitorListener listener) {
        this.listener = listener;
    }

    // ---------- input from data sources (called on the Swing event thread) ----------

    public void lineReceived(String line, DataSource source) {
        handleLine(line, source, System.currentTimeMillis());
    }

    public void sourceStatus(DataSource source, String status) {
        addLog(source.getName() + ": " + status, System.currentTimeMillis());
        changed();
    }

    /** Same as lineReceived but with the time passed in, so tests can control the clock. */
    public void handleLine(String line, DataSource source, long nowMs) {
        Message message;
        try {
            message = parser.parse(line);
        } catch (InvalidMessageException e) {
            invalidCount++;
            addLog("Rejected message from " + source.getName() + ": " + e.getMessage(), nowMs);
            changed();
            return;
        }

        // Simulated and replayed data is always labelled, whatever the line says.
        if (source.isSimulated()) {
            message.setSimulated(true);
        }
        message.setReceivedAtMs(nowMs);

        if (seenIds.contains(message.getSourceId())) {
            duplicateCount++;
            addLog("Duplicate ignored: " + message.getSourceId(), nowMs);
            if (message instanceof HelmetEvent) {
                // The helmet did not get our first receipt; it is already stored, so confirm again.
                sendReceipt(source, message.getSourceId());
            }
            changed();
            return;
        }
        remember(message.getSourceId());

        if (message instanceof Reading) {
            handleReading((Reading) message, source, nowMs);
        } else {
            handleEvent((HelmetEvent) message, source, nowMs);
        }
        changed();
    }

    private void handleReading(Reading reading, DataSource source, long nowMs) {
        DeviceStatus device = getOrCreateDevice(reading.getDeviceId());
        if (device.isStaleReported()) {
            addLog(reading.getDeviceId() + " is sending again", nowMs);
            device.setStaleReported(false);
        }
        if (!device.update(reading, source.getName())) {
            addLog("Old reading ignored for display: " + reading.getSourceId(), nowMs);
            return;
        }
        readingCount++;
        if (readingCount % SAVE_EVERY_NTH_READING == 0) {
            try {
                store.saveReading(reading);
            } catch (StorageException e) {
                storageFailed(e, nowMs);
            }
        }

        for (int i = 0; i < rules.length; i++) {
            boolean violated = rules[i].isViolated(reading, thresholds);
            int count = device.countViolation(i, violated);
            int needed = rules[i].needsSustainedViolation() ? thresholds.getSustainedSamples() : 1;
            if (count == needed && findOpen(reading.getDeviceId(), rules[i].getName()) == null) {
                createIncident(reading, rules[i].getName(), rules[i].getSeverity(), nowMs);
            }
        }
    }

    private void handleEvent(HelmetEvent event, DataSource source, long nowMs) {
        DeviceStatus device = getOrCreateDevice(event.getDeviceId());
        device.touch(nowMs, source.getName(), event.isSimulated());
        String cause;
        if (event.getEventType().equals(HelmetEvent.SOS)) {
            cause = "SOS button pressed";
        } else {
            cause = "Sudden motion (not a fall diagnosis)";
        }
        Incident incident = createIncident(event, cause, Incident.HIGH, nowMs);
        if (incident != null) {
            sendReceipt(source, event.getSourceId());
        }
    }

    /** Creates, saves and returns an incident, or returns null when saving failed. */
    private Incident createIncident(Message cause, String text, String severity, long nowMs) {
        Incident incident = new Incident(nextIncidentId, cause.getDeviceId(), cause.getSourceId(),
                text, severity, nowMs, cause.isSimulated());
        nextIncidentId++;
        // Show it even if saving fails: the operator must still see an SOS.
        incidents.add(incident);
        addLog("NEW incident " + incident.summary(), nowMs);
        try {
            store.saveNewIncident(incident);
            return incident;
        } catch (StorageException e) {
            storageFailed(e, nowMs);
            return null;
        }
    }

    private void sendReceipt(DataSource source, String sourceId) {
        String receipt = "R," + sourceId + ",stored";
        if (!source.send(receipt)) {
            addLog("Receipt " + receipt + " (not sent: " + source.getName() + " has no return channel)",
                    System.currentTimeMillis());
        }
    }

    private void storageFailed(StorageException e, long nowMs) {
        storageProblem = e.getMessage();
        addLog("STORAGE ERROR: " + e.getMessage(), nowMs);
    }

    // ---------- checks run by the GUI timer ----------

    /** Marks helmets that stopped sending. Returns how many are stale. */
    public int checkStale(long nowMs) {
        int stale = 0;
        for (DeviceStatus d : devices) {
            if (d.isStale(nowMs, thresholds.getStaleSeconds())) {
                stale++;
                if (!d.isStaleReported()) {
                    d.setStaleReported(true);
                    addLog(d.getDeviceId() + " readings are STALE (no data for "
                            + thresholds.getStaleSeconds() + " s)", nowMs);
                    changed();
                }
            }
        }
        return stale;
    }

    // ---------- operator actions ----------

    public void acknowledge(int id, String note, long nowMs) throws InvalidActionException, StorageException {
        Incident incident = getIncident(id);
        incident.acknowledge(nowMs, note);
        saveAction(incident, Incident.ACKNOWLEDGED, nowMs, note);
    }

    public void resolve(int id, String note, long nowMs) throws InvalidActionException, StorageException {
        Incident incident = getIncident(id);
        incident.resolve(nowMs, note);
        saveAction(incident, Incident.RESOLVED, nowMs, note);
    }

    public void addNote(int id, String note, long nowMs) throws InvalidActionException, StorageException {
        Incident incident = getIncident(id);
        incident.addNote(nowMs, note);
        saveAction(incident, "NOTE", nowMs, note);
    }

    private void saveAction(Incident incident, String action, long nowMs, String note) throws StorageException {
        addLog("Incident #" + incident.getId() + " " + action, nowMs);
        changed();
        try {
            store.saveAction(incident.getId(), action, nowMs, note);
        } catch (StorageException e) {
            storageFailed(e, nowMs);
            throw e;
        }
    }

    public Incident getIncident(int id) throws InvalidActionException {
        for (Incident i : incidents) {
            if (i.getId() == id) {
                return i;
            }
        }
        throw new InvalidActionException("There is no incident #" + id);
    }

    /**
     * Incidents for the list in the GUI, newest first.
     * severity is "ALL" or one of HIGH / MEDIUM / LOW; deviceText may be empty.
     */
    public ArrayList<Incident> filterIncidents(boolean openOnly, String severity, String deviceText) {
        ArrayList<Incident> result = new ArrayList<Incident>();
        String device = deviceText.trim().toLowerCase();
        for (int k = incidents.size() - 1; k >= 0; k--) {
            Incident i = incidents.get(k);
            if (openOnly && !i.isOpen()) {
                continue;
            }
            if (!severity.equals("ALL") && !i.getSeverity().equals(severity)) {
                continue;
            }
            if (!device.isEmpty() && !i.getDeviceId().toLowerCase().contains(device)) {
                continue;
            }
            result.add(i);
        }
        return result;
    }

    public int countOpen() {
        int n = 0;
        for (Incident i : incidents) {
            if (i.isOpen()) {
                n++;
            }
        }
        return n;
    }

    // ---------- helpers ----------

    private Incident findOpen(String deviceId, String cause) {
        for (Incident i : incidents) {
            if (i.isOpen() && i.getDeviceId().equals(deviceId) && i.getCause().equals(cause)) {
                return i;
            }
        }
        return null;
    }

    private DeviceStatus getOrCreateDevice(String deviceId) {
        for (DeviceStatus d : devices) {
            if (d.getDeviceId().equals(deviceId)) {
                return d;
            }
        }
        DeviceStatus d = new DeviceStatus(deviceId, rules.length);
        devices.add(d);
        return d;
    }

    private void remember(String sourceId) {
        seenIds.add(sourceId);
        if (seenIds.size() > MAX_SEEN_IDS) {
            seenIds.remove(0);
        }
    }

    private void addLog(String text, long timeMs) {
        log.add(TimeText.clock(timeMs) + "  " + text);
        if (log.size() > MAX_LOG_LINES) {
            log.remove(0);
        }
    }

    private void changed() {
        if (listener != null) {
            listener.monitorChanged();
        }
    }

    public Thresholds getThresholds() { return thresholds; }
    public ArrayList<DeviceStatus> getDevices() { return devices; }
    public ArrayList<Incident> getIncidents() { return incidents; }
    public ArrayList<String> getLog() { return log; }
    public IncidentStore getStore() { return store; }
    public String getStorageProblem() { return storageProblem; }
    public int getInvalidCount() { return invalidCount; }
    public int getDuplicateCount() { return duplicateCount; }
}
