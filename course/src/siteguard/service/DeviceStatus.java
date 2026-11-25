package siteguard.service;

import java.util.ArrayList;

import siteguard.model.Reading;

/**
 * What Java currently knows about one helmet.
 */
public class DeviceStatus {

    public static final int HISTORY_SIZE = 60;

    private String deviceId;
    private boolean simulated;
    private String sourceName;
    private Reading latest;
    private long lastReceivedMs;
    private int[] violationCounts;
    private boolean staleReported;
    private ArrayList<Double> temperatureHistory = new ArrayList<Double>();
    private ArrayList<Double> humidityHistory = new ArrayList<Double>();

    public DeviceStatus(String deviceId, int ruleCount) {
        this.deviceId = deviceId;
        this.violationCounts = new int[ruleCount];
    }

    /**
     * Stores a new reading. Returns false (and keeps the old reading)
     * when the reading is older than the one already shown.
     */
    public boolean update(Reading reading, String sourceName) {
        this.sourceName = sourceName;
        this.simulated = reading.isSimulated();
        this.lastReceivedMs = reading.getReceivedAtMs();
        if (latest != null
                && latest.getBootId().equals(reading.getBootId())
                && reading.getSeq() < latest.getSeq()) {
            return false;
        }
        latest = reading;
        if (reading.hasEnvironment()) {
            addLimited(temperatureHistory, reading.getTemperatureC());
            addLimited(humidityHistory, reading.getHumidityPct());
        }
        return true;
    }

    public void touch(long nowMs, String sourceName, boolean simulated) {
        this.lastReceivedMs = nowMs;
        this.sourceName = sourceName;
        this.simulated = simulated;
    }

    private void addLimited(ArrayList<Double> list, double value) {
        list.add(value);
        if (list.size() > HISTORY_SIZE) {
            list.remove(0);
        }
    }

    /** Seconds since the last message from this helmet. */
    public long getAgeSeconds(long nowMs) {
        if (lastReceivedMs == 0) {
            return -1;
        }
        return (nowMs - lastReceivedMs) / 1000;
    }

    public boolean isStale(long nowMs, int staleSeconds) {
        return lastReceivedMs == 0 || nowMs - lastReceivedMs > staleSeconds * 1000L;
    }

    /** Counts violations in a row; returns the new count. A good reading resets it to 0. */
    public int countViolation(int ruleIndex, boolean violated) {
        if (violated) {
            violationCounts[ruleIndex]++;
        } else {
            violationCounts[ruleIndex] = 0;
        }
        return violationCounts[ruleIndex];
    }

    public boolean isStaleReported() { return staleReported; }
    public void setStaleReported(boolean staleReported) { this.staleReported = staleReported; }

    public String getDeviceId() { return deviceId; }
    public boolean isSimulated() { return simulated; }
    public String getSourceName() { return sourceName; }
    public Reading getLatest() { return latest; }
    public long getLastReceivedMs() { return lastReceivedMs; }
    public ArrayList<Double> getTemperatureHistory() { return temperatureHistory; }
    public ArrayList<Double> getHumidityHistory() { return humidityHistory; }
}
