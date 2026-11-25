package siteguard.rules;

import siteguard.model.Incident;
import siteguard.model.Reading;

/**
 * Creates an incident when a helmet reports that one of its sensors failed.
 */
public class SensorFaultRule implements Rule {

    public String getName() { return "Sensor fault"; }
    public String getSeverity() { return Incident.MEDIUM; }

    public boolean isViolated(Reading reading, Thresholds thresholds) {
        return !reading.hasEnvironment() || !reading.hasMotion();
    }

    /** Fault must also repeat, so one bad sample does not raise an incident. */
    public boolean needsSustainedViolation() {
        return true;
    }
}
