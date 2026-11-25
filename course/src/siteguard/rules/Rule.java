package siteguard.rules;

import siteguard.model.Reading;

/**
 * A rule that checks one reading (Chapter 10: polymorphism through an interface).
 * MonitorService keeps all rules in one list and calls isViolated() on each,
 * without knowing which kind of rule it is.
 */
public interface Rule {

    /** Short name used as the incident cause, for example "High temperature". */
    String getName();

    String getSeverity();

    /** True when the reading breaks this rule. */
    boolean isViolated(Reading reading, Thresholds thresholds);

    /** True when the rule must fail several readings in a row before an incident is created. */
    boolean needsSustainedViolation();
}
