package com.siteguard.domain;

import java.time.Instant;

/** Rule thresholds. 35.0 C and 3 samples are placeholders (D9); the motion threshold is null until measured (D10). */
public record ConfigVersion(long configVersion, double tempWarnC, int tempConsecutiveSamples,
                            Double motionThresholdMps2, Long createdBy, Instant createdAt) {
    public ConfigVersion {
        if (tempConsecutiveSamples < 1) throw new IllegalArgumentException("Samples must be at least 1");
    }
}
