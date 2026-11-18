package com.siteguard.application.port;

import java.time.Instant;
import java.util.Optional;

public interface CredentialRepository {
    Optional<StoredCredential> find(String loginId);
    void setPassword(long operatorId, String hash, String salt, int iterations, Instant createdAt);
    /** Store the failure count. */
    void recordFailure(long operatorId, int failedAttempts);
    /** Lock until the given time and reset the failure count. */
    void lock(long operatorId, Instant until);
    void clearFailures(long operatorId);
}
