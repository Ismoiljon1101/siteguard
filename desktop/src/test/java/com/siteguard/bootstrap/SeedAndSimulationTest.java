package com.siteguard.bootstrap;

import static org.junit.jupiter.api.Assertions.*;

import com.siteguard.adapter.simulation.FleetSimulator;
import com.siteguard.adapter.simulation.HelmetScenario;
import com.siteguard.application.port.IncidentFilter;
import com.siteguard.application.port.IncidentView;
import com.siteguard.application.service.DemoSeedService;
import com.siteguard.application.service.SiteQueryService;
import com.siteguard.application.service.PasswordHasher;
import com.siteguard.domain.EventType;
import com.siteguard.domain.HealthState;
import com.siteguard.domain.IncidentType;
import com.siteguard.testsupport.Fixture;
import com.siteguard.testsupport.MutableClock;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SeedAndSimulationTest {
    @TempDir Path dir;

    private final PasswordHasher fast = new PasswordHasher(1000, new SecureRandom());

    private AppContext open(MutableClock clock, ByteArrayOutputStream console) {
        return AppContext.open(dir, clock, fast, new PrintStream(console));
    }

    @Test void firstRunSeedsEverythingPrintsCredentialsOnceAndRestartIsIdempotent() throws Exception {
        MutableClock clock = new MutableClock(Fixture.START);
        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        long siteId;
        try (AppContext first = open(clock, out1)) {
            assertEquals(3, first.firstRunCredentials().size());
            siteId = first.demoSiteId();
            String printed = out1.toString();
            for (var c : first.firstRunCredentials()) assertTrue(printed.contains(c.password()), "printed once on console");
            String file = Files.readString(dir.resolve(AppContext.CREDENTIALS_FILE));
            for (var c : first.firstRunCredentials()) assertTrue(file.contains(c.loginId() + "  " + c.role() + "  " + c.password()));
            long operators = first.queries().operators().size();
            assertEquals(3, operators);
            assertEquals(13, first.queries().helmets(first.queries().operators().get(0).operatorId(), siteId).size());
            assertEquals(6, first.queries().zones(siteId).size());
        }
        String fileBefore = Files.readString(dir.resolve(AppContext.CREDENTIALS_FILE));
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        try (AppContext second = open(clock, out2)) {
            assertTrue(second.firstRunCredentials().isEmpty(), "no new credentials on restart");
            assertEquals("", out2.toString(), "nothing printed on restart");
            assertEquals(siteId, second.demoSiteId());
            assertEquals(3, second.queries().operators().size());
            assertEquals(1, second.queries().sites(second.queries().operators().get(0).operatorId()).size());
            assertEquals(13, second.queries().helmets(second.queries().operators().get(0).operatorId(), siteId).size());
            assertEquals(12, second.queries().workers().size());
        }
        assertEquals(fileBefore, Files.readString(dir.resolve(AppContext.CREDENTIALS_FILE)), "credentials file untouched");
    }

    @Test void siteScopingOfSeedOperators() {
        MutableClock clock = new MutableClock(Fixture.START);
        try (AppContext ctx = open(clock, new ByteArrayOutputStream())) {
            var ops = ctx.queries().operators();
            long director = ops.stream().filter(o -> o.loginId().equals("director")).findFirst().orElseThrow().operatorId();
            long supervisor = ops.stream().filter(o -> o.loginId().equals("supervisor")).findFirst().orElseThrow().operatorId();
            assertEquals(1, ctx.queries().sites(director).size());
            assertEquals(1, ctx.queries().sites(supervisor).size());
            assertEquals("Demo Site A", ctx.queries().sites(supervisor).get(0).name());
        }
    }

    /** Runs the scripted fleet through the real ingest path, one simulated second per tick. */
    private void run(AppContext ctx, MutableClock clock, int ticks) {
        for (int i = 0; i < ticks; i++) {
            ctx.simulation().tick();
            clock.advance(Duration.ofSeconds(1));
        }
    }

    @Test void scriptedScenariosProduceTheExpectedIncidentsAndHealth() {
        MutableClock clock = new MutableClock(Fixture.START);
        try (AppContext ctx = open(clock, new ByteArrayOutputStream())) {
            long supervisor = ctx.queries().operators().stream().filter(o -> o.loginId().equals("supervisor")).findFirst().orElseThrow().operatorId();
            run(ctx, clock, 9);
            var rows = ctx.queries().workers(supervisor, new SiteQueryService.WorkerQuery(ctx.demoSiteId(), null, false, null, null, null,
                    SiteQueryService.WorkerSort.NAME, 0, 100)).rows();
            Map<String, SiteQueryService.WorkerRow> byDevice = rows.stream().collect(Collectors.toMap(SiteQueryService.WorkerRow::deviceId, r -> r));
            assertEquals(12, rows.size());
            assertEquals(HealthState.ONLINE, byDevice.get("SIM-001").health());
            assertNotNull(byDevice.get("SIM-001").airTempC());
            assertEquals(HealthState.SENSOR_FAULT, byDevice.get("SIM-010").health());
            assertNull(byDevice.get("SIM-010").airTempC());
            assertEquals(HealthState.STALE, byDevice.get("SIM-011").health(), "stopped telemetry goes stale");
            assertNull(byDevice.get("SIM-011").airTempC());
            assertEquals(IncidentType.ENVIRONMENT, byDevice.get("SIM-007").incidentType());
            assertEquals(IncidentType.SOS, byDevice.get("SIM-008").incidentType());
            assertEquals(IncidentType.MOTION, byDevice.get("SIM-009").incidentType());
            assertNull(byDevice.get("SIM-001").incidentIdentity());

            List<IncidentView> all = ctx.incidents().list(supervisor, IncidentFilter.openForSite(ctx.demoSiteId())).stream().map(x -> x.view()).toList();
            assertEquals(3, all.size());
            assertTrue(all.stream().allMatch(v -> v.incident().sourceMode().equals("SIMULATED")));

            run(ctx, clock, 40);
            var later = ctx.queries().workers(supervisor, SiteQueryService.WorkerQuery.firstPage(ctx.demoSiteId())).rows();
            assertEquals(HealthState.OFFLINE, later.stream().filter(r -> r.deviceId().equals("SIM-011")).findFirst().orElseThrow().health());
            assertEquals(3, ctx.incidents().list(supervisor, IncidentFilter.openForSite(ctx.demoSiteId())).size(), "health never creates incidents; no spam");
        }
    }

    @Test void statusCountsAddUpWithTwelveHelmetsAndNeedsAttentionSortsFirst() {
        MutableClock clock = new MutableClock(Fixture.START);
        try (AppContext ctx = open(clock, new ByteArrayOutputStream())) {
            long supervisor = ctx.queries().operators().stream().filter(o -> o.loginId().equals("supervisor")).findFirst().orElseThrow().operatorId();
            run(ctx, clock, 12);
            var overview = ctx.queries().overview(supervisor, ctx.demoSiteId());
            assertEquals(12, overview.workersOnShift());
            assertEquals(12, overview.statusCounts().values().stream().mapToInt(Integer::intValue).sum());
            assertEquals(12, overview.zones().stream().mapToInt(SiteQueryService.ZoneCount::workers).sum());
            assertEquals(1, overview.unassignedHelmets());
            assertEquals(13, overview.simulatedHelmets());
            assertEquals(0, overview.liveHelmets());
            assertEquals(1, overview.openSos());
            assertEquals(2, overview.openWarnings());
            assertEquals(1, overview.statusCounts().get(HealthState.SENSOR_FAULT));
            assertEquals(1, overview.statusCounts().get(HealthState.STALE) + overview.statusCounts().get(HealthState.OFFLINE));
            assertTrue(overview.zones().stream().anyMatch(z -> z.zoneId() == null && z.workers() == 1), "one worker has no zone");
            assertEquals(7, overview.zones().size());

            var page = ctx.queries().workers(supervisor, new SiteQueryService.WorkerQuery(ctx.demoSiteId(), null, false, null, null, null,
                    SiteQueryService.WorkerSort.NEEDS_ATTENTION, 0, 5));
            assertEquals(12, page.total());
            assertEquals(5, page.rows().size());
            assertEquals(IncidentType.SOS, page.rows().get(0).incidentType(), "SOS first");
            assertTrue(Set.of(HealthState.STALE, HealthState.SENSOR_FAULT, HealthState.OFFLINE).contains(page.rows().get(1).health()));
            assertTrue(Set.of(HealthState.STALE, HealthState.SENSOR_FAULT, HealthState.OFFLINE).contains(page.rows().get(2).health()));
            var second = ctx.queries().workers(supervisor, new SiteQueryService.WorkerQuery(ctx.demoSiteId(), null, false, null, null, null,
                    SiteQueryService.WorkerSort.NEEDS_ATTENTION, 1, 5));
            assertEquals(5, second.rows().size());

            var electricians = ctx.queries().workers(supervisor, new SiteQueryService.WorkerQuery(ctx.demoSiteId(), null, false, "Electrician",
                    null, null, SiteQueryService.WorkerSort.NAME, 0, 25));
            assertEquals(2, electricians.total());
            var searched = ctx.queries().workers(supervisor, new SiteQueryService.WorkerQuery(ctx.demoSiteId(), null, false, null, "worker c",
                    null, SiteQueryService.WorkerSort.NAME, 0, 25));
            assertEquals(1, searched.total());
            var noZone = ctx.queries().workers(supervisor, new SiteQueryService.WorkerQuery(ctx.demoSiteId(), null, true, null, null, null,
                    SiteQueryService.WorkerSort.NAME, 0, 25));
            assertEquals(1, noZone.total());
        }
    }

    @Test void fleetSimulatorIsDeterministicAndStoppedHelmetsGoSilent() {
        List<String> ids = List.of("SIM-001", "SIM-008", "SIM-011");
        FleetSimulator a = new FleetSimulator(ids, "r1");
        FleetSimulator b = new FleetSimulator(ids, "r1");
        for (int i = 0; i < 12; i++) assertEquals(a.tick(), b.tick());
        FleetSimulator c = new FleetSimulator(ids, "r1");
        int sos = 0;
        int frames011 = 0;
        for (int i = 0; i < 12; i++) {
            for (var f : c.tick()) {
                if (f.deviceId().equals("SIM-011")) frames011++;
                sos += (int) f.events().stream().filter(e -> e.type() == EventType.SOS).count();
                assertTrue(f.reading().simulated());
            }
        }
        assertEquals(3, frames011);
        assertEquals(1, sos);
        assertEquals(HelmetScenario.STOPPED, c.scenarioOf("SIM-011"));
        c.reset("r2");
        assertEquals("sim-r2", c.tick().get(0).reading().bootId());
    }
}
