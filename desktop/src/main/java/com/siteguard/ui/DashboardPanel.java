package com.siteguard.ui;

import com.siteguard.application.port.IncidentFilter;
import com.siteguard.application.port.IncidentView;
import com.siteguard.application.service.IncidentService.IncidentRow;
import com.siteguard.application.service.SiteQueryService.SiteOverview;
import com.siteguard.application.service.SiteQueryService.WorkerPage;
import com.siteguard.application.service.SiteQueryService.WorkerQuery;
import com.siteguard.application.service.SiteQueryService.WorkerRow;
import com.siteguard.application.service.SiteQueryService.WorkerSort;
import com.siteguard.bootstrap.AppContext;
import com.siteguard.domain.EscalationLevel;
import com.siteguard.domain.HealthState;
import com.siteguard.domain.IncidentState;
import com.siteguard.domain.IncidentType;
import com.siteguard.domain.Operator;
import com.siteguard.ui.Presentation.Count;
import com.siteguard.ui.Presentation.Tagged;
import com.siteguard.ui.Presentation.Tone;
import com.siteguard.ui.Presentation.ZoneChoice;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/** Site overview: alert strip, count bar, zones, workers, open incidents. Refreshes about once a second off the EDT. */
public final class DashboardPanel extends JPanel {
    /** One refresh result. */
    public record Snapshot(SiteOverview overview, WorkerPage workers, List<IncidentRow> incidents, boolean simRunning) { }

    private static final HealthState[] STATUS_CHOICES = {null, HealthState.ONLINE, HealthState.STALE,
            HealthState.OFFLINE, HealthState.SENSOR_FAULT, HealthState.NO_DATA};
    private static final String[] STATUS_NAMES = {"All statuses", "Online", "Stale", "Offline", "Sensor fault", "No data yet"};

    private final AppContext ctx;
    private final Operator operator;
    private final Consumer<Snapshot> onSnapshot;
    private final Timer timer = new Timer(1000, e -> refresh());
    private final int pageSize = Toolkit.getDefaultToolkit().getScreenSize().height >= 1000 ? 50 : 25;

    private boolean busy;
    private boolean dirty;
    private String zoneKey = "all";
    private int page;
    private int lastTotal;

    private final JTextField search = new JTextField(14);
    private final JComboBox<String> statusBox = new JComboBox<>(STATUS_NAMES);
    private final JComboBox<String> sortBox = new JComboBox<>(new String[] {"Needs attention", "Name"});
    private final JLabel[] countLabels = new JLabel[8];
    private final JPanel alertStrip = new JPanel(new BorderLayout(8, 0));
    private final JLabel alertText = new JLabel(" ");
    private final JButton alertAck = new JButton("Acknowledge");
    private final JButton alertOpen = new JButton("Open");
    private String alertIdentity;
    private final DefaultListModel<ZoneChoice> zoneModel = new DefaultListModel<>();
    private final JList<ZoneChoice> zoneList = new JList<>(zoneModel);
    private boolean updatingZones;
    private final WorkerModel workerModel = new WorkerModel();
    private final JTable workerTable = new JTable(workerModel);
    private final IncidentModel incidentModel = new IncidentModel();
    private final JTable incidentTable = new JTable(incidentModel);
    private final JLabel pageLabel = new JLabel(" ");
    private final JButton prevButton = new JButton("Previous");
    private final JButton nextButton = new JButton("Next");
    private final JLabel footer = new JLabel(" ");

