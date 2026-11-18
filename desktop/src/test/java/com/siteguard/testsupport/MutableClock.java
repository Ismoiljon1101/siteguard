package com.siteguard.testsupport;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Deterministic clock for tests. */
public final class MutableClock extends Clock {
    private Instant now;

    public MutableClock(Instant start) { this.now = start; }

    public synchronized void advance(Duration d) { now = now.plus(d); }
    public synchronized void set(Instant t) { now = t; }

    @Override public synchronized Instant instant() { return now; }
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
}
