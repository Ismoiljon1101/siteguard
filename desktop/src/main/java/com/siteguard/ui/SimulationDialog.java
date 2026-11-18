package com.siteguard.ui;

import com.siteguard.adapter.simulation.HelmetScenario;
import com.siteguard.bootstrap.AppContext;
import com.siteguard.domain.EventType;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/** Start, stop, reset and script the simulated helmets. Every row is SIM. Scenario changes apply on the next tick. */
final class SimulationDialog extends JDialog {
    private final AppContext ctx;
    private final DefaultTableModel model = new DefaultTableModel(new Object[] {"Helmet", "Scenario"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return column == 1; }
    };
    private final JTable table = new JTable(model);
    private final JLabel status = new JLabel(" ");
    private final JLabel error = new JLabel(" ");
    private boolean loading;

    SimulationDialog(Window owner, AppContext ctx) {
        super(owner, "Simulation (SIMULATED DATA)", ModalityType.MODELESS);
        this.ctx = ctx;
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(new JComboBox<>(HelmetScenario.values())));
        error.setForeground(Theme.text(Presentation.Tone.SOS_HIGH));

        model.addTableModelListener(e -> {
            if (loading || e.getColumn() != 1 || e.getFirstRow() < 0) return;
            String helmet = String.valueOf(model.getValueAt(e.getFirstRow(), 0)).replace(" " + Presentation.SIM, "");
            HelmetScenario scenario = (HelmetScenario) model.getValueAt(e.getFirstRow(), 1);
            run(() -> {
                ctx.simulation().setScenario(helmet, scenario);
                return Boolean.TRUE;
            });
        });

        JButton start = new JButton("Start");
        JButton stop = new JButton("Stop");
        JButton reset = new JButton("Reset");
        JButton trigger = new JButton("Trigger SOS on selected");
        start.addActionListener(e -> run(() -> { ctx.simulation().start(); return Boolean.TRUE; }));
        stop.addActionListener(e -> run(() -> { ctx.simulation().stop(); return Boolean.TRUE; }));
        reset.addActionListener(e -> confirmReset());
        trigger.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            String helmet = String.valueOf(model.getValueAt(row, 0)).replace(" " + Presentation.SIM, "");
            run(() -> { ctx.simulation().trigger(helmet, EventType.SOS); return Boolean.TRUE; });
        });

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controls.add(start);
        controls.add(stop);
        controls.add(reset);
        controls.add(trigger);

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.add(status, BorderLayout.NORTH);
        top.add(controls, BorderLayout.CENTER);
        top.add(error, BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        body.add(top, BorderLayout.NORTH);
        body.add(new JScrollPane(table), BorderLayout.CENTER);
        setContentPane(body);
        setSize(560, 520);
        setLocationRelativeTo(owner);
        load();
    }

    private void load() {
        Background.run(() -> {
            List<Object[]> rows = new ArrayList<>();
            boolean running = ctx.simulation().running();
            for (String id : ctx.simulation().helmets()) {
                rows.add(new Object[] {id + " " + Presentation.SIM, ctx.simulation().scenarioOf(id)});
            }
            return new Loaded(running, rows);
        }, loaded -> {
            loading = true;
            try {
                model.setRowCount(0);
                for (Object[] row : loaded.rows()) model.addRow(row);
            } finally {
                loading = false;
            }
            showStatus(loaded.running());
        }, err -> error.setText(Presentation.errorText(err)));
    }

    private record Loaded(boolean running, List<Object[]> rows) { }

    private void showStatus(boolean running) {
        status.setText(running
                ? Presentation.SIMULATED_BADGE + ": simulator running. Every helmet here is simulated (" + Presentation.SIM + ")."
                : "Simulator stopped. Start it to feed simulated helmets.");
        error.setText(" ");
    }

    private void confirmReset() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Reset the simulation? It starts a new simulated boot. Stored history is kept.",
                "Reset simulation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null);
        if (choice == JOptionPane.YES_OPTION) {
            run(() -> { ctx.simulation().reset(); return Boolean.TRUE; });
        }
    }

    private void run(java.util.concurrent.Callable<Boolean> action) {
        Background.run(action, ok -> load(), err -> error.setText(Presentation.errorText(err)));
    }
}