    public DashboardPanel(AppContext ctx, Operator operator, Consumer<Snapshot> onSnapshot) {
        super(new BorderLayout(0, 8));
        this.ctx = ctx;
        this.operator = operator;
        this.onSnapshot = onSnapshot;
        setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        add(buildNorth(), BorderLayout.NORTH);
        add(buildZones(), BorderLayout.WEST);
        JSplitPane centre = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildWorkers(), buildIncidents());
        centre.setResizeWeight(0.62);
        centre.setBorder(null);
        add(centre, BorderLayout.CENTER);
        footer.setForeground(Theme.secondaryText());
        add(footer, BorderLayout.SOUTH);
        wireEvents();
    }

    // ---- layout -----------------------------------------------------------------------------------------------

    private JPanel buildNorth() {
        JPanel north = new JPanel(new BorderLayout(0, 8));

        alertStrip.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        alertStrip.add(alertText, BorderLayout.CENTER);
        JPanel alertButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        alertButtons.setOpaque(false);
        alertButtons.add(alertAck);
        alertButtons.add(alertOpen);
        alertStrip.add(alertButtons, BorderLayout.EAST);
        alertStrip.setVisible(false);
        north.add(alertStrip, BorderLayout.NORTH);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        for (int i = 0; i < countLabels.length; i++) {
            countLabels[i] = new JLabel("-");
            bar.add(countLabels[i]);
        }
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filters.add(new JLabel("Status"));
        filters.add(statusBox);
        filters.add(new JLabel("Sort"));
        filters.add(sortBox);
        filters.add(new JLabel("Search"));
        filters.add(search);
        JButton sim = new JButton("Simulation...");
        sim.addActionListener(e -> openSimulation());
        filters.add(sim);
        JPanel rows = new JPanel(new BorderLayout());
        rows.add(bar, BorderLayout.NORTH);
        rows.add(filters, BorderLayout.SOUTH);
        north.add(rows, BorderLayout.CENTER);
        return north;
    }

    private JScrollPane buildZones() {
        zoneList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        zoneList.setCellRenderer((list, value, index, selected, focus) -> {
            JLabel label = new JLabel(value.label() + "  " + value.workers());
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            label.setBackground(selected ? list.getSelectionBackground() : list.getBackground());
            label.setForeground(selected ? list.getSelectionForeground() : list.getForeground());
            return label;
        });
        JScrollPane pane = new JScrollPane(zoneList);
        pane.setPreferredSize(new Dimension(200, 100));
        pane.setBorder(BorderFactory.createTitledBorder("Zones (click to filter)"));
        return pane;
    }

    private JPanel buildWorkers() {
        workerTable.setDefaultRenderer(Object.class, new ToneRenderer());
        workerTable.setRowHeight(24);
        workerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        workerTable.getTableHeader().setReorderingAllowed(false);
        workerTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        workerTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        workerTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        workerTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        workerTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        workerTable.getColumnModel().getColumn(7).setPreferredWidth(140);

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Workers"));
        panel.add(new JScrollPane(workerTable), BorderLayout.CENTER);
        JPanel pager = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pager.add(prevButton);
        pager.add(nextButton);
        pager.add(pageLabel);
        panel.add(pager, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildIncidents() {
        incidentTable.setDefaultRenderer(Object.class, new ToneRenderer());
        incidentTable.setRowHeight(24);
        incidentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        incidentTable.getTableHeader().setReorderingAllowed(false);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Open incidents (A acknowledge, R or Enter open)"));
        panel.add(new JScrollPane(incidentTable), BorderLayout.CENTER);
        return panel;
    }

    private void wireEvents() {
        alertAck.addActionListener(e -> { if (alertIdentity != null) acknowledge(alertIdentity); });
        alertOpen.addActionListener(e -> { if (alertIdentity != null) openIncident(alertIdentity); });
        statusBox.addActionListener(e -> reloadFirstPage());
        sortBox.addActionListener(e -> reloadFirstPage());
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { reloadFirstPage(); }
            @Override public void removeUpdate(DocumentEvent e) { reloadFirstPage(); }
            @Override public void changedUpdate(DocumentEvent e) { reloadFirstPage(); }
        });
        zoneList.addListSelectionListener((ListSelectionEvent e) -> {
            if (updatingZones || e.getValueIsAdjusting()) return;
            ZoneChoice choice = zoneList.getSelectedValue();
            if (choice != null && !choice.key().equals(zoneKey)) {
                zoneKey = choice.key();
                reloadFirstPage();
            }
        });
        prevButton.addActionListener(e -> {
            if (page > 0) {
                page--;
                refresh();
            }
        });
        nextButton.addActionListener(e -> {
            if ((long) (page + 1) * pageSize < lastTotal) {
                page++;
                refresh();
            }
        });
        workerTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openSelectedWorker();
            }
        });
        incidentTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openSelectedIncident();
            }
        });
        bindKeys(incidentTable, "A", () -> {
            IncidentView v = selectedIncident();
            if (v != null && v.incident().state() == IncidentState.OPEN) acknowledge(v.incident().sourceIdentity());
        });
        bindKeys(incidentTable, "R", this::openSelectedIncident);
        bindKeys(incidentTable, "ENTER", this::openSelectedIncident);
        bindKeys(workerTable, "ENTER", this::openSelectedWorker);
    }

    private static void bindKeys(JTable table, String key, Runnable action) {
        table.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key), key);
        table.getActionMap().put(key, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { action.run(); }
        });
    }

    // ---- lifecycle and keyboard entry points -------------------------------------------------------------------

    public void start() {
        refresh();
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    public void refreshNow() {
        refresh();
    }

    public void focusSearch() { search.requestFocusInWindow(); search.selectAll(); }

    public void focusWorkers() { workerTable.requestFocusInWindow(); }

    public void focusIncidents() { incidentTable.requestFocusInWindow(); }

    public void openSimulation() {
        new SimulationDialog(windowOf(), ctx).setVisible(true);
    }

    // ---- refresh -----------------------------------------------------------------------------------------------

    private void reloadFirstPage() {
        page = 0;
        refresh();
    }

    /** At most one refresh runs. A request during a run is remembered and runs once more afterwards. */
    private void refresh() {
        if (busy) {
            dirty = true;
            return;
        }
        busy = true;
        dirty = false;
        WorkerQuery query = query();
        long opId = operator.operatorId();
        long siteId = ctx.demoSiteId();
        Background.run(() -> new Snapshot(
                ctx.queries().overview(opId, siteId),
                ctx.queries().workers(opId, query),
                ctx.incidents().list(opId, IncidentFilter.openForSite(siteId)),
                ctx.simulation().running()),
                snap -> {
                    busy = false;
                    apply(snap);
                    if (dirty) refresh();
                },
                err -> {
                    busy = false;
                    showError(err);
                    if (dirty) refresh();
                });
    }

    private WorkerQuery query() {
        Long zoneId = null;
        boolean noZone = "none".equals(zoneKey);
        if (zoneKey.startsWith("z")) zoneId = Long.parseLong(zoneKey.substring(1));
        String text = search.getText().strip();
        HealthState status = STATUS_CHOICES[Math.max(0, statusBox.getSelectedIndex())];
        Set<HealthState> health = status == null ? Set.of() : Set.of(status);
        WorkerSort sort = sortBox.getSelectedIndex() == 1 ? WorkerSort.NAME : WorkerSort.NEEDS_ATTENTION;
        return new WorkerQuery(ctx.demoSiteId(), zoneId, noZone, null, text.isEmpty() ? null : text,
                health, sort, page, pageSize);
    }

    private void apply(Snapshot snap) {
        SiteOverview overview = snap.overview();
        onSnapshot.accept(snap);

        List<Count> counts = Presentation.counts(overview);
        for (int i = 0; i < countLabels.length; i++) {
            Count c = counts.get(i);
            countLabels[i].setText(c.label() + ": " + c.value());
            countLabels[i].setForeground(Theme.text(c.tone()));
        }
        applyZones(Presentation.zoneChoices(overview));
        applyAlert(snap.incidents());

        workerModel.setRows(snap.workers().rows());
        lastTotal = snap.workers().total();
        int pages = Math.max(1, (lastTotal + pageSize - 1) / pageSize);
        pageLabel.setText("Page " + (page + 1) + " of " + pages + "   " + lastTotal + " workers match");
        prevButton.setEnabled(page > 0);
        nextButton.setEnabled((long) (page + 1) * pageSize < lastTotal);
        if (snap.workers().rows().isEmpty() && lastTotal > 0 && page > 0) {
            page = pages - 1;
            refresh();
        }

        incidentModel.setRows(snap.incidents());
        footer.setForeground(Theme.secondaryText());
        footer.setText("Updated " + Presentation.clock(java.time.Instant.now()) + "   Health total "
                + Presentation.healthTotal(overview) + " of " + overview.workersOnShift() + " workers on shift"
                + "   Simulated helmets " + overview.simulatedHelmets() + " (" + Presentation.SIM + ")"
                + "   Storage OK");
    }

    private void applyZones(List<ZoneChoice> choices) {
        updatingZones = true;
        try {
            zoneModel.clear();
            int selected = 0;
            for (int i = 0; i < choices.size(); i++) {
                ZoneChoice c = choices.get(i);
                zoneModel.addElement(c);
                if (c.key().equals(zoneKey)) selected = i;
            }
            zoneList.setSelectedIndex(selected);
        } finally {
            updatingZones = false;
        }
    }

    /** Shows the oldest unacknowledged SOS. Other open SOS are counted in the text. */
    private void applyAlert(List<IncidentRow> incidents) {
        IncidentRow top = null;
        int count = 0;
        for (IncidentRow row : incidents) {
            if (row.view().incident().type() != IncidentType.SOS || row.view().incident().state() != IncidentState.OPEN) continue;
            count++;
            if (top == null || age(row).compareTo(age(top)) > 0) top = row;
        }
        if (top == null) {
            alertStrip.setVisible(false);
            alertIdentity = null;
            return;
        }
        EscalationLevel level = top.view().incident().escalation();
        Tone tone = level == EscalationLevel.LEVEL_2 ? Tone.ESCALATION_2
                : level == EscalationLevel.LEVEL_1 ? Tone.ESCALATION_1 : Tone.SOS_HIGH;
        String text = Presentation.sosLine(top.view().workerName(), top.view().zoneName(),
                top.unacknowledgedFor(), level);
        if (count > 1) text += "   (+" + (count - 1) + " more open SOS)";
        alertText.setText(text);
        alertText.setForeground(Theme.stripText());
        alertStrip.setBackground(Theme.fill(tone));
        alertStrip.setOpaque(true);
        alertAck.setEnabled(true);
        alertIdentity = top.view().incident().sourceIdentity();
        alertStrip.setVisible(true);
    }

    private static java.time.Duration age(IncidentRow row) {
        return row.unacknowledgedFor() == null ? java.time.Duration.ZERO : row.unacknowledgedFor();
    }

    /** Shows a short message in the footer (used for shortcuts not built yet). */
    void setStatusMessage(String text) {
        footer.setForeground(Theme.secondaryText());
        footer.setText(text);
    }

    private void showError(Throwable err) {
        footer.setForeground(Theme.text(Tone.SOS_HIGH));
        footer.setText(Presentation.errorText(err));
    }

    // ---- actions -----------------------------------------------------------------------------------------------

    private void acknowledge(String identity) {
        long opId = operator.operatorId();
        Background.run(() -> {
            ctx.incidents().acknowledge(identity, opId);
            return Boolean.TRUE;
        }, ok -> refresh(), this::showError);
    }

    private IncidentView selectedIncident() {
        int row = incidentTable.getSelectedRow();
        return row < 0 ? null : incidentModel.at(incidentTable.convertRowIndexToModel(row)).view();
    }

    private void openSelectedIncident() {
        IncidentView v = selectedIncident();
        if (v != null) openIncident(v.incident().sourceIdentity());
    }

    private void openSelectedWorker() {
        int row = workerTable.getSelectedRow();
        if (row < 0) return;
        WorkerRow w = workerModel.at(workerTable.convertRowIndexToModel(row));
        new WorkerDetailDialog(windowOf(), ctx, operator, w.workerId(), ctx.demoSiteId(), this::refresh).setVisible(true);
    }

    /** QA tour: opens the oldest unacknowledged SOS, if any. */
    void openFirstSos() {
        if (alertIdentity != null) openIncident(alertIdentity);
    }


    private void openIncident(String identity) {
        new IncidentDialog(windowOf(), ctx, operator.operatorId(), identity, this::refresh).setVisible(true);
    }

    private Window windowOf() {
        return SwingUtilities.getWindowAncestor(this);
    }

    // ---- table models and renderers ----------------------------------------------------------------------------

    /** Renders Tagged cells in their tone colour. Selected rows keep the selection colours. */
    static final class ToneRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus,
                                                       int row, int column) {
            Object shown = value instanceof Tagged tagged ? tagged.text() : value;
            super.getTableCellRendererComponent(table, shown, selected, focus, row, column);
            if (selected) {
                setForeground(table.getSelectionForeground());
            } else if (value instanceof Tagged tagged) {
                setForeground(Theme.text(tagged.tone()));
            } else {
                setForeground(table.getForeground());
            }
            return this;
        }
    }

    static final class WorkerModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Worker", "Helmet", Presentation.ZONE_LABEL, "Status",
                Presentation.TEMP_LABEL, "Humidity %", "Updated", "Incident"};
        private List<WorkerRow> rows = List.of();

        void setRows(List<WorkerRow> next) {
            rows = List.copyOf(next);
            fireTableDataChanged();
        }

        WorkerRow at(int index) { return rows.get(index); }

        @Override public int getRowCount() { return rows.size(); }

        @Override public int getColumnCount() { return COLUMNS.length; }

        @Override public String getColumnName(int column) { return COLUMNS[column]; }

        @Override
        public Object getValueAt(int rowIndex, int column) {
            WorkerRow w = rows.get(rowIndex);
            return switch (column) {
                case 0 -> w.workerName();
                case 1 -> w.deviceLabel() + (w.simulated() ? " " + Presentation.SIM : "");
                case 2 -> w.zoneName() == null ? "No zone" : w.zoneName();
                case 3 -> Presentation.health(w.health());
                case 4 -> Presentation.temperature(w.airTempC());
                case 5 -> Presentation.humidity(w.humidityPct());
                case 6 -> Presentation.duration(w.updatedAge());
                default -> w.incidentType() == null ? Presentation.MISSING
                        : Presentation.type(w.incidentType()) + " " + Presentation.state(w.incidentState());
            };
        }
    }

    static final class IncidentModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Time", "Type", "Severity", "State", "Level", "Worker", "Helmet",
                "Unacknowledged for"};
        private List<IncidentRow> rows = List.of();

        void setRows(List<IncidentRow> next) {
            rows = List.copyOf(next);
            fireTableDataChanged();
        }

        IncidentRow at(int index) { return rows.get(index); }

        @Override public int getRowCount() { return rows.size(); }

        @Override public int getColumnCount() { return COLUMNS.length; }

        @Override public String getColumnName(int column) { return COLUMNS[column]; }

        @Override
        public Object getValueAt(int rowIndex, int column) {
            IncidentRow row = rows.get(rowIndex);
            IncidentView v = row.view();
            return switch (column) {
                case 0 -> Presentation.clock(v.incident().occurredAt());
                case 1 -> Presentation.type(v.incident().type());
                case 2 -> Presentation.severity(v.incident().severity());
                case 3 -> Presentation.state(v.incident().state());
                case 4 -> Presentation.level(v.incident().escalation());
                case 5 -> v.workerName() == null ? "Unassigned" : v.workerName();
                case 6 -> v.deviceLabel() + ("SIMULATED".equals(v.incident().sourceMode()) ? " " + Presentation.SIM : "");
                default -> Presentation.duration(row.unacknowledgedFor());
            };
        }
    }
}
