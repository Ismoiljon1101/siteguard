package com.siteguard.ui;

import com.siteguard.application.port.StorageException;
import com.siteguard.application.service.ApplicationException;
import com.siteguard.application.service.SiteQueryService.SiteOverview;
import com.siteguard.application.service.SiteQueryService.ZoneCount;
import com.siteguard.domain.EscalationLevel;
import com.siteguard.domain.HealthState;
import com.siteguard.domain.IncidentState;
import com.siteguard.domain.IncidentType;
import com.siteguard.domain.ResolutionCategory;
import com.siteguard.domain.Severity;
import com.siteguard.domain.StoredReading;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Text, glyphs and tones for the screens. No Swing here, so it is unit-tested directly. */
public final class Presentation {
    /** Colour meaning lives in Theme. Every tone also has a word or glyph in the text. */
    public enum Tone { ONLINE, STALE, OFFLINE, SENSOR_FAULT, NO_DATA, UNASSIGNED, SOS_HIGH, WARNING, ESCALATION_1, ESCALATION_2, NEUTRAL }

    /** Text with its tone. */
    public record Tagged(String text, Tone tone) { }

    /** One figure in the count bar. */
    public record Count(String label, int value, Tone tone) { }

    /** One zone list row. "All workers" has all=true; "No zone" has noZoneOnly=true. */
    public record ZoneChoice(String label, Long zoneId, boolean noZoneOnly, boolean all, int workers) {
        public String key() {
            if (all) return "all";
            if (noZoneOnly) return "none";
            return "z" + zoneId;
        }
    }

    public static final String SIM = "SIM";
    public static final String TEMP_LABEL = "Air temp near helmet";
    public static final String ZONE_LABEL = "Zone (operator-selected)";
    public static final String MISSING = "--";
    public static final String SIMULATED_BADGE = "SIMULATED DATA";
    public static final String STORAGE_FAULT = "Storage fault. Event not stored. Helmet alarm still running.";

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final List<HealthState> COUNTED = List.of(HealthState.ONLINE, HealthState.STALE,
            HealthState.OFFLINE, HealthState.SENSOR_FAULT, HealthState.NO_DATA);

    private Presentation() { }

    public static String healthName(HealthState s) {
        return switch (s) {
            case ONLINE -> "Online";
            case STALE -> "Stale";
            case OFFLINE -> "Offline";
            case SENSOR_FAULT -> "Sensor fault";
            case NO_DATA -> "No data yet";
            case UNASSIGNED -> "Unassigned";
        };
    }

    public static Tagged health(HealthState s) {
        String glyph = switch (s) {
            case ONLINE -> "● ";
            case STALE -> "◐ ";
            case OFFLINE -> "○ ";
            case SENSOR_FAULT -> "⚠ ";
            case NO_DATA -> "◌ ";
            case UNASSIGNED -> "▢ ";
        };
        Tone tone = switch (s) {
            case ONLINE -> Tone.ONLINE;
            case STALE -> Tone.STALE;
            case OFFLINE -> Tone.OFFLINE;
            case SENSOR_FAULT -> Tone.SENSOR_FAULT;
            case NO_DATA -> Tone.NO_DATA;
            case UNASSIGNED -> Tone.UNASSIGNED;
        };
        return new Tagged(glyph + healthName(s), tone);
    }

    public static Tagged severity(Severity s) {
        return s == Severity.HIGH ? new Tagged("■ SOS HIGH", Tone.SOS_HIGH) : new Tagged("▲ Warning", Tone.WARNING);
    }

    public static String type(IncidentType t) {
        return switch (t) {
            case SOS -> "SOS";
            case MOTION -> "Motion";
            case ENVIRONMENT -> "Environment warning";
        };
    }

    public static String state(IncidentState s) {
        return switch (s) {
            case OPEN -> "Open";
            case ACKNOWLEDGED -> "Acknowledged";
            case RESOLVED -> "Resolved";
        };
    }

    public static String level(EscalationLevel l) {
        return switch (l) {
            case NONE -> "None";
            case LEVEL_1 -> "L1";
            case LEVEL_2 -> "L2";
        };
    }

    public static String category(ResolutionCategory c) {
        return switch (c) {
            case FALSE_ALARM -> "False alarm";
            case TEST -> "Test";
            case ASSISTANCE_GIVEN -> "Assistance given";
            case NO_ACTION_NEEDED -> "No action needed";
            case OTHER -> "Other";
        };
    }

