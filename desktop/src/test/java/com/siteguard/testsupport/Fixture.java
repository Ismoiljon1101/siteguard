package com.siteguard.testsupport;

import com.siteguard.adapter.sqlite.SqliteDatabase;
import com.siteguard.adapter.sqlite.SqliteStore;
import com.siteguard.application.service.AssignmentService;
import com.siteguard.application.service.AuthService;
import com.siteguard.application.service.HealthService;
import com.siteguard.application.service.IncidentService;
import com.siteguard.application.service.IngestResult;
import com.siteguard.application.service.IngestService;
import com.siteguard.application.service.PasswordHasher;
import com.siteguard.application.service.SiteQueryService;
import com.siteguard.domain.Acceleration;
import com.siteguard.domain.Role;
import com.siteguard.domain.TelemetryReading;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

/** A small real-SQLite world: Test Site (zones Z1, Z2, helmets H1..H3, workers W1..W3) and Other Site. */
public final class Fixture {
    public static final Instant START = Instant.parse("2026-01-01T08:00:00Z");

    public final MutableClock clock;
    private final Path dbFile;
    public final SqliteDatabase database;
    public final SqliteStore store;
    public final IngestService ingest;
    public final IncidentService incidents;
    public final AssignmentService assignments;
    public final HealthService health;
    public final SiteQueryService queries;
    public final AuthService auth;
    public final PasswordHasher fastHasher = new PasswordHasher(1000, new SecureRandom());

    public long siteId, otherSiteId, zone1, zone2, otherZone;
    public long supervisorId, managerId, outsiderId, directorId;
    public long worker1, worker2, worker3;

    private Fixture(Path dbFile, MutableClock clock) {
        this.dbFile = dbFile;
        this.clock = clock;
        database = new SqliteDatabase(dbFile);
        store = SqliteStore.open(database);
        ingest = new IngestService(store, clock);
        incidents = new IncidentService(store, clock);
        assignments = new AssignmentService(store, clock);
        health = new HealthService(store, clock);
        queries = new SiteQueryService(store, clock);
        auth = new AuthService(store, clock, fastHasher);
    }

    public static Fixture create(Path dir) {
        Fixture f = new Fixture(dir.resolve("test.db"), new MutableClock(START));
        f.populate();
        return f;
    }

    /** Re-open the same file as a restarted process would: new services, same data and clock, ids carried over. */
    public Fixture restart() {
        Fixture f = new Fixture(dbFile, clock);
        f.siteId = siteId; f.otherSiteId = otherSiteId; f.zone1 = zone1; f.zone2 = zone2; f.otherZone = otherZone;
        f.supervisorId = supervisorId; f.managerId = managerId; f.outsiderId = outsiderId; f.directorId = directorId;
        f.worker1 = worker1; f.worker2 = worker2; f.worker3 = worker3;
        return f;
    }

    private void populate() {
        store.inTransaction(r -> {
            var site = r.sites().createSite("Test Site", clock.instant());
            var other = r.sites().createSite("Other Site", clock.instant());
            siteId = site.siteId();
            otherSiteId = other.siteId();
            zone1 = r.sites().createZone(siteId, "Z1", 0).zoneId();
            zone2 = r.sites().createZone(siteId, "Z2", 1).zoneId();
            otherZone = r.sites().createZone(otherSiteId, "Elsewhere", 0).zoneId();
            var sup = r.operators().create("sup", "Test Supervisor", Role.SUPERVISOR, clock.instant());
            var mgr = r.operators().create("mgr", "Test Manager", Role.SITE_MANAGER, clock.instant());
            var out = r.operators().create("out", "Outsider", Role.SITE_MANAGER, clock.instant());
            var dir = r.operators().create("dir", "Test Director", Role.SAFETY_DIRECTOR, clock.instant());
            supervisorId = sup.operatorId();
            managerId = mgr.operatorId();
            outsiderId = out.operatorId();
            directorId = dir.operatorId();
            r.operators().grantSite(supervisorId, siteId);
            r.operators().grantSite(managerId, siteId);
            r.operators().grantSite(outsiderId, otherSiteId);
            for (int i = 1; i <= 3; i++) r.devices().register("H" + i, siteId, "H" + i, true, clock.instant());
            r.devices().register("OTHER-1", otherSiteId, "OTHER-1", true, clock.instant());
            worker1 = r.workers().create("W1", "Electrician", clock.instant()).workerId();
            worker2 = r.workers().create("W2", "Plumber", clock.instant()).workerId();
            worker3 = r.workers().create("W3", "Welder", clock.instant()).workerId();
            return null;
        });
    }

    public TelemetryReading reading(String device, String boot, long seq, Double temp) {
        Double humidity = temp == null ? null : 50.0;
        return new TelemetryReading(device, boot, seq, seq * 1000, true, temp, humidity, new Acceleration(0.1, 0.0, 9.8));
    }

    /** Advance one second, then ingest a reading in boot b1. */
    public IngestResult feed(String device, long seq, Double temp) {
        clock.advance(Duration.ofSeconds(1));
        return ingest.ingest(reading(device, "b1", seq, temp));
    }
}
