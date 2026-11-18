package com.siteguard.application.service;

import com.siteguard.application.port.Store;
import com.siteguard.application.port.StoredCredential;
import com.siteguard.domain.Operator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Operator login. Five failures lock the account for 60 s. One generic error for unknown operator, wrong
 * password and locked account, with a dummy hash so timing does not reveal which. Blocking (about 0.2 to 0.5 s per check).
 */
public final class AuthService {
    public static final int MAX_FAILURES = 5;
    public static final Duration LOCK_DURATION = Duration.ofSeconds(60);

    private final Store store;
    private final Clock clock;
    private final PasswordHasher hasher;

    public AuthService(Store store, Clock clock, PasswordHasher hasher) {
        this.store = Objects.requireNonNull(store);
        this.clock = Objects.requireNonNull(clock);
        this.hasher = Objects.requireNonNull(hasher);
    }

    /** Returns the operator or throws {@link AuthenticationException}. Clears the password array. */
    public Operator login(String loginId, char[] password) {
        Objects.requireNonNull(password, "password");
        try {
            Instant now = clock.instant();
            Optional<Operator> result = store.inTransaction(r -> attempt(r.credentials().find(loginId == null ? "" : loginId.strip()), password, now,
                    r.credentials()));
            return result.orElseThrow(AuthenticationException::new);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private Optional<Operator> attempt(Optional<StoredCredential> found, char[] password, Instant now,
                                       com.siteguard.application.port.CredentialRepository credentials) {
        if (found.isEmpty() || found.get().hash() == null) {
            hasher.burn(password);
            return Optional.empty();
        }
        StoredCredential c = found.get();
        if (c.lockedUntil() != null && now.isBefore(c.lockedUntil())) {
            hasher.burn(password);
            return Optional.empty();
        }
        long id = c.operator().operatorId();
        if (hasher.verify(password, c.salt(), c.iterations(), c.hash())) {
            if (c.failedAttempts() > 0 || c.lockedUntil() != null) credentials.clearFailures(id);
            return Optional.of(c.operator());
        }
        int failures = c.failedAttempts() + 1;
        if (failures >= MAX_FAILURES) credentials.lock(id, now.plus(LOCK_DURATION));
        else credentials.recordFailure(id, failures);
        return Optional.empty();
    }
}
