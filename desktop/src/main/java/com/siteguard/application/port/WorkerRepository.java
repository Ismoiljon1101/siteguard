package com.siteguard.application.port;

import com.siteguard.domain.Worker;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WorkerRepository {
    Worker create(String displayName, String trade, Instant at);
    Optional<Worker> find(long workerId);
    Optional<Worker> findByName(String displayName);
    List<Worker> list();
}
