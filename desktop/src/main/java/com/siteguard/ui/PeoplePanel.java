package com.siteguard.ui;

import com.siteguard.application.service.SiteQueryService.HelmetRow;
import com.siteguard.application.service.SiteQueryService.WorkerQuery;
import com.siteguard.application.service.SiteQueryService.WorkerRow;
import com.siteguard.application.service.SiteQueryService.WorkerSort;
import com.siteguard.bootstrap.AppContext;
import com.siteguard.domain.Operator;
import com.siteguard.domain.Worker;
import com.siteguard.domain.Zone;
import com.siteguard.ui.Presentation.Tone;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/**
 * People and helmets: unassigned and assigned helmets, workers with their current helmet and zone. Assign, end
 * assignment and zone change for site managers and supervisors only. Every row is simulated (SIM) in this build.
 */
public final class PeoplePanel extends JPanel {
    private final AppContext ctx;
    private final Operator operator;
    private final RowModel helmetModel = new RowModel("Helmet", "Status", "Last seen", "Assigned to");
    private final JTable helmetTable = new JTable(helmetModel);
    private final RowModel workerModel = new RowModel("Worker", "Helmet", Presentation.ZONE_LABEL, "Status");
    private final JTable workerTable = new JTable(workerModel);
    private final JComboBox<Worker> workerBox = new JComboBox<>();
    private final JComboBox<Zone> zoneBox = new JComboBox<>();
    private final JButton assign = new JButton("Assign selected helmet");
    private final JButton changeZone = new JButton("Change zone");
    private final JButton endAssignment = new JButton("End assignment");
    private final JButton openWorker = new JButton("Worker detail");
    private final JLabel status = new JLabel(" ");
    private final JLabel error = new JLabel(" ");
    private List<HelmetRow> helmets = List.of();
    private List<WorkerRow> workers = List.of();
    private List<Zone> zones = List.of();
    private final boolean canManage;

