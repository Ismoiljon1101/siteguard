package com.siteguard.application.service;

import static org.junit.jupiter.api.Assertions.*;

import com.siteguard.domain.Operator;
import com.siteguard.domain.Role;
import com.siteguard.testsupport.Fixture;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuthServiceTest {
    @TempDir Path dir;
    Fixture f;

    @BeforeEach void setUp() { f = Fixture.create(dir); }

    @Test void seedCreatesThreeOperatorsWithStrongRandomPasswordsAndIsIdempotent() {
        var store = freshStore();
        var seeder = new OperatorSeedService(store, f.clock, f.fastHasher, new SecureRandom());
        List<GeneratedCredential> first = seeder.seedIfEmpty();
        assertEquals(List.of("director", "manager", "supervisor"), first.stream().map(GeneratedCredential::loginId).toList());
        assertEquals(List.of(Role.SAFETY_DIRECTOR, Role.SITE_MANAGER, Role.SUPERVISOR), first.stream().map(GeneratedCredential::role).toList());
        for (GeneratedCredential c : first) {
            assertEquals(16, c.password().length());
            assertFalse(c.password().matches(".*[0O1lI].*"), "no ambiguous characters");
        }
        assertEquals(3, first.stream().map(GeneratedCredential::password).distinct().count());
        assertTrue(seeder.seedIfEmpty().isEmpty(), "does nothing when operators exist");
        assertEquals(3L, store.read(r -> r.operators().count()).longValue());
    }

    @Test void loginSucceedsWithRightPasswordAndFailsGenericallyOtherwise() {
        var store = freshStore();
        var seeder = new OperatorSeedService(store, f.clock, f.fastHasher, new SecureRandom());
        GeneratedCredential dir0 = seeder.seedIfEmpty().get(0);
        var auth = new AuthService(store, f.clock, f.fastHasher);
        Operator ok = auth.login("director", dir0.password().toCharArray());
        assertEquals(Role.SAFETY_DIRECTOR, ok.role());

        AuthenticationException wrong = assertThrows(AuthenticationException.class, () -> auth.login("director", "wrong-password".toCharArray()));
        AuthenticationException unknown = assertThrows(AuthenticationException.class, () -> auth.login("nobody", "wrong-password".toCharArray()));
        assertEquals(wrong.getMessage(), unknown.getMessage(), "no user enumeration");
        assertEquals(AuthenticationException.MESSAGE, wrong.getMessage());
        assertThrows(AuthenticationException.class, () -> auth.login(null, "x".toCharArray()));
    }

    @Test void onlyHashAndSaltAreStoredNeverThePassword() throws Exception {
        var db = new com.siteguard.adapter.sqlite.SqliteDatabase(dir.resolve("hash.db"));
        var store = com.siteguard.adapter.sqlite.SqliteStore.open(db);
        List<GeneratedCredential> creds = new OperatorSeedService(store, f.clock, f.fastHasher, new SecureRandom()).seedIfEmpty();
        List<String> cells = new ArrayList<>();
        try (var c = db.open(true); var s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT * FROM operators")) {
            int cols = rs.getMetaData().getColumnCount();
            while (rs.next()) for (int i = 1; i <= cols; i++) cells.add(String.valueOf(rs.getString(i)));
        }
        for (GeneratedCredential cred : creds) {
            for (String cell : cells) assertFalse(cell.contains(cred.password()), "plaintext found in database");
        }
        try (var c = db.open(true); var s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT password_hash, salt, iterations FROM operators WHERE login_id = 'manager'")) {
            assertTrue(rs.next());
            assertEquals(32, java.util.Base64.getDecoder().decode(rs.getString(1)).length, "256-bit key");
            assertEquals(16, java.util.Base64.getDecoder().decode(rs.getString(2)).length, "16-byte salt");
            assertEquals(1000, rs.getInt(3));
        }
    }

    @Test void saltsDifferPerOperator() throws Exception {
        var store = freshStore();
        new OperatorSeedService(store, f.clock, f.fastHasher, new SecureRandom()).seedIfEmpty();
        var salts = store.read(r -> List.of(r.credentials().find("director").orElseThrow().salt(),
                r.credentials().find("manager").orElseThrow().salt(), r.credentials().find("supervisor").orElseThrow().salt()));
        assertEquals(3, salts.stream().distinct().count());
    }

    @Test void fiveFailuresLockForSixtySecondsThenAllowAgain() {
        var store = freshStore();
        GeneratedCredential c = new OperatorSeedService(store, f.clock, f.fastHasher, new SecureRandom()).seedIfEmpty().get(2);
        var auth = new AuthService(store, f.clock, f.fastHasher);
        for (int i = 0; i < 4; i++) assertThrows(AuthenticationException.class, () -> auth.login("supervisor", "bad".toCharArray()));
        // Fifth failure locks. The correct password is rejected while locked.
        assertThrows(AuthenticationException.class, () -> auth.login("supervisor", "bad".toCharArray()));
        assertThrows(AuthenticationException.class, () -> auth.login("supervisor", c.password().toCharArray()));
        f.clock.advance(Duration.ofMillis(59999));
        assertThrows(AuthenticationException.class, () -> auth.login("supervisor", c.password().toCharArray()));
        f.clock.advance(Duration.ofMillis(1));
        assertEquals("supervisor", auth.login("supervisor", c.password().toCharArray()).loginId());
    }

    @Test void successResetsFailureCount() {
        var store = freshStore();
        GeneratedCredential c = new OperatorSeedService(store, f.clock, f.fastHasher, new SecureRandom()).seedIfEmpty().get(1);
        var auth = new AuthService(store, f.clock, f.fastHasher);
        for (int i = 0; i < 4; i++) assertThrows(AuthenticationException.class, () -> auth.login("manager", "bad".toCharArray()));
        auth.login("manager", c.password().toCharArray());
        for (int i = 0; i < 4; i++) assertThrows(AuthenticationException.class, () -> auth.login("manager", "bad".toCharArray()));
        assertEquals("manager", auth.login("manager", c.password().toCharArray()).loginId(), "4 + success + 4 never reaches 5 in a row");
    }

    @Test void owaspIterationCountIsTheDefault() {
        assertEquals(600_000, PasswordHasher.OWASP_ITERATIONS);
        assertEquals(600_000, new PasswordHasher().iterations());
    }

    @Test void hashIsDeterministicForSameSaltAndDiffersForDifferentPassword() {
        PasswordHasher h = new PasswordHasher(1000, new SecureRandom());
        byte[] salt = h.newSalt();
        assertArrayEquals(h.derive("abc".toCharArray(), salt, 1000), h.derive("abc".toCharArray(), salt, 1000));
        assertFalse(java.util.Arrays.equals(h.derive("abc".toCharArray(), salt, 1000), h.derive("abd".toCharArray(), salt, 1000)));
    }

    private com.siteguard.application.port.Store freshStore() {
        var db = new com.siteguard.adapter.sqlite.SqliteDatabase(dir.resolve("seed-" + System.nanoTime() + ".db"));
        return com.siteguard.adapter.sqlite.SqliteStore.open(db);
    }
}
