package siteguard.model;

/**
 * Parent class of everything a helmet sends to Java (Chapter 9: inheritance).
 * Reading and HelmetEvent extend this class and inherit the identity fields.
 */
public abstract class Message {

    protected String deviceId;
    protected String bootId;
    protected int seq;
    protected long uptimeMs;
    protected boolean simulated;
    protected long receivedAtMs;

    public Message(String deviceId, String bootId, int seq, long uptimeMs, boolean simulated) {
        this.deviceId = deviceId;
        this.bootId = bootId;
        this.seq = seq;
        this.uptimeMs = uptimeMs;
        this.simulated = simulated;
    }

    /** Unique identity of this message: device / boot / sequence number. */
    public String getSourceId() {
        return deviceId + "/" + bootId + "/" + seq;
    }

    /** Each subclass describes itself differently (Chapter 10: overriding). */
    public abstract String describe();

    public String getDeviceId() { return deviceId; }
    public String getBootId() { return bootId; }
    public int getSeq() { return seq; }
    public long getUptimeMs() { return uptimeMs; }
    public boolean isSimulated() { return simulated; }
    public long getReceivedAtMs() { return receivedAtMs; }

    public void setSimulated(boolean simulated) { this.simulated = simulated; }
    public void setReceivedAtMs(long receivedAtMs) { this.receivedAtMs = receivedAtMs; }
}
