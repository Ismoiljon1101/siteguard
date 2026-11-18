package com.siteguard.adapter.sqlite;

import com.siteguard.application.port.AssignmentRepository;
import com.siteguard.application.port.ConfigRepository;
import com.siteguard.application.port.CredentialRepository;
import com.siteguard.application.port.DeviceRepository;
import com.siteguard.application.port.HealthRepository;
import com.siteguard.application.port.IncidentRepository;
import com.siteguard.application.port.OperatorRepository;
import com.siteguard.application.port.ReadingRepository;
import com.siteguard.application.port.Repositories;
import com.siteguard.application.port.SiteRepository;
import com.siteguard.application.port.WorkerRepository;
import java.sql.Connection;

/** Repositories bound to a single connection. */
final class JdbcRepositories implements Repositories {
    private final JdbcSiteRepository sites;
    private final JdbcOperatorRepository operators;
    private final JdbcWorkerRepository workers;
    private final JdbcDeviceRepository devices;
    private final JdbcAssignmentRepository assignments;
    private final JdbcReadingRepository readings;
    private final JdbcIncidentRepository incidents;
    private final JdbcHealthRepository health;
    private final JdbcConfigRepository config;

    JdbcRepositories(Connection c) {
        sites = new JdbcSiteRepository(c);
        operators = new JdbcOperatorRepository(c);
        workers = new JdbcWorkerRepository(c);
        devices = new JdbcDeviceRepository(c);
        assignments = new JdbcAssignmentRepository(c);
        readings = new JdbcReadingRepository(c);
        incidents = new JdbcIncidentRepository(c);
        health = new JdbcHealthRepository(c);
        config = new JdbcConfigRepository(c);
    }

    @Override public SiteRepository sites() { return sites; }
    @Override public OperatorRepository operators() { return operators; }
    @Override public CredentialRepository credentials() { return operators; }
    @Override public WorkerRepository workers() { return workers; }
    @Override public DeviceRepository devices() { return devices; }
    @Override public AssignmentRepository assignments() { return assignments; }
    @Override public ReadingRepository readings() { return readings; }
    @Override public IncidentRepository incidents() { return incidents; }
    @Override public HealthRepository health() { return health; }
    @Override public ConfigRepository config() { return config; }
}