    /** Air temperature near the helmet. Null means unavailable and shows as "--". */
    public static String temperature(Double celsius) {
        return celsius == null ? MISSING : String.format(Locale.ROOT, "%.1f °C", celsius);
    }

    public static String humidity(Double pct) {
        return pct == null ? MISSING : String.format(Locale.ROOT, "%.0f %%", pct);
    }

    /** mm:ss, or h:mm:ss from one hour. Null shows as "--". */
    public static String duration(Duration d) {
        if (d == null) return MISSING;
        long s = Math.max(0, d.getSeconds());
        if (s >= 3600) return String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
        return String.format(Locale.ROOT, "%02d:%02d", s / 60, s % 60);
    }

    public static String clock(Instant t) {
        return t == null ? MISSING : CLOCK.withZone(ZoneId.systemDefault()).format(t);
    }

    /** Health counts in fixed order, then open SOS, open warnings, unassigned helmets. */
    public static List<Count> counts(SiteOverview o) {
        List<Count> out = new ArrayList<>();
        for (HealthState s : COUNTED) {
            out.add(new Count(healthName(s), o.statusCounts().getOrDefault(s, 0), health(s).tone()));
        }
        out.add(new Count("Open SOS", o.openSos(), Tone.SOS_HIGH));
        out.add(new Count("Open warnings", o.openWarnings(), Tone.WARNING));
        out.add(new Count("Unassigned helmets", o.unassignedHelmets(), Tone.UNASSIGNED));
        return out;
    }

    /** The five health counts. They add up to workers on shift. */
    public static int healthTotal(SiteOverview o) {
        int sum = 0;
        for (HealthState s : COUNTED) sum += o.statusCounts().getOrDefault(s, 0);
        return sum;
    }

    /** "All workers" first, then each zone and the "No zone" bucket. */
    public static List<ZoneChoice> zoneChoices(SiteOverview o) {
        List<ZoneChoice> out = new ArrayList<>();
        out.add(new ZoneChoice("All workers", null, false, true, o.workersOnShift()));
        for (ZoneCount z : o.zones()) {
            if (z.zoneId() == null) out.add(new ZoneChoice("No zone", null, true, false, z.workers()));
            else out.add(new ZoneChoice(z.name(), z.zoneId(), false, false, z.workers()));
        }
        return out;
    }

    public static String sosLine(String workerName, String zoneName, Duration open, EscalationLevel level) {
        String who = workerName == null ? "Unassigned helmet" : workerName;
        String where = zoneName == null ? "No zone" : zoneName;
        String escalated = level == null || level == EscalationLevel.NONE ? "" : " (escalated " + level(level) + ")";
        return "SOS " + who + ", " + where + ", open " + duration(open) + escalated;
    }

    /** Text summary of stored readings. The UI shows it instead of a chart in this build. */
    public static String playbackSummary(List<StoredReading> readings) {
        if (readings.isEmpty()) return "No stored readings in the 5 minute window.";
        Double min = null;
        Double max = null;
        int envFaults = 0;
        int motionFaults = 0;
        for (StoredReading r : readings) {
            if (r.temperatureC() == null) {
                envFaults++;
            } else {
                min = min == null ? r.temperatureC() : Math.min(min, r.temperatureC());
                max = max == null ? r.temperatureC() : Math.max(max, r.temperatureC());
            }
            if (r.motionFault()) motionFaults++;
        }
        String range = min == null ? MISSING : String.format(Locale.ROOT, "%.1f to %.1f °C", min, max);
        return readings.size() + " stored readings (1 Hz, " + SIM + "). Air temp near helmet " + range
                + ". Sensor fault samples: environment " + envFaults + ", motion " + motionFaults + ".";
    }

    public static String errorText(Throwable t) {
        if (t instanceof StorageException) return STORAGE_FAULT;
        if (t instanceof ApplicationException) return t.getMessage();
        return "Unexpected error. Retry.";
    }

    /** Assign, end and zone changes are offered to site managers and supervisors only. Read-only otherwise. */
    public static boolean canManageAssignments(com.siteguard.domain.Role role) {
        return role == com.siteguard.domain.Role.SITE_MANAGER || role == com.siteguard.domain.Role.SUPERVISOR;
    }
}
