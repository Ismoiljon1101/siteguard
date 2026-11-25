package siteguard.rules;

import siteguard.model.Reading;

/**
 * Common parent of the "value is above a limit" rules.
 * Subclasses only say which value and which limit to compare.
 */
public abstract class ThresholdRule implements Rule {

    private String name;
    private String severity;

    public ThresholdRule(String name, String severity) {
        this.name = name;
        this.severity = severity;
    }

    public String getName() { return name; }
    public String getSeverity() { return severity; }

    public boolean needsSustainedViolation() {
        return true;
    }

    /** A threshold rule is only checked when the environment sensor works. */
    public boolean isViolated(Reading reading, Thresholds thresholds) {
        if (!reading.hasEnvironment()) {
            return false;
        }
        return value(reading) > limit(thresholds);
    }

    protected abstract double value(Reading reading);

    protected abstract double limit(Thresholds thresholds);
}
