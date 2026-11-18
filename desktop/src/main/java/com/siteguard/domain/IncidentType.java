package com.siteguard.domain;

public enum IncidentType {
    SOS(Severity.HIGH), MOTION(Severity.WARNING), ENVIRONMENT(Severity.WARNING);

    private final Severity severity;
    IncidentType(Severity severity) { this.severity = severity; }
    public Severity severity() { return severity; }

    public static IncidentType fromEvent(EventType type) {
        return switch (type) { case SOS -> SOS; case MOTION -> MOTION; };
    }
}
