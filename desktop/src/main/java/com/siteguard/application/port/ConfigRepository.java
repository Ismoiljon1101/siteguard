package com.siteguard.application.port;

import com.siteguard.domain.ConfigVersion;
import java.time.Instant;

public interface ConfigRepository {
    /** Highest version. Migration v3 seeds version 1 with the placeholders. */
    ConfigVersion current();
    ConfigVersion create(double tempWarnC, int samples, Double motionThreshold, Long createdBy, Instant at);
}
