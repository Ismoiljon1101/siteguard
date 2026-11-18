package com.siteguard.ui;

import com.siteguard.application.service.ApplicationException;
import com.siteguard.application.service.IncidentService.IncidentDetail;
import com.siteguard.bootstrap.AppContext;
import com.siteguard.domain.EscalationLevel;
import com.siteguard.domain.IncidentAction;
import com.siteguard.domain.IncidentState;
import com.siteguard.domain.ResolutionCategory;
import com.siteguard.ui.Presentation.Tone;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;

/**
 * Incident detail: facts, escalation, unacknowledged timer, stored-readings chart, append-only history, acknowledge,
 * note and resolve. Alt+A acknowledges, Alt+R resolves, Esc closes. Resolve needs an acknowledged incident, a
 * category and a note, then a confirmation.
 */
final class IncidentDialog extends JDialog {
    private final AppContext ctx;
    private final long operatorId;
    private final String identity;
    private final Runnable onChanged;

    private final JLabel heading = new JLabel(" ");
    private final JLabel escalation = new JLabel(" ");
    private final JLabel unackTimer = new JLabel(" ");
    private final JTextArea facts = new JTextArea(5, 60);
    private final JScrollPane chartHolder = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    private final HistoryModel history = new HistoryModel();
    private final JTable historyTable = new JTable(history);
    private final JTextArea note = new JTextArea(3, 60);
    private final JComboBox<ResolutionCategory> category = new JComboBox<>(ResolutionCategory.values());
    private final JButton ack = new JButton("Acknowledge (Alt+A)");
    private final JButton addNote = new JButton("Add note");
    private final JButton resolve = new JButton("Resolve (Alt+R)");
    private final JLabel reason = new JLabel(" ");
    private final JLabel error = new JLabel(" ");
    private final Timer ticker = new Timer(1000, e -> tickTimer());
    private IncidentDetail detail;
    private Instant loadedAt;
    private boolean screenshotDone;

