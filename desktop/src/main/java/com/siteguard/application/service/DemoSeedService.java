package com.siteguard.application.service;

import com.siteguard.application.port.Store;
import com.siteguard.domain.Operator;
import com.siteguard.domain.Site;
import com.siteguard.domain.Zone;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Fictional demo site: 'Demo Site A', 6 zones, 13 simulated helmets SIM-001..SIM-013, workers A..L with trades,
 * active assignments with operator-selected zones, and SIM-013 left UNASSIGNED. All rows are simulated.
 * Idempotent: it creates nothing when the site exists, and only re-links operators to the site.
 */
public final class DemoSeedService {
    public static final String SITE_NAME = "Demo Site A";
    public static final List<String> ZONES = List.of("Ground", "Level 1 West", "Level 1 East", "Level 2 East", "Level 2 West", "Roof");
    public static final List<String> TRADES = List.of("Electrician", "Plumber", "Carpenter", "Welder", "Scaffolder", "Crane operator",
            "Electrician", "Bricklayer", "Painter", "Steel fixer", "Labourer", "Safety officer");
    public static final int HELMETS = 13;

    private final Store store;
    private final Clock clock;

    public DemoSeedService(Store store, Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.clock = Objects.requireNonNull(clock);
    }

    public static String deviceId(int index) { return String.format("SIM-%03d", index); }

    /** @return true when the site was created now. */
    public boolean seedIfAbsent() {
        Instant now = clock.instant();
        return store.inTransaction(r -> {
            Optional<Site> existing = r.sites().findSiteByName(SITE_NAME);
            boolean created = existing.isEmpty();
            Site site = existing.orElseGet(() -> r.sites().createSite(SITE_NAME, now));
            Operator supervisor = r.operators().findByLogin("supervisor").orElse(null);
            for (Operator op : r.operators().list()) {
                if (!op.role().seesAllSites()) r.operators().grantSite(op.operatorId(), site.siteId());
            }
            if (!created) return false;

            List<Zone> zones = new ArrayList<>();
            for (int i = 0; i < ZONES.size(); i++) zones.add(r.sites().createZone(site.siteId(), ZONES.get(i), i));
            for (int i = 1; i <= HELMETS; i++) {
                r.devices().register(deviceId(i), site.siteId(), deviceId(i), true, now);
            }
            Long by = supervisor == null ? null : supervisor.operatorId();
            for (int i = 1; i <= TRADES.size(); i++) {
                String name = "Worker " + (char) ('A' + i - 1);
                var worker = r.workers().create(name, TRADES.get(i - 1), now);
                var assignment = r.assignments().start(deviceId(i), worker.workerId(), by, now);
                // The last worker has no zone yet, so the "No zone" bucket is exercised.
                if (i < TRADES.size()) {
                    r.assignments().setZone(assignment.assignmentId(), zones.get((i - 1) % zones.size()).zoneId(), by, now);
                }
            }
            return true;
        });
    }
}
