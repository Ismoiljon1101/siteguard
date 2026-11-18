package com.siteguard.application.port;

import com.siteguard.domain.HealthChange;
import java.util.List;
import java.util.Optional;

public interface HealthRepository {
    void record(HealthChange change);
    Optional<HealthChange> latest(String deviceId);
    /** Newest first. */
    List<HealthChange> history(String deviceId, int limit);
}
