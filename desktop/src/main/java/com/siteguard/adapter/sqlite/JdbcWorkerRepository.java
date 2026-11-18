package com.siteguard.adapter.sqlite;

import com.siteguard.application.port.WorkerRepository;
import com.siteguard.domain.Worker;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

final class JdbcWorkerRepository implements WorkerRepository {
    private final Connection c;

    JdbcWorkerRepository(Connection c) { this.c = c; }

    private static final Jdbc.Mapper<Worker> WORKER = rs ->
            new Worker(rs.getLong(1), rs.getString(2), rs.getString(3), Jdbc.instant(rs.getString(4)));

    @Override public Worker create(String displayName, String trade, Instant at) {
        long id = Jdbc.insert(c, "INSERT INTO workers(display_name, trade, created_at) VALUES(?,?,?)", displayName, trade, at);
        return new Worker(id, displayName, trade, at);
    }

    @Override public Optional<Worker> find(long workerId) {
        return Jdbc.one(c, "SELECT worker_id, display_name, trade, created_at FROM workers WHERE worker_id = ?", WORKER, workerId);
    }

    @Override public Optional<Worker> findByName(String displayName) {
        return Jdbc.one(c, "SELECT worker_id, display_name, trade, created_at FROM workers WHERE display_name = ? ORDER BY worker_id", WORKER, displayName);
    }

    @Override public List<Worker> list() {
        return Jdbc.list(c, "SELECT worker_id, display_name, trade, created_at FROM workers ORDER BY display_name", WORKER);
    }
}
