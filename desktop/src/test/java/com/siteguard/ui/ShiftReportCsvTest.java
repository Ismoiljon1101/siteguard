package com.siteguard.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.siteguard.application.port.IncidentView;
import com.siteguard.application.service.IncidentService.IncidentRow;
import com.siteguard.domain.EscalationLevel;
import com.siteguard.domain.Incident;
import com.siteguard.domain.IncidentState;
import com.siteguard.domain.IncidentType;
import com.siteguard.domain.Severity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ShiftReportCsvTest {

    private static IncidentRow row(String id, IncidentType type, Severity severity, IncidentState state) {
        Incident incident = new Incident(id, type, severity, "SIM-008", state, Instant.parse("2026-11-18T10:00:00Z"),
                "SIMULATED", 1L, 1L, null, EscalationLevel.NONE);
        IncidentView view = new IncidentView(incident, 1L, "SIM-008", 2L, "Worker \"H\"", "Level 1 West");
        return new IncidentRow(view, Duration.ofSeconds(5));
    }

    private static List<IncidentRow> sample() {
        return List.of(
                row("a", IncidentType.SOS, Severity.HIGH, IncidentState.OPEN),
                row("b", IncidentType.SOS, Severity.HIGH, IncidentState.OPEN),
                row("c", IncidentType.MOTION, Severity.WARNING, IncidentState.OPEN),
                row("d", IncidentType.SOS, Severity.HIGH, IncidentState.RESOLVED),
                row("e", IncidentType.MOTION, Severity.WARNING, IncidentState.RESOLVED));
    }

    @Test
    void kpiCountsMatchTheRows() {
        assertArrayEquals(new long[] {5, 3, 2, 3, 2, 2}, ShiftReportPanel.kpiCounts(sample()));
    }

    @Test
    void csvHasOneRowPerIncidentPassingTheFilter(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("shift-report-SIM.csv");
        ShiftReportPanel.writeCsv(file, ShiftReportPanel.csvLines(sample(), ShiftReportPanel.Kpi.ALL));
        List<String> back = Files.readAllLines(file);
        assertEquals(6, back.size(), "header plus five incidents");
        assertTrue(back.get(0).startsWith("\"Time\",\"Type\""));

        Path sos = dir.resolve("sos.csv");
        ShiftReportPanel.writeCsv(sos, ShiftReportPanel.csvLines(sample(), ShiftReportPanel.Kpi.SOS));
        assertEquals(4, Files.readAllLines(sos).size(), "header plus three SOS, matching the SOS tile");

        Path unack = dir.resolve("unack.csv");
        ShiftReportPanel.writeCsv(unack, ShiftReportPanel.csvLines(sample(), ShiftReportPanel.Kpi.SOS_UNACKNOWLEDGED));
        assertEquals(3, Files.readAllLines(unack).size(), "header plus two, matching the SOS unacknowledged tile");
    }

    @Test
    void quotesInNamesAreEscaped(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("q.csv");
        ShiftReportPanel.writeCsv(file, ShiftReportPanel.csvLines(sample(), ShiftReportPanel.Kpi.ALL));
        String line = Files.readAllLines(file).get(1);
        assertTrue(line.contains("\"Worker \"\"H\"\"\""), line);
    }
}
