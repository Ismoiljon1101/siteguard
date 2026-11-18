package com.siteguard.application.port;

/** All repositories bound to one unit of work (one connection or transaction). */
public interface Repositories {
    SiteRepository sites();
    OperatorRepository operators();
    CredentialRepository credentials();
    WorkerRepository workers();
    DeviceRepository devices();
    AssignmentRepository assignments();
    ReadingRepository readings();
    IncidentRepository incidents();
    HealthRepository health();
    ConfigRepository config();
}