    public PeoplePanel(AppContext ctx, Operator operator) {
        super(new BorderLayout(0, 8));
        this.ctx = ctx;
        this.operator = operator;
        this.canManage = Presentation.canManageAssignments(operator.role());
        setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        for (JTable t : new JTable[] {helmetTable, workerTable}) {
            t.setRowHeight(24);
            t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            t.setDefaultRenderer(Object.class, new DashboardPanel.ToneRenderer());
        }
        helmetTable.setBorder(null);
        workerBox.setRenderer((list, value, index, selected, focus) -> new JLabel(value == null ? "" : value.displayName()));
        zoneBox.setRenderer((list, value, index, selected, focus) -> new JLabel(value == null ? "" : value.name()));

        JPanel helmetPanel = new JPanel(new BorderLayout());
        helmetPanel.setBorder(BorderFactory.createTitledBorder("Helmets (SIM)"));
        helmetPanel.add(new JScrollPane(helmetTable), BorderLayout.CENTER);
        JPanel workerPanel = new JPanel(new BorderLayout());
        workerPanel.setBorder(BorderFactory.createTitledBorder("Workers and current helmet (double-click for detail)"));
        workerPanel.add(new JScrollPane(workerTable), BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, helmetPanel, workerPanel);
        split.setResizeWeight(0.45);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actions.setBorder(BorderFactory.createTitledBorder("Assignments"));
        actions.add(new JLabel("Worker"));
        actions.add(workerBox);
        actions.add(new JLabel("Zone"));
        actions.add(zoneBox);
        actions.add(assign);
        actions.add(changeZone);
        actions.add(endAssignment);
        actions.add(openWorker);
        if (!canManage) actions.add(new JLabel("Read-only for your role."));

        error.setForeground(Theme.text(Tone.SOS_HIGH));
        status.setForeground(Theme.secondaryText());
        JPanel south = new JPanel(new BorderLayout());
        south.add(actions, BorderLayout.NORTH);
        south.add(status, BorderLayout.CENTER);
        south.add(error, BorderLayout.SOUTH);

        add(split, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        boolean enabled = canManage;
        assign.setEnabled(enabled);
        changeZone.setEnabled(enabled);
        endAssignment.setEnabled(enabled);
        workerBox.setEnabled(enabled);
        zoneBox.setEnabled(enabled);
        assign.addActionListener(e -> assignSelected());
        changeZone.addActionListener(e -> changeZoneSelected());
        endAssignment.addActionListener(e -> endSelected());
        openWorker.addActionListener(e -> openSelectedWorker());
        workerTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openSelectedWorker();
            }
        });
    }

    /** Reloads helmets, workers, workers' zone choices. Called when the screen is shown and after each change. */
    public void refresh() {
        long opId = operator.operatorId();
        long siteId = ctx.demoSiteId();
        Background.run(() -> {
            List<HelmetRow> h = ctx.queries().helmets(opId, siteId);
            List<WorkerRow> w = ctx.queries().workers(opId,
                    new WorkerQuery(siteId, null, false, null, null, null, WorkerSort.NAME, 0, 500)).rows();
            List<Worker> all = ctx.queries().workers();
            List<Zone> z = ctx.queries().zones(siteId);
            return new Loaded(h, w, all, z);
        }, this::apply, err -> error.setText(Presentation.errorText(err)));
    }

    private record Loaded(List<HelmetRow> helmets, List<WorkerRow> workers, List<Worker> allWorkers, List<Zone> zones) { }

    private void apply(Loaded loaded) {
        helmets = loaded.helmets();
        workers = loaded.workers();
        zones = loaded.zones();
        List<Object[]> hr = new ArrayList<>();
        for (HelmetRow h : helmets) {
            hr.add(new Object[] {h.device().displayName() + " " + Presentation.SIM,
                    Presentation.health(h.health()), Presentation.clock(h.lastSeen()),
                    h.workerName() == null ? "Unassigned" : h.workerName()});
        }
        helmetModel.setRows(hr);
        List<Object[]> wr = new ArrayList<>();
        for (WorkerRow w : workers) {
            wr.add(new Object[] {w.workerName(), w.deviceLabel() + " " + Presentation.SIM,
                    w.zoneName() == null ? "No zone" : w.zoneName(), Presentation.health(w.health())});
        }
        workerModel.setRows(wr);
        Worker selectedWorker = (Worker) workerBox.getSelectedItem();
        workerBox.removeAllItems();
        for (Worker w : loaded.allWorkers()) workerBox.addItem(w);
        if (selectedWorker != null) workerBox.setSelectedItem(selectedWorker);
        zoneBox.removeAllItems();
        zoneBox.addItem(null);
        for (Zone z : zones) zoneBox.addItem(z);
        status.setText(helmets.size() + " helmets, " + workers.size() + " workers with a helmet (SIMULATED)");
        error.setText(" ");
    }

    private void assignSelected() {
        int row = helmetTable.getSelectedRow();
        Worker worker = (Worker) workerBox.getSelectedItem();
        if (row < 0 || worker == null) {
            error.setText("Select a helmet and a worker first.");
            return;
        }
        HelmetRow helmet = helmets.get(helmetTable.convertRowIndexToModel(row));
        Zone zone = (Zone) zoneBox.getSelectedItem();
        Long zoneId = zone == null ? null : zone.zoneId();
        long opId = operator.operatorId();
        String deviceId = helmet.device().deviceId();
        long workerId = worker.workerId();
        run(() -> ctx.assignments().assign(opId, deviceId, workerId, zoneId));
    }

    private WorkerRow selectedWorkerRow() {
        int row = workerTable.getSelectedRow();
        return row < 0 ? null : workers.get(workerTable.convertRowIndexToModel(row));
    }

    private void changeZoneSelected() {
        WorkerRow w = selectedWorkerRow();
        Zone zone = (Zone) zoneBox.getSelectedItem();
        if (w == null || zone == null) {
            error.setText("Select a worker and a zone.");
            return;
        }
        long opId = operator.operatorId();
        long assignmentId = w.assignmentId();
        long zoneId = zone.zoneId();
        run(() -> {
            ctx.assignments().setZone(opId, assignmentId, zoneId);
            return Boolean.TRUE;
        });
    }

    private void endSelected() {
        WorkerRow w = selectedWorkerRow();
        if (w == null) {
            error.setText("Select a worker first.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "End the assignment of " + w.workerName() + " to helmet " + w.deviceLabel() + "?",
                "End assignment", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null);
        if (choice != JOptionPane.YES_OPTION) return;
        long opId = operator.operatorId();
        long assignmentId = w.assignmentId();
        run(() -> {
            ctx.assignments().endAssignment(opId, assignmentId);
            return Boolean.TRUE;
        });
    }

    /** QA tour: selects the first worker row and opens its detail. */
    void openFirstWorker() {
        if (workerModel.getRowCount() == 0) return;
        workerTable.setRowSelectionInterval(0, 0);
        openSelectedWorker();
    }

    private void openSelectedWorker() {
        WorkerRow w = selectedWorkerRow();
        if (w == null) return;
        Window owner = SwingUtilities.getWindowAncestor(this);
        new WorkerDetailDialog(owner, ctx, operator, w.workerId(), ctx.demoSiteId(), this::refresh).setVisible(true);
    }

    private <T> void run(java.util.concurrent.Callable<T> action) {
        Background.run(action, ok -> refresh(), err -> error.setText(Presentation.errorText(err)));
    }

}
