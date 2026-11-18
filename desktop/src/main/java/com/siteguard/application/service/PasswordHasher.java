package com.siteguard.application.service;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * PBKDF2WithHmacSHA256 from the JDK, random 16-byte salt, constant-time compare.
 * Iteration count: OWASP Password Storage Cheat Sheet, "PBKDF2-HMAC-SHA256: 600,000 iterations"
 * https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html (read 2026-11-18).
 * The count used is stored per row, so it can be raised later without invalidating old hashes.
 */
public final class PasswordHasher {
    public static final int OWASP_ITERATIONS = 600_000;
    public static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    private final int iterations;
    private final SecureRandom random;

    public PasswordHasher() { this(OWASP_ITERATIONS, new SecureRandom()); }

    /** Lower counts are for tests only. */
    public PasswordHasher(int iterations, SecureRandom random) {
        if (iterations < 1) throw new IllegalArgumentException("Iterations must be positive");
        this.iterations = iterations;
        this.random = random;
    }

    public int iterations() { return iterations; }

    public byte[] newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return salt;
    }

    public byte[] derive(char[] password, byte[] salt, int iterationCount) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterationCount, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }

    public static String encode(byte[] bytes) { return Base64.getEncoder().encodeToString(bytes); }

    /** Constant-time comparison of the derived key with the stored hash. */
    public boolean verify(char[] password, String saltBase64, int iterationCount, String hashBase64) {
        byte[] expected = Base64.getDecoder().decode(hashBase64);
        byte[] actual = derive(password, Base64.getDecoder().decode(saltBase64), iterationCount);
        try {
            return MessageDigest.isEqual(expected, actual);
        } finally {
            Arrays.fill(actual, (byte) 0);
        }
    }

    /** Burn the same time as a real check, for unknown or locked accounts. */
    public void burn(char[] password) {
        Arrays.fill(derive(password, newSalt(), iterations), (byte) 0);
    }
}
