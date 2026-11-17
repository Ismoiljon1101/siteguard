package com.siteguard.ui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import com.siteguard.adapter.simulation.Scenario;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/** Swing view for simulated telemetry and operator-managed incidents. */
public final class DashboardFrame extends JFrame {
    private static final Color INK = new Color(31, 43, 54);
    private static final Color MUTED = new Color(101, 116, 128);
    private static final Color ACCENT = new Color(15, 112, 105);
    private static final Color BACKGROUND = new Color(243, 246, 246);
    private final DashboardController controller;
    private final JLabel device = new JLabel("Waiting for simulation");
    private final JLabel deviceStatus = new JLabel("STARTING");
    private final JLabel temperature = valueLabel();
    private final JLabel humidity = valueLabel();
    private final JLabel motion = valueLabel();
    private final JLabel updated = new JLabel("No readings yet");
    private final JLabel storage = new JLabel("Storage: waiting");
    private final JLabel message = new JLabel("Select an incident to take action.");
    private final JTextField operator = new JTextField("Operator", 12);
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Time", "Type", "Severity", "State", "Details", "ID"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private final JComboBox<String> filter = new JComboBox<>(new String[]{"All incidents", "Open", "Acknowledged", "Resolved"});

    public DashboardFrame(DashboardController controller) {
        super("SiteGuard | Simulation");
        this.controller = controller;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(900, 650));
        setSize(1180, 760);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { controller.close(); }
        });
    }

    /** Call on the Swing event thread after constructing the frame. */
    public void showDashboard() {
        setVisible(true);
        controller.start(snapshot -> SwingUtilities.invokeLater(() -> {
            if (isDisplayable()) render(snapshot);
        }));
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(BACKGROUND);
        root.setBorder(new EmptyBorder(24, 28, 24, 28));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JPanel title = new JPanel();
        title.setOpaque(false);
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("SITEGUARD  /  OPERATIONS");
        eyebrow.setForeground(ACCENT);
        eyebrow.setFont(eyebrow.getFont().deriveFont(Font.BOLD, 12f));
        JLabel heading = new JLabel("Safety overview");
        heading.setForeground(INK);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 28f));
        JLabel subtitle = new JLabel("Synthetic data for demonstration and testing");
        subtitle.setForeground(MUTED);
        title.add(eyebrow); title.add(Box.createVerticalStrut(5)); title.add(heading); title.add(subtitle);
        JLabel badge = new JLabel("  SIMULATED  ");
        badge.setOpaque(true); badge.setBackground(new Color(225, 242, 237));
        badge.setForeground(ACCENT); badge.setFont(badge.getFont().deriveFont(Font.BOLD, 13f));
        badge.setBorder(new EmptyBorder(8, 10, 8, 10));
        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        badgeWrap.setOpaque(false); badgeWrap.add(badge);
        top.add(title, BorderLayout.WEST); top.add(badgeWrap, BorderLayout.EAST);
        root.add(top, BorderLayout.NORTH);

        JPanel body = new JPanel(); body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JPanel status = new JPanel(new BorderLayout()); status.setOpaque(false);
        device.setFont(device.getFont().deriveFont(Font.BOLD, 16f)); device.setForeground(INK);
        deviceStatus.setForeground(ACCENT); deviceStatus.setFont(deviceStatus.getFont().deriveFont(Font.BOLD));
        status.add(device, BorderLayout.WEST); status.add(deviceStatus, BorderLayout.EAST);
        body.add(status); body.add(Box.createVerticalStrut(12));
        JPanel cards = new JPanel(new GridLayout(1, 3, 12, 0)); cards.setOpaque(false);
        cards.add(card("TEMPERATURE", temperature, "Degrees Celsius"));
        cards.add(card("HUMIDITY", humidity, "Relative humidity"));
        cards.add(card("MOTION", motion, "Acceleration · m/s²"));
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        body.add(cards); body.add(Box.createVerticalStrut(10));
        JPanel health = new JPanel(new BorderLayout()); health.setOpaque(false);
        updated.setForeground(MUTED); storage.setForeground(MUTED);
        health.add(updated, BorderLayout.WEST); health.add(storage, BorderLayout.EAST);
        body.add(health); body.add(Box.createVerticalStrut(22));

        JPanel incidentHeader = new JPanel(new BorderLayout()); incidentHeader.setOpaque(false);
        JLabel incidentsTitle = new JLabel("Incident activity");
        incidentsTitle.setForeground(INK); incidentsTitle.setFont(incidentsTitle.getFont().deriveFont(Font.BOLD, 19f));
        incidentHeader.add(incidentsTitle, BorderLayout.WEST); incidentHeader.add(filter, BorderLayout.EAST);
        body.add(incidentHeader); body.add(Box.createVerticalStrut(8));
        table.setRowHeight(32); table.setFillsViewportHeight(true); table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(218, 238, 234)); table.setSelectionForeground(INK);
        table.setRowSorter(sorter); table.getTableHeader().setReorderingAllowed(false);
        filter.addActionListener(e -> {
            int selected = filter.getSelectedIndex();
            sorter.setRowFilter(selected == 0 ? null : RowFilter.regexFilter("(?i)^" + filter.getSelectedItem() + "$", 3));
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 228, 227)));
        body.add(scroll); body.add(Box.createVerticalStrut(10));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); actions.setOpaque(false);
        actions.add(new JLabel("Operator")); actions.add(operator);
        actions.add(button("Acknowledge", () -> act("acknowledge")));
        actions.add(button("Add note", () -> act("note")));
        actions.add(button("Resolve", () -> act("resolve")));
        body.add(actions); body.add(Box.createVerticalStrut(8));
        message.setForeground(MUTED); body.add(message);
        body.add(Box.createVerticalStrut(18));
        JPanel simulations = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); simulations.setOpaque(false);
        simulations.add(new JLabel("SIMULATION CONTROLS"));
        JComboBox<String> scenario = new JComboBox<>(new String[]{"Normal", "Environmental warning", "Sensor fault"});
        scenario.addActionListener(e -> controller.setScenario(Scenario.values()[scenario.getSelectedIndex()]));
        simulations.add(scenario);
        simulations.add(button("Trigger SOS", controller::triggerSos));
        simulations.add(button("Trigger motion", controller::triggerMotion));
        body.add(simulations);
        root.add(body, BorderLayout.CENTER);
        return root;
    }

    private static JLabel valueLabel() {
        JLabel label = new JLabel("—"); label.setForeground(INK);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 25f)); return label;
    }

    private static JPanel card(String title, JLabel value, String caption) {
        JPanel card = new JPanel(); card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(title); name.setForeground(MUTED);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 11f));
        JLabel description = new JLabel(caption); description.setForeground(MUTED);
        card.add(name); card.add(Box.createVerticalStrut(8)); card.add(value);
        card.add(Box.createVerticalStrut(6)); card.add(description); return card;
    }

    private static JButton button(String text, Runnable action) {
        JButton button = new JButton(text); button.setFocusPainted(false);
        button.addActionListener(e -> action.run()); return button;
    }

    private void render(DashboardSnapshot snapshot) {
        String selected = selectedId();
        device.setText(snapshot.deviceId() == null ? "Waiting for simulation" : snapshot.deviceId());
        deviceStatus.setText(snapshot.deviceStatus() == null ? "UNKNOWN" : snapshot.deviceStatus());
        temperature.setText(format(snapshot.temperatureC(), "°C"));
        humidity.setText(format(snapshot.humidityPct(), "%"));
        if (snapshot.accelerationX() == null || snapshot.accelerationY() == null || snapshot.accelerationZ() == null) {
            motion.setText("Unavailable");
        } else {
            motion.setText(String.format(Locale.ROOT, "%.1f / %.1f / %.1f", snapshot.accelerationX(), snapshot.accelerationY(), snapshot.accelerationZ()));
        }
        updated.setText("Last update: " + fallback(snapshot.lastUpdate()));
        storage.setText("Storage: " + fallback(snapshot.storageStatus()));
        model.setRowCount(0);
        for (IncidentRow incident : snapshot.incidents()) {
            model.addRow(new Object[]{incident.occurredAt(), incident.type(), incident.severity(), incident.state(), incident.detail(), incident.id()});
        }
        if (selected != null) {
            for (int i = 0; i < model.getRowCount(); i++) {
                if (selected.equals(model.getValueAt(i, 5))) {
                    int view = table.convertRowIndexToView(i);
                    if (view >= 0) table.setRowSelectionInterval(view, view);
                    break;
                }
            }
        }
    }

    private void act(String action) {
        String id = selectedId();
        if (id == null) { message.setText("Select an incident first."); return; }
        String name = operator.getText().trim();
        if (name.isEmpty()) { message.setText("Enter an operator name."); return; }
        int row = table.convertRowIndexToModel(table.getSelectedRow());
        String state = String.valueOf(model.getValueAt(row, 3));
        if (action.equals("acknowledge") && !state.equalsIgnoreCase("OPEN")) {
            message.setText("Only open incidents can be acknowledged."); return;
        }
        if (action.equals("resolve") && !state.equalsIgnoreCase("ACKNOWLEDGED")) {
            message.setText("Acknowledge the incident before resolving it."); return;
        }
        if (action.equals("note") && state.equalsIgnoreCase("RESOLVED")) {
            message.setText("Resolved incidents cannot be changed."); return;
        }
        if (action.equals("acknowledge")) {
            controller.acknowledge(id, name);
        } else {
            String note = JOptionPane.showInputDialog(this, action.equals("resolve") ? "Resolution note:" : "Incident note:");
            if (note == null) return;
            if (note.trim().isEmpty()) { message.setText("A note is required."); return; }
            if (action.equals("resolve")) controller.resolve(id, name, note.trim());
            else controller.addNote(id, name, note.trim());
        }
        message.setText("Action submitted. Waiting for updated incident state.");
    }

    private String selectedId() {
        int row = table.getSelectedRow();
        return row < 0 ? null : String.valueOf(model.getValueAt(table.convertRowIndexToModel(row), 5));
    }
    private static String format(Double value, String unit) {
        return value == null ? "Unavailable" : String.format(Locale.ROOT, "%.1f %s", value, unit);
    }
    private static String fallback(String value) { return value == null || value.isBlank() ? "Unavailable" : value; }
}
