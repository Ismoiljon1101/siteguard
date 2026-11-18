package com.siteguard.domain;

import java.time.Instant;

public record Site(long siteId, String name, Instant createdAt) { }
