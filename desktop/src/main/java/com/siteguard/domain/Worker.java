package com.siteguard.domain;

import java.time.Instant;

public record Worker(long workerId, String displayName, String trade, Instant createdAt) { }
