package com.siteguard.ui;

import com.siteguard.application.port.IncidentView;
import com.siteguard.application.service.SiteQueryService.AssignmentHistoryRow;
import com.siteguard.application.service.SiteQueryService.WorkerDetail;
import com.siteguard.application.service.SiteQueryService.WorkerRow;
import com.siteguard.application.service.ApplicationException;
import com.siteguard.bootstrap.AppContext;
import com.siteguard.domain.Operator;
import com.siteguard.domain.Zone;
import com.siteguard.ui.Presentation.Tone;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

/** Worker detail: status, zone (operator-selected), last 10 minutes chart, assignment and incident history. */
final class WorkerDetailDialog extends JDialog {
    private final AppContext ctx;
    private final Operator operator;
    private final long workerId;
    private final long siteId;
    private final Runnable onChanged;

    private final JLabel heading = new JLabel(" ");
    private final JLabel zoneLine = new JLabel(" ");
    private final JLabel error = new JLabel(" ");
    private final JScrollPane chartHolder = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    private final RowModel assignments = new RowModel("Helmet", "Zone (operator-selected)", "Started", "Ended");
    private final RowModel incidents = new RowModel("Time", "Type", "State", "Level");
    private final JComboBox<Zone> zoneBox = new JComboBox<>();
    private final JButton changeZone = new JButton("Change zone");
    private final JButton endAssignment = new JButton("End assignment");
    private WorkerRow current;

