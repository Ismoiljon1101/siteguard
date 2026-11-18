package com.siteguard.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.siteguard.application.port.StorageException;
import com.siteguard.application.service.AuthenticationException;
import com.siteguard.application.service.SiteQueryService.SiteOverview;
import com.siteguard.application.service.SiteQueryService.ZoneCount;
import com.siteguard.domain.EscalationLevel;
import com.siteguard.domain.HealthState;
import com.siteguard.ui.Presentation.Count;
import com.siteguard.ui.Presentation.ZoneChoice;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PresentationTest {

    private static SiteOverview overview() {
        Map<HealthState, Integer> counts = new EnumMap<>(HealthState.class);
        counts.put(HealthState.ONLINE, 5);
        counts.put(HealthState.STALE, 2);
        counts.put(HealthState.OFFLINE, 1);
        counts.put(HealthState.SENSOR_FAULT, 1);
        counts.put(HealthState.NO_DATA, 3);
        List<ZoneCount> zones = List.of(new ZoneCount(1L, "Level 2 East", 7), new ZoneCount(null, "No zone", 5));
        return new SiteOverview(1L, "Demo Site A", 12, counts, 1, zones, 1, 2, Duration.ofSeconds(42), 0, 12);
    }

    @Test
    void healthCountsAddUpToWorkersOnShift() {
        SiteOverview o = overview();
        assertEquals(o.workersOnShift(), Presentation.healthTotal(o));
        List<Count> bar = Presentation.counts(o);
        assertEquals(8, bar.size());
        assertEquals(5, bar.get(0).value());
        assertEquals(1, bar.get(5).value());
    }

    @Test
    void zoneChoicesAddUpToWorkersOnShift() {
        SiteOverview o = overview();
        List<ZoneChoice> zones = Presentation.zoneChoices(o);
        assertTrue(zones.get(0).all());
        assertEquals(12, zones.get(0).workers());
        int sum = zones.stream().skip(1).mapToInt(ZoneChoice::workers).sum();
        assertEquals(o.workersOnShift(), sum);
        assertEquals("all", zones.get(0).key());
        assertEquals("none", zones.get(2).key());
        assertEquals("z1", zones.get(1).key());
    }

    @Test
    void missingValuesShowDashes() {
        assertEquals("--", Presentation.temperature(null));
        assertEquals("--", Presentation.humidity(null));
        assertEquals("--", Presentation.duration(null));
        assertEquals("21.5 °C", Presentation.temperature(21.5));
    }

    @Test
    void durationFormatsMinutesAndHours() {
        assertEquals("00:42", Presentation.duration(Duration.ofSeconds(42)));
        assertEquals("02:05", Presentation.duration(Duration.ofSeconds(125)));
        assertEquals("1:01:40", Presentation.duration(Duration.ofSeconds(3700)));
    }

    @Test
    void sosLineNamesWorkerZoneAndEscalation() {
        assertEquals("SOS Worker C, Level 2 East, open 00:42 (escalated L2)",
                Presentation.sosLine("Worker C", "Level 2 East", Duration.ofSeconds(42), EscalationLevel.LEVEL_2));
        assertEquals("SOS Unassigned helmet, No zone, open 00:05",
                Presentation.sosLine(null, null, Duration.ofSeconds(5), EscalationLevel.NONE));
    }

    @Test
    void errorTextHidesInternalsAndUsesStorageMessage() {
        assertEquals(AuthenticationException.MESSAGE, Presentation.errorText(new AuthenticationException()));
        assertEquals(Presentation.STORAGE_FAULT, Presentation.errorText(new StorageException("disk")));
        assertEquals("Unexpected error. Retry.", Presentation.errorText(new IllegalStateException("secret detail")));
    }
}
