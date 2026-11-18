package com.siteguard.ui;

import com.siteguard.application.port.IncidentFilter;
import com.siteguard.application.port.IncidentView;
import com.siteguard.application.service.IncidentService.IncidentRow;
import com.siteguard.bootstrap.AppContext;
import com.siteguard.domain.IncidentState;
import com.siteguard.domain.IncidentType;
import com.siteguard.domain.Operator;
import com.siteguard.domain.Severity;
import com.siteguard.ui.Presentation.Tone;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Shift report: KPI tiles computed from stored incidents, a click on a tile filters the list below, CSV export.
 * Time to acknowledge is not shown: it needs action times the services do not expose as a list (see docs/UI_REQUESTS.md).
 */
public final class ShiftReportPanel extends JPanel {
    /** The KPI a click selects. ALL shows every row in the window. */
    enum Kpi { ALL, SOS, WARNING, OPEN, RESOLVED, SOS_UNACKNOWLEDGED }

    private static final String[] WINDOW_NAMES = {"Last 8 hours", "Last 24 hours", "All stored"};
    private static final long[] WINDOW_HOURS = {8, 24, -1};
    private static final String[] COLUMNS = {"Time", "Type", "Severity", "State", "Level", "Worker", "Helmet", "Zone (operator-selected)"};

    private final AppContext ctx;
    private final Operator operator;
    private final JComboBox<String> windowBox = new JComboBox<>(WINDOW_NAMES);
    private final JPanel tiles = new JPanel(new GridLayout(1, 6, 8, 0));
    private final JLabel[] tileValues = new JLabel[Kpi.values().length];
    private final RowModel model = new RowModel(COLUMNS);
    private final JTable table = new JTable(model);
    private final JLabel status = new JLabel(" ");
    private final JLabel error = new JLabel(" ");
    private final JButton export = new JButton("Export CSV");
    private List<IncidentRow> rows = List.of();
    private Kpi selected = Kpi.ALL;

    public ShiftReportPanel(AppContext ctx, Operator operator) {
        super(new BorderLayout(0, 10));
        this.ctx = ctx;
        this.operator = operator;
        setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.add(new JLabel("Window"));
        top.add(windowBox);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refresh());
        windowBox.addActionListener(e -> refresh());
        top.add(refresh);
        export.addActionListener(e -> exportCsv());
        top.add(export);
        top.add(new JLabel("Click a figure to filter the list. SIMULATED data."));

