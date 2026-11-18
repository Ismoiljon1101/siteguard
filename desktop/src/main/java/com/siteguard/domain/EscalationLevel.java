package com.siteguard.domain;

import java.time.Duration;

/** Unacknowledged-SOS escalation (D20, PROPOSED). Timers are placeholders. On-screen and sound only. */
public enum EscalationLevel {
    NONE(0), LEVEL_1(1), LEVEL_2(2);

    public static final Duration LEVEL_1_AFTER = Duration.ofSeconds(30);
    public static final Duration LEVEL_2_AFTER = Duration.ofSeconds(120);

    private final int level;
    EscalationLevel(int level) { this.level = level; }
    public int level() { return level; }

    public static EscalationLevel fromLevel(int level) {
        for (EscalationLevel e : values()) if (e.level == level) return e;
        throw new IllegalArgumentException("Unknown escalation level " + level);
    }

    public static EscalationLevel forUnacknowledgedAge(Duration age) {
        if (age.compareTo(LEVEL_2_AFTER) >= 0) return LEVEL_2;
        if (age.compareTo(LEVEL_1_AFTER) >= 0) return LEVEL_1;
        return NONE;
    }
}
