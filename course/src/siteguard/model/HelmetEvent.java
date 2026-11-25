package siteguard.model;

/**
 * An event decided by the helmet itself: the SOS button or sudden motion.
 */
public class HelmetEvent extends Message {

    public static final String SOS = "SOS";
    public static final String MOTION = "MOTION";

    private String eventType;

    public HelmetEvent(String deviceId, String bootId, int seq, long uptimeMs, boolean simulated,
                       String eventType) {
        super(deviceId, bootId, seq, uptimeMs, simulated);
        this.eventType = eventType;
    }

    public String getEventType() { return eventType; }

    @Override
    public String describe() {
        return deviceId + " event #" + seq + ": " + eventType;
    }
}
