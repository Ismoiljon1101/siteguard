package com.siteguard.adapter.sqlite;

import com.siteguard.application.port.CredentialRepository;
import com.siteguard.application.port.OperatorRepository;
import com.siteguard.application.port.StoredCredential;
import com.siteguard.domain.Operator;
import com.siteguard.domain.Role;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Operators and their credentials. Only hashes are stored. */
final class JdbcOperatorRepository implements OperatorRepository, CredentialRepository {
    private final Connection c;

    JdbcOperatorRepository(Connection c) { this.c = c; }

    private static final String COLS = "operator_id, login_id, display_name, role";
    private static final Jdbc.Mapper<Operator> OPERATOR = rs ->
            new Operator(rs.getLong(1), rs.getString(2), rs.getString(3), Role.valueOf(rs.getString(4)));

    @Override public Operator create(String loginId, String displayName, Role role, Instant at) {
        long id = Jdbc.insert(c, "INSERT INTO operators(login_id, display_name, role, created_at) VALUES(?,?,?,?)",
                loginId, displayName, role, at);
        return new Operator(id, loginId, displayName, role);
    }

    @Override public Optional<Operator> find(long operatorId) {
        return Jdbc.one(c, "SELECT " + COLS + " FROM operators WHERE operator_id = ?", OPERATOR, operatorId);
    }

    @Override public Optional<Operator> findByLogin(String loginId) {
        return Jdbc.one(c, "SELECT " + COLS + " FROM operators WHERE login_id = ?", OPERATOR, loginId);
    }

    @Override public List<Operator> list() {
        return Jdbc.list(c, "SELECT " + COLS + " FROM operators ORDER BY operator_id", OPERATOR);
    }

    @Override public long count() {
        return Jdbc.one(c, "SELECT COUNT(*) FROM operators", rs -> rs.getLong(1)).orElse(0L);
    }

    @Override public void grantSite(long operatorId, long siteId) {
        Jdbc.update(c, "INSERT OR IGNORE INTO operator_sites(operator_id, site_id) VALUES(?,?)", operatorId, siteId);
    }

    @Override public Set<Long> siteIds(long operatorId) {
        return new HashSet<>(Jdbc.list(c, "SELECT site_id FROM operator_sites WHERE operator_id = ?", rs -> rs.getLong(1), operatorId));
    }

    @Override public Optional<StoredCredential> find(String loginId) {
        return Jdbc.one(c, "SELECT operator_id, login_id, display_name, role, password_hash, salt, iterations, failed_attempts, locked_until "
                + "FROM operators WHERE login_id = ?",
                rs -> new StoredCredential(
                        new Operator(rs.getLong(1), rs.getString(2), rs.getString(3), Role.valueOf(rs.getString(4))),
                        rs.getString(5), rs.getString(6), rs.getInt(7), rs.getInt(8), Jdbc.instant(rs.getString(9))),
                loginId);
    }

    @Override public void setPassword(long operatorId, String hash, String salt, int iterations, Instant createdAt) {
        Jdbc.update(c, "UPDATE operators SET password_hash = ?, salt = ?, iterations = ?, created_at = ?, "
                + "failed_attempts = 0, locked_until = NULL WHERE operator_id = ?", hash, salt, iterations, createdAt, operatorId);
    }

    @Override public void recordFailure(long operatorId, int failedAttempts) {
        Jdbc.update(c, "UPDATE operators SET failed_attempts = ? WHERE operator_id = ?", failedAttempts, operatorId);
    }

    @Override public void lock(long operatorId, Instant until) {
        Jdbc.update(c, "UPDATE operators SET failed_attempts = 0, locked_until = ? WHERE operator_id = ?", until, operatorId);
    }

    @Override public void clearFailures(long operatorId) {
        Jdbc.update(c, "UPDATE operators SET failed_attempts = 0, locked_until = NULL WHERE operator_id = ?", operatorId);
    }
}