    IncidentDialog(Window owner, AppContext ctx, long operatorId, String identity, Runnable onChanged) {
        super(owner, "Incident detail (SIMULATED)", ModalityType.MODELESS);
        this.ctx = ctx;
        this.operatorId = operatorId;
        this.identity = identity;
        this.onChanged = onChanged;

        category.setRenderer((list, value, index, selected, focus) -> {
            JLabel label = new JLabel(value == null ? "" : Presentation.category(value));
            label.setOpaque(true);
            label.setBackground(selected ? list.getSelectionBackground() : list.getBackground());
            label.setForeground(selected ? list.getSelectionForeground() : list.getForeground());
            return label;
        });
        facts.setEditable(false);
        facts.setLineWrap(true);
        facts.setWrapStyleWord(true);
        facts.setBackground(getBackground());
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateButtons(); }
            @Override public void removeUpdate(DocumentEvent e) { updateButtons(); }
            @Override public void changedUpdate(DocumentEvent e) { updateButtons(); }
        });
        category.addActionListener(e -> updateButtons());
        error.setForeground(Theme.text(Tone.SOS_HIGH));
        reason.setForeground(Theme.secondaryText());
        heading.setFont(heading.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        escalation.setFont(escalation.getFont().deriveFont(java.awt.Font.BOLD, 13f));

        ack.addActionListener(e -> acknowledge());
        addNote.addActionListener(e -> addNote());
        resolve.addActionListener(e -> confirmResolve());

        JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        headerRow.add(heading);
        headerRow.add(escalation);
        headerRow.add(unackTimer);
        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.add(headerRow, BorderLayout.NORTH);
        top.add(new JScrollPane(facts), BorderLayout.CENTER);

        historyTable.setRowHeight(22);
        JPanel chartPanel = new JPanel(new BorderLayout());
        chartPanel.setBorder(BorderFactory.createTitledBorder("Stored readings, 5 min before and after (SIMULATED)"));
        chartPanel.add(chartHolder, BorderLayout.CENTER);
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createTitledBorder("History (append-only)"));
        historyPanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        JSplitPane center = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chartPanel, historyPanel);
        center.setResizeWeight(0.68);

        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        actions.setBorder(BorderFactory.createTitledBorder("Actions"));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.add(ack);
        row.add(new JLabel("Category"));
        row.add(category);
        row.add(resolve);
        JPanel noteRow = new JPanel(new BorderLayout(8, 0));
        noteRow.add(new JLabel("Note"), BorderLayout.WEST);
        noteRow.add(new JScrollPane(note), BorderLayout.CENTER);
        noteRow.add(addNote, BorderLayout.EAST);
        actions.add(row);
        actions.add(noteRow);
        actions.add(reason);
        actions.add(error);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        body.add(top, BorderLayout.NORTH);
        body.add(center, BorderLayout.CENTER);
        body.add(actions, BorderLayout.SOUTH);
        setContentPane(body);

        bindKeys();
        setPreferredSize(new Dimension(1200, 700));
        pack();
        setLocationRelativeTo(owner);
        updateButtons();
        load();
    }

    @Override
    public void dispose() {
        ticker.stop();
        super.dispose();
    }

    private void bindKeys() {
        JComponent root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt A"), "ack");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt R"), "resolve");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "close");
        root.getActionMap().put("ack", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (ack.isEnabled()) ack.doClick(); }
        });
        root.getActionMap().put("resolve", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (resolve.isEnabled()) resolve.doClick(); }
        });
        root.getActionMap().put("close", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });
    }

    private void load() {
        Background.run(() -> {
            Optional<IncidentDetail> found = ctx.incidents().detail(operatorId, identity);
            return found.orElseThrow(() -> new ApplicationException("Incident not found."));
        }, this::apply, err -> error.setText(Presentation.errorText(err)));
    }

    private void apply(IncidentDetail d) {
        detail = d;
        loadedAt = Instant.now();
        var v = d.view();
        var incident = v.incident();
        heading.setText(Presentation.type(incident.type())
                + "   " + Presentation.state(incident.state()) + "   " + Presentation.SIM);
        EscalationLevel level = incident.escalation();
        Tone levelTone = level == EscalationLevel.LEVEL_2 ? Tone.ESCALATION_2
                : level == EscalationLevel.LEVEL_1 ? Tone.ESCALATION_1 : Tone.NEUTRAL;
        escalation.setText("Escalation " + Presentation.level(level));
        escalation.setForeground(Theme.text(levelTone));
        tickTimer();
        ticker.start();

        StringBuilder text = new StringBuilder();
        text.append("Receipt ID: ").append(incident.sourceIdentity()).append('\n');
        text.append("Worker: ").append(v.workerName() == null ? "Unassigned" : v.workerName())
                .append("   Helmet: ").append(v.deviceLabel()).append(' ').append(Presentation.SIM).append('\n');
        text.append("Zone (operator-selected): ").append(v.zoneName() == null ? "No zone" : v.zoneName()).append('\n');
        text.append("Occurred: ").append(Presentation.clock(incident.occurredAt())).append('\n');
        if (d.environmentConditionCleared()) text.append("Environment condition cleared.\n");
        text.append("Playback: ").append(Presentation.playbackSummary(d.playback()));
        facts.setText(text.toString());
        facts.setCaretPosition(0);

        chartHolder.setViewportView(ChartPanel.forReadings("Air temp, humidity and acceleration. Gaps are sensor faults. SIMULATED.",
                d.playback(), incident.occurredAt()));
        history.setRows(d.actions());
        error.setText(" ");
        updateButtons();
        if (!screenshotDone) {
            screenshotDone = true;
            ScreenshotHook.capture("incident-detail", getContentPane());
        }
    }

    /** Unacknowledged timer: the backend value plus time since it was loaded, while the incident is OPEN. */
    private void tickTimer() {
        if (detail == null || loadedAt == null) return;
        if (detail.view().incident().state() != IncidentState.OPEN) {
            unackTimer.setText("Acknowledged or resolved");
            ticker.stop();
            return;
        }
        Duration base = detail.unacknowledgedFor() == null ? Duration.ZERO : detail.unacknowledgedFor();
        Duration live = base.plus(Duration.between(loadedAt, Instant.now()));
        unackTimer.setText("Unacknowledged for " + Presentation.duration(live));
    }

    private void updateButtons() {
        IncidentState state = detail == null ? null : detail.view().incident().state();
        boolean hasNote = !note.getText().isBlank();
        boolean hasCategory = category.getSelectedItem() != null;
        ack.setEnabled(state == IncidentState.OPEN);
        addNote.setEnabled(detail != null && state != null && hasNote);
        resolve.setEnabled(state == IncidentState.ACKNOWLEDGED && hasNote && hasCategory);
        if (state == null) reason.setText(" ");
        else if (state == IncidentState.OPEN) reason.setText("Acknowledge first. Resolve stays disabled until then.");
        else if (state == IncidentState.RESOLVED) reason.setText("Resolved. No further actions.");
        else if (!hasNote) reason.setText("Add a note to resolve.");
        else reason.setText(" ");
    }

    private void acknowledge() {
        run(() -> ctx.incidents().acknowledge(identity, operatorId));
    }

    private void addNote() {
        String text = note.getText().strip();
        run(() -> ctx.incidents().addNote(identity, operatorId, text));
        note.setText("");
    }

    private void confirmResolve() {
        ResolutionCategory chosen = (ResolutionCategory) category.getSelectedItem();
        String text = note.getText().strip();
        if (chosen == null || text.isEmpty()) return;
        String message = "Resolve this incident as \"" + Presentation.category(chosen) + "\"?\n"
                + "The category and your note are recorded and cannot be changed.";
        JOptionPane pane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION,
                null, new Object[] {"Resolve", "Cancel"}, "Cancel");
        pane.createDialog(this, "Resolve incident").setVisible(true);
        if ("Resolve".equals(pane.getValue())) {
            run(() -> ctx.incidents().resolve(identity, operatorId, chosen, text));
            note.setText("");
        }
    }

    /** Runs one write off the EDT, then reloads the detail and tells the dashboard. */
    private void run(Callable<?> action) {
        Background.run(() -> {
            action.call();
            return Boolean.TRUE;
        }, ok -> {
            error.setText(" ");
            onChanged.run();
            load();
        }, err -> error.setText(Presentation.errorText(err)));
    }

    /** Append-only history rows. */
    static final class HistoryModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Time", "Action", "Operator", "Detail"};
        private List<IncidentAction> rows = List.of();

        void setRows(List<IncidentAction> next) {
            rows = List.copyOf(next);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }

        @Override public int getColumnCount() { return COLUMNS.length; }

        @Override public String getColumnName(int column) { return COLUMNS[column]; }

        @Override
        public Object getValueAt(int rowIndex, int column) {
            IncidentAction a = rows.get(rowIndex);
            return switch (column) {
                case 0 -> Presentation.clock(a.recordedAt());
                case 1 -> a.action().name();
                case 2 -> a.operatorName() == null ? "System" : a.operatorName();
                default -> detail(a);
            };
        }

        private static String detail(IncidentAction a) {
            StringBuilder s = new StringBuilder();
            if (a.category() != null) s.append(Presentation.category(a.category()));
            if (a.note() != null && !a.note().isBlank()) {
                if (s.length() > 0) s.append(": ");
                s.append(a.note());
            }
            return s.toString();
        }
    }
}
