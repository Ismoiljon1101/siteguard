package com.siteguard.domain;

import java.time.Instant;

public record HealthChange(long id, String deviceId, HealthState state, Instant changedAt, SourceMode sourceMode) { }
