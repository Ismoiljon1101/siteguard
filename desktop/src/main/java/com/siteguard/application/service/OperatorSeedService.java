package com.siteguard.application.service;

import com.siteguard.application.port.Store;
import com.siteguard.domain.Operator;
import com.siteguard.domain.Role;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * First-run seed: three demo operators with random 16-character passwords (no ambiguous characters).
 * Does nothing when any operator exists. Returns the plaintext once so the caller can print it and write the
 * ignored credentials file; only hashes are stored.
 */
public final class OperatorSeedService {
    /** No 0 O 1 l I. */
    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    static final int PASSWORD_LENGTH = 16;

    private final Store store;
    private final Clock clock;
    private final PasswordHasher hasher;
    private final SecureRandom random;

    public OperatorSeedService(Store store, Clock clock, PasswordHasher hasher, SecureRandom random) {
        this.store = Objects.requireNonNull(store);
        this.clock = Objects.requireNonNull(clock);
        this.hasher = Objects.requireNonNull(hasher);
        this.random = Objects.requireNonNull(random);
    }

    public List<GeneratedCredential> seedIfEmpty() {
        Instant now = clock.instant();
        return store.inTransaction(r -> {
            if (r.operators().count() > 0) return List.of();
            List<GeneratedCredential> out = new ArrayList<>();
            String[][] demo = {
                {"director", "Demo Safety Director", Role.SAFETY_DIRECTOR.name()},
                {"manager", "Demo Site Manager", Role.SITE_MANAGER.name()},
                {"supervisor", "Demo Supervisor", Role.SUPERVISOR.name()}};
            for (String[] d : demo) {
                Role role = Role.valueOf(d[2]);
                Operator op = r.operators().create(d[0], d[1], role, now);
                String password = randomPassword();
                byte[] salt = hasher.newSalt();
                byte[] hash = hasher.derive(password.toCharArray(), salt, hasher.iterations());
                r.credentials().setPassword(op.operatorId(), PasswordHasher.encode(hash), PasswordHasher.encode(salt),
                        hasher.iterations(), now);
                out.add(new GeneratedCredential(d[0], d[1], role, password));
            }
            return List.copyOf(out);
        });
    }

    String randomPassword() {
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return sb.toString();
    }
}
