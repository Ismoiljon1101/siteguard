package com.siteguard.application.port;

import com.siteguard.domain.Operator;
import java.time.Instant;

/** Credential row for one operator. hash and salt are Base64; both null when no password is set. */
public record StoredCredential(Operator operator, String hash, String salt, int iterations,
                               int failedAttempts, Instant lockedUntil) { }