    WorkerDetailDialog(Window owner, AppContext ctx, Operator operator, long workerId, long siteId, Runnable onChanged) {
        super(owner, "Worker detail (SIMULATED)", ModalityType.MODELESS);
        this.ctx = ctx;
        this.operator = operator;
        this.workerId = workerId;
        this.siteId = siteId;
        this.onChanged = onChanged;

        heading.setFont(heading.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        zoneLine.setFont(zoneLine.getFont().deriveFont(java.awt.Font.BOLD, 13f));
        error.setForeground(Theme.text(Tone.SOS_HIGH));
        boolean canManage = Presentation.canManageAssignments(operator.role());
        changeZone.setEnabled(false);
        zoneBox.setRenderer((list, value, index, selected, focus) -> new JLabel(value == null ? "" : value.name()));
        endAssignment.setEnabled(false);
        changeZone.addActionListener(e -> changeZone());
        endAssignment.addActionListener(e -> confirmEnd());

        JTable assignmentTable = new JTable(assignments);
        JTable incidentTable = new JTable(incidents);
        for (JTable t : new JTable[] {assignmentTable, incidentTable}) {
            t.setRowHeight(22);
            t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }
        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.add(heading, BorderLayout.NORTH);
        top.add(zoneLine, BorderLayout.CENTER);

        JPanel chartPanel = new JPanel(new BorderLayout());
        chartPanel.setBorder(BorderFactory.createTitledBorder("Air temp near helmet, last 10 min (SIMULATED)"));
        chartPanel.add(chartHolder, BorderLayout.CENTER);
        JPanel historyPanel = new JPanel(new BorderLayout(0, 6));
        historyPanel.add(titled("Assignments", assignmentTable), BorderLayout.CENTER);
        historyPanel.add(titled("Incidents", incidentTable), BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chartPanel, historyPanel);
        split.setResizeWeight(0.55);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actions.setBorder(BorderFactory.createTitledBorder("Actions"));
        actions.add(new JLabel("Zone"));
        actions.add(zoneBox);
        actions.add(changeZone);
        actions.add(endAssignment);
        if (!canManage) actions.add(new JLabel("Read-only for your role."));
        zoneBox.setEnabled(canManage);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        body.add(top, BorderLayout.NORTH);
        body.add(split, BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout());
        south.add(actions, BorderLayout.CENTER);
        south.add(error, BorderLayout.SOUTH);
        body.add(south, BorderLayout.SOUTH);
        setContentPane(body);
        setPreferredSize(new Dimension(1100, 640));
        pack();
        setLocationRelativeTo(owner);
        load();
    }

    private static JPanel titled(String title, JTable table) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private void load() {
        long opId = operator.operatorId();
        Background.run(() -> {
            WorkerDetail d = ctx.queries().workerDetail(opId, workerId)
                    .orElseThrow(() -> new ApplicationException("Worker not found."));
            return new Loaded(d, ctx.queries().zones(siteId));
        }, this::apply, err -> error.setText(Presentation.errorText(err)));
    }

    private record Loaded(WorkerDetail detail, List<Zone> zones) { }

    private void apply(Loaded loaded) {
        WorkerDetail d = loaded.detail();
        current = d.current();
        heading.setText(d.worker().displayName() + "   " + (d.worker().trade() == null ? "" : d.worker().trade()));
        if (current == null) {
            zoneLine.setText("No helmet assigned. Status: Unassigned.");
        } else {
            zoneLine.setText(Presentation.health(current.health()).text() + "   Helmet " + current.deviceLabel()
                    + " " + (current.simulated() ? Presentation.SIM : "") + "   Zone (operator-selected): "
                    + (current.zoneName() == null ? "No zone" : current.zoneName())
                    + (current.zoneSetAt() == null ? ""
                    : "   set " + Presentation.clock(current.zoneSetAt()) + " by " + current.zoneSetBy()));
        }

        chartHolder.setViewportView(ChartPanel.forReadings("Last 10 minutes, 1 Hz. Gaps are sensor faults. SIMULATED.",
                d.lastTenMinutes(), null));

        List<Object[]> rows = new ArrayList<>();
        for (AssignmentHistoryRow h : d.history()) {
            rows.add(new Object[] {h.deviceLabel(), h.zoneName() == null ? "No zone" : h.zoneName(),
                    Presentation.clock(h.assignment().startsAt()), Presentation.clock(h.assignment().endsAt())});
        }
        assignments.setRows(rows);
        List<Object[]> incidentRows = new ArrayList<>();
        for (IncidentView v : d.incidents()) {
            incidentRows.add(new Object[] {Presentation.clock(v.incident().occurredAt()),
                    Presentation.type(v.incident().type()), Presentation.state(v.incident().state()),
                    Presentation.level(v.incident().escalation())});
        }
        incidents.setRows(incidentRows);

        zoneBox.removeAllItems();
        for (Zone z : loaded.zones()) zoneBox.addItem(z);
        if (current != null && current.zoneId() != null) {
            for (int i = 0; i < zoneBox.getItemCount(); i++) {
                if (zoneBox.getItemAt(i).zoneId() == current.zoneId()) zoneBox.setSelectedIndex(i);
            }
        }
        boolean canManage = Presentation.canManageAssignments(operator.role());
        boolean active = current != null;
        changeZone.setEnabled(canManage && active && zoneBox.getItemCount() > 0);
        endAssignment.setEnabled(canManage && active);
        ScreenshotHook.capture("worker-detail", getContentPane());
    }

    private void changeZone() {
        Zone zone = (Zone) zoneBox.getSelectedItem();
        if (current == null || zone == null) return;
        long assignmentId = current.assignmentId();
        long opId = operator.operatorId();
        run(() -> {
            ctx.assignments().setZone(opId, assignmentId, zone.zoneId());
            return Boolean.TRUE;
        });
    }

    private void confirmEnd() {
        if (current == null) return;
        int choice = JOptionPane.showConfirmDialog(this,
                "End the assignment of " + current.workerName() + " to helmet " + current.deviceLabel() + "?",
                "End assignment", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null);
        if (choice != JOptionPane.YES_OPTION) return;
        long assignmentId = current.assignmentId();
        long opId = operator.operatorId();
        run(() -> {
            ctx.assignments().endAssignment(opId, assignmentId);
            return Boolean.TRUE;
        });
    }

    private void run(java.util.concurrent.Callable<Boolean> action) {
        Background.run(action, ok -> {
            error.setText(" ");
            onChanged.run();
            load();
        }, err -> error.setText(Presentation.errorText(err)));
    }
}