        String[] labels = {"Incidents", "SOS high", "Warnings", "Open", "Resolved", "SOS unacknowledged"};
        for (int i = 0; i < Kpi.values().length; i++) {
            Kpi kpi = Kpi.values()[i];
            JPanel tile = new JPanel(new BorderLayout());
            tile.setBorder(BorderFactory.createTitledBorder(labels[i]));
            JLabel value = new JLabel("0", JLabel.CENTER);
            value.setFont(value.getFont().deriveFont(java.awt.Font.BOLD, 28f));
            tileValues[i] = value;
            tile.add(value, BorderLayout.CENTER);
            tile.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            tile.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    selected = kpi;
                    applyFilter();
                }
            });
            tiles.add(tile);
        }

        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new DashboardPanel.ToneRenderer());
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openSelected();
            }
        });
        error.setForeground(Theme.text(Tone.SOS_HIGH));
        status.setForeground(Theme.secondaryText());

        JPanel list = new JPanel(new BorderLayout());
        list.setBorder(BorderFactory.createTitledBorder("Incidents (double-click to open detail)"));
        list.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.add(top, BorderLayout.NORTH);
        north.add(tiles, BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);
        add(list, BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout());
        south.add(status, BorderLayout.CENTER);
        south.add(error, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);
    }

    /** Loads incidents for the chosen window. Runs off the EDT. */
    public void refresh() {
        int index = Math.max(0, windowBox.getSelectedIndex());
        long hours = WINDOW_HOURS[index];
        Instant from = hours < 0 ? null : Instant.now().minus(Duration.ofHours(hours));
        long opId = operator.operatorId();
        long siteId = ctx.demoSiteId();
        Background.run(() -> ctx.incidents().list(opId,
                        new IncidentFilter(siteId, null, null, null, null, null, from, null, 1000, 0)),
                loaded -> {
                    rows = List.copyOf(loaded);
                    error.setText(" ");
                    applyFilter();
                },
                err -> error.setText(Presentation.errorText(err)));
    }

    /** KPI values in tile order: all, SOS, warnings, open, resolved, SOS unacknowledged. */
    static long[] kpiCounts(List<IncidentRow> rows) {
        long sos = rows.stream().filter(r -> r.view().incident().type() == IncidentType.SOS).count();
        long warnings = rows.stream().filter(r -> r.view().incident().severity() == Severity.WARNING).count();
        long open = rows.stream().filter(r -> r.view().incident().state() != IncidentState.RESOLVED).count();
        long resolved = rows.stream().filter(r -> r.view().incident().state() == IncidentState.RESOLVED).count();
        long sosUnack = rows.stream().filter(r -> r.view().incident().type() == IncidentType.SOS
                && r.view().incident().state() == IncidentState.OPEN).count();
        return new long[] {rows.size(), sos, warnings, open, resolved, sosUnack};
    }

    /** CSV lines (header first) for the rows that pass the KPI filter. Pure, so it is unit-tested. */
    static List<String> csvLines(List<IncidentRow> rows, Kpi kpi) {
        List<String> lines = new ArrayList<>();
        lines.add(csvLine(COLUMNS));
        for (IncidentRow r : rows) {
            if (!filterFor(kpi).test(r)) continue;
            Object[] row = toRow(r.view());
            String[] cells = new String[row.length];
            for (int i = 0; i < row.length; i++) {
                cells[i] = row[i] instanceof Presentation.Tagged t ? t.text() : String.valueOf(row[i]);
            }
            lines.add(csvLine(cells));
        }
        return lines;
    }

    /** Writes UTF-8 lines. Blocking file I/O: call off the EDT. */
    static void writeCsv(java.nio.file.Path target, List<String> lines) throws IOException {
        Files.writeString(target, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }

    private void applyFilter() {
        long[] values = kpiCounts(rows);
        for (int i = 0; i < values.length; i++) tileValues[i].setText(String.valueOf(values[i]));
        Predicate<IncidentRow> keep = filterFor(selected);
        List<Object[]> display = new ArrayList<>();
        for (IncidentRow r : rows) {
            if (keep.test(r)) display.add(toRow(r.view()));
        }
        model.setRows(display);
        status.setText("Showing " + display.size() + " of " + rows.size() + " incidents (" + selected + ")   SIMULATED");
    }

    private static Predicate<IncidentRow> filterFor(Kpi kpi) {
        return switch (kpi) {
            case ALL -> r -> true;
            case SOS -> r -> r.view().incident().type() == IncidentType.SOS;
            case WARNING -> r -> r.view().incident().severity() == Severity.WARNING;
            case OPEN -> r -> r.view().incident().state() != IncidentState.RESOLVED;
            case RESOLVED -> r -> r.view().incident().state() == IncidentState.RESOLVED;
            case SOS_UNACKNOWLEDGED -> r -> r.view().incident().type() == IncidentType.SOS
                    && r.view().incident().state() == IncidentState.OPEN;
        };
    }

    private static Object[] toRow(IncidentView v) {
        return new Object[] {
                Presentation.clock(v.incident().occurredAt()),
                Presentation.type(v.incident().type()),
                Presentation.severity(v.incident().severity()),
                Presentation.state(v.incident().state()),
                Presentation.level(v.incident().escalation()),
                v.workerName() == null ? "Unassigned" : v.workerName(),
                v.deviceLabel() + " " + Presentation.SIM,
                v.zoneName() == null ? "No zone" : v.zoneName()};
    }

    private void openSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        List<IncidentRow> shown = rows.stream().filter(filterFor(selected)).toList();
        int index = table.convertRowIndexToModel(row);
        if (index >= shown.size()) return;
        Window owner = SwingUtilities.getWindowAncestor(this);
        new IncidentDialog(owner, ctx, operator.operatorId(), shown.get(index).view().incident().sourceIdentity(),
                this::refresh).setVisible(true);
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export shift report (CSV)");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        chooser.setSelectedFile(new File("shift-report-SIM.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) file = new File(file.getParentFile(), file.getName() + ".csv");
        if (file.exists() && JOptionPane.showConfirmDialog(this, "Overwrite " + file.getName() + "?", "Export",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        List<String> lines = csvLines(rows, selected);
        java.nio.file.Path target = file.toPath();
        status.setText("Writing " + file.getName() + " ...");
        // File I/O runs on a worker thread. The EDT only shows the result.
        Background.run(() -> {
            writeCsv(target, lines);
            return target.toAbsolutePath().toString();
        }, path -> status.setText("Exported " + (lines.size() - 1) + " rows to " + path + " (SIMULATED)"),
                err -> error.setText("Export failed: " + err.getMessage()));
    }

    private static String csvLine(String... cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            String c = cells[i] == null ? "" : cells[i];
            sb.append('"').append(c.replace("\"", "\"\"")).append('"');
        }
        return sb.toString();
    }
}
