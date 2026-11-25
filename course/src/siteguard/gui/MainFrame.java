package siteguard.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;

import siteguard.exceptions.InvalidActionException;
import siteguard.exceptions.StorageException;
import siteguard.model.Incident;
import siteguard.rules.Thresholds;
import siteguard.service.DeviceStatus;
import siteguard.service.MonitorListener;
import siteguard.service.MonitorService;
import siteguard.source.DataSource;
import siteguard.source.ReplaySource;
import siteguard.source.SerialSource;
import siteguard.source.SimulatedSource;
import siteguard.source.WifiSource;
import siteguard.storage.CsvExporter;

/**
 * The operator dashboard. All listeners are anonymous inner classes, as in Chapters 13-15.
 */
public class MainFrame extends JFrame implements MonitorListener {

    private MonitorService monitor;
    private DataSource source;
    private int configVersion = 0;
    private int lastIncidentCount;

    // top bar
    private JRadioButton simRadio = new JRadioButton("Simulation", true);
    private JRadioButton replayRadio = new JRadioButton("Replay file");
    private JRadioButton serialRadio = new JRadioButton("USB serial");
    private JRadioButton wifiRadio = new JRadioButton("Wi-Fi");
    private JTextField sourceField = new JTextField("", 18);
    private JButton startButton = new JButton("Start");
    private JButton stopButton = new JButton("Stop");
    private JLabel sourceLabel = new JLabel("Source: stopped");
    private JLabel alertLabel = new JLabel(" ");
    private JLabel storageLabel = new JLabel();

    // helmets tab
    private JPanel cardsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private ArrayList<DeviceCard> cards = new ArrayList<DeviceCard>();
    private TrendPanel trendPanel = new TrendPanel();

    // incidents tab
    private JCheckBox openOnlyBox = new JCheckBox("Open only", false);
    private JRadioButton allSeverity = new JRadioButton("All", true);
    private JRadioButton highSeverity = new JRadioButton("HIGH");
    private JRadioButton mediumSeverity = new JRadioButton("MEDIUM");
    private JRadioButton lowSeverity = new JRadioButton("LOW");
    private JTextField deviceFilterField = new JTextField(8);
    private JTextArea incidentArea = new JTextArea(18, 80);
    private JTextField idField = new JTextField(4);
    private JTextField noteField = new JTextField(25);

    // simulation tab
    private JRadioButton[] scenarioRadios;

    // settings tab
    private JTextField tempField = new JTextField(6);
    private JTextField humField = new JTextField(6);
    private JTextField sustainField = new JTextField(6);
    private JTextField staleField = new JTextField(6);

    // log tab
    private JTextArea logArea = new JTextArea(20, 80);

    public MainFrame(MonitorService monitor) {
        super("SiteGuard - Smart Construction Helmet Monitor");
        this.monitor = monitor;
        monitor.setListener(this);
        lastIncidentCount = monitor.getIncidents().size();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                stopSource();
                dispose();
                System.exit(0);
            }
        });

        setLayout(new BorderLayout());
        add(buildTopPanel(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Helmets", buildHelmetsTab());
        tabs.addTab("Incidents", buildIncidentsTab());
        tabs.addTab("Simulation", buildSimulationTab());
        tabs.addTab("Settings", buildSettingsTab());
        tabs.addTab("Log", new JScrollPane(logArea));
        add(tabs, BorderLayout.CENTER);

        // Every second: check for stale helmets and update the "seconds ago" values.
        Timer clock = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MainFrame.this.monitor.checkStale(System.currentTimeMillis());
                refreshHelmets();
            }
        });
        clock.start();

        refreshAll();
        pack();
        setLocationRelativeTo(null);
    }

    // ---------------------------------------------------------------- top bar

    private JPanel buildTopPanel() {
        JPanel top = new JPanel(new GridLayout(3, 1));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup group = new ButtonGroup();
        group.add(simRadio);
        group.add(replayRadio);
        group.add(serialRadio);
        group.add(wifiRadio);
        row1.add(new JLabel("Data source:"));
        row1.add(simRadio);
        row1.add(replayRadio);
        row1.add(serialRadio);
        row1.add(wifiRadio);
        row1.add(new JLabel("File / port:"));
        row1.add(sourceField);
        row1.add(startButton);
        row1.add(stopButton);
        stopButton.setEnabled(false);
        top.add(row1);

        // Put a sensible example into the text field when the source type changes.
        simRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sourceField.setText("");
            }
        });
        replayRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sourceField.setText("samples/demo-replay.txt");
            }
        });
        serialRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sourceField.setText("COM3");
            }
        });
        wifiRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sourceField.setText("5050");
            }
        });

        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startSource();
            }
        });
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopSource();
            }
        });

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.add(sourceLabel);
        row2.add(new JLabel("   |   "));
        row2.add(storageLabel);
        top.add(row2);

        alertLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        alertLabel.setOpaque(true);
        top.add(alertLabel);
        return top;
    }

    private void startSource() {
        if (source != null && source.isRunning()) {
            return;
        }
        String text = sourceField.getText().trim();
        try {
            if (simRadio.isSelected()) {
                source = new SimulatedSource();
            } else if (replayRadio.isSelected()) {
                if (text.isEmpty()) {
                    throw new IllegalArgumentException("Enter the replay file name");
                }
                source = new ReplaySource(new File(text));
            } else if (serialRadio.isSelected()) {
                if (text.isEmpty()) {
                    throw new IllegalArgumentException("Enter the serial port, for example COM3");
                }
                source = new SerialSource(text);
            } else {
                int port = Integer.parseInt(text);
                if (port < 1024 || port > 65535) {
                    throw new IllegalArgumentException("Port must be between 1024 and 65535");
                }
                source = new WifiSource(port);
            }
            source.start(monitor);
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            sourceLabel.setText("Source: " + source.getName() + (source.isSimulated() ? "  [SIMULATED DATA]" : ""));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "The Wi-Fi port must be a number, for example 5050");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not start " + source.getName() + ":\n" + ex.getMessage());
            source = null;
        }
    }

    private void stopSource() {
        if (source != null && source.isRunning()) {
            source.stop();
        }
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        sourceLabel.setText("Source: stopped");
    }

    // ---------------------------------------------------------------- helmets tab

    private JPanel buildHelmetsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(cardsPanel), BorderLayout.CENTER);
        panel.add(trendPanel, BorderLayout.SOUTH);
        cardsPanel.setPreferredSize(new java.awt.Dimension(720, 190));
        return panel;
    }

    private void refreshHelmets() {
        long now = System.currentTimeMillis();
        ArrayList<DeviceStatus> devices = monitor.getDevices();
        while (cards.size() < devices.size()) {
            DeviceCard card = new DeviceCard();
            cards.add(card);
            cardsPanel.add(card);
            cardsPanel.revalidate();
        }
        for (int i = 0; i < devices.size(); i++) {
            cards.get(i).show(devices.get(i), monitor.getThresholds(), now);
        }
        trendPanel.setData(devices, monitor.getThresholds().getMaxTemperatureC());
        if (source != null && !source.isRunning() && stopButton.isEnabled()) {
            // the source ended by itself (replay finished or connection closed)
            stopSource();
        }
    }

    // ---------------------------------------------------------------- incidents tab

    private JPanel buildIncidentsTab() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup severityGroup = new ButtonGroup();
        severityGroup.add(allSeverity);
        severityGroup.add(highSeverity);
        severityGroup.add(mediumSeverity);
        severityGroup.add(lowSeverity);
        filters.add(openOnlyBox);
        filters.add(new JLabel("  Severity:"));
        filters.add(allSeverity);
        filters.add(highSeverity);
        filters.add(mediumSeverity);
        filters.add(lowSeverity);
        filters.add(new JLabel("  Device:"));
        filters.add(deviceFilterField);
        panel.add(filters, BorderLayout.NORTH);

        // One listener object shared by all filter controls.
        ActionListener filterListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshIncidents();
            }
        };
        openOnlyBox.addActionListener(filterListener);
        allSeverity.addActionListener(filterListener);
        highSeverity.addActionListener(filterListener);
        mediumSeverity.addActionListener(filterListener);
        lowSeverity.addActionListener(filterListener);
        deviceFilterField.addActionListener(filterListener);

        incidentArea.setEditable(false);
        incidentArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        panel.add(new JScrollPane(incidentArea), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton ackButton = new JButton("Acknowledge");
        JButton resolveButton = new JButton("Resolve");
        JButton noteButton = new JButton("Add note");
        JButton historyButton = new JButton("History");
        JButton exportButton = new JButton("Export CSV");
        actions.add(new JLabel("Incident #"));
        actions.add(idField);
        actions.add(new JLabel("Note:"));
        actions.add(noteField);
        actions.add(ackButton);
        actions.add(resolveButton);
        actions.add(noteButton);
        actions.add(historyButton);
        actions.add(exportButton);
        panel.add(actions, BorderLayout.SOUTH);

        ackButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                incidentAction("ack");
            }
        });
        resolveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                incidentAction("resolve");
            }
        });
        noteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                incidentAction("note");
            }
        });
        historyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showHistory();
            }
        });
        exportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exportCsv();
            }
        });
        return panel;
    }

    private int readIncidentId() {
        String text = idField.getText().trim();
        if (text.startsWith("#")) {
            text = text.substring(1);
        }
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Enter the incident number first");
        }
        return Integer.parseInt(text);
    }

    private void incidentAction(String action) {
        try {
            int id = readIncidentId();
            String note = noteField.getText();
            long now = System.currentTimeMillis();
            if (action.equals("ack")) {
                monitor.acknowledge(id, note, now);
            } else if (action.equals("resolve")) {
                monitor.resolve(id, note, now);
            } else {
                monitor.addNote(id, note, now);
            }
            noteField.setText("");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "The incident number must be a whole number");
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        } catch (InvalidActionException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        } catch (StorageException e) {
            JOptionPane.showMessageDialog(this, "The change is shown but was NOT saved:\n" + e.getMessage());
        }
    }

    private void showHistory() {
        try {
            Incident incident = monitor.getIncident(readIncidentId());
            String text = incident.summary() + "\nSource message: " + incident.getSourceId() + "\n\n";
            for (String entry : incident.getHistory()) {
                text = text + entry + "\n";
            }
            JOptionPane.showMessageDialog(this, text, "Incident history", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "The incident number must be a whole number");
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        } catch (InvalidActionException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private void exportCsv() {
        File file = new File("data", "incidents-export.csv");
        try {
            file.getParentFile().mkdirs();
            CsvExporter.export(monitor.getIncidents(), file);
            JOptionPane.showMessageDialog(this, "Exported " + monitor.getIncidents().size()
                    + " incidents to\n" + file.getAbsolutePath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage());
        }
    }

    private String selectedSeverity() {
        if (highSeverity.isSelected()) {
            return Incident.HIGH;
        } else if (mediumSeverity.isSelected()) {
            return Incident.MEDIUM;
        } else if (lowSeverity.isSelected()) {
            return Incident.LOW;
        }
        return "ALL";
    }

    private void refreshIncidents() {
        ArrayList<Incident> list = monitor.filterIncidents(openOnlyBox.isSelected(), selectedSeverity(),
                deviceFilterField.getText());
        StringBuilder text = new StringBuilder();
        for (Incident i : list) {
            text.append(i.summary()).append("\n");
        }
        if (list.isEmpty()) {
            text.append("No incidents match the filter.");
        }
        incidentArea.setText(text.toString());
        incidentArea.setCaretPosition(0);
    }

    // ---------------------------------------------------------------- simulation tab

    private JPanel buildSimulationTab() {
        JPanel panel = new JPanel(new GridLayout(4, 1));

        JPanel note = new JPanel(new FlowLayout(FlowLayout.LEFT));
        note.add(new JLabel("Select Simulation as the data source and press Start. "
                + "All data here is SIMULATED. The scenario changes SIM-01 only."));
        panel.add(note);

        JPanel scenarios = new JPanel(new FlowLayout(FlowLayout.LEFT));
        scenarios.add(new JLabel("Scenario for SIM-01:"));
        String[] names = {SimulatedSource.NORMAL, SimulatedSource.HEAT, SimulatedSource.HUMID,
                SimulatedSource.FAULT, SimulatedSource.SILENT};
        scenarioRadios = new JRadioButton[names.length];
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < names.length; i++) {
            final String scenario = names[i];
            scenarioRadios[i] = new JRadioButton(scenario, i == 0);
            group.add(scenarioRadios[i]);
            scenarios.add(scenarioRadios[i]);
            scenarioRadios[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    SimulatedSource sim = runningSimulation();
                    if (sim != null) {
                        sim.setScenario(scenario);
                    }
                }
            });
        }
        panel.add(scenarios);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (int i = 0; i < 3; i++) {
            final int helmet = i;
            JButton sos = new JButton("SOS on SIM-0" + (i + 1));
            sos.setForeground(Color.RED);
            sos.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    SimulatedSource sim = runningSimulation();
                    if (sim != null) {
                        sim.pressSos(helmet);
                    }
                }
            });
            buttons.add(sos);
        }
        JButton motion = new JButton("Sudden motion on SIM-01");
        motion.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SimulatedSource sim = runningSimulation();
                if (sim != null) {
                    sim.suddenMotion(0);
                }
            }
        });
        buttons.add(motion);
        panel.add(buttons);
        return panel;
    }

    /** Returns the running simulator, or shows a message and returns null. */
    private SimulatedSource runningSimulation() {
        if (source instanceof SimulatedSource && source.isRunning()) {
            SimulatedSource sim = (SimulatedSource) source;
            for (JRadioButton r : scenarioRadios) {
                if (r.isSelected()) {
                    sim.setScenario(r.getText());
                }
            }
            return sim;
        }
        JOptionPane.showMessageDialog(this, "Start the Simulation data source first.");
        return null;
    }

    // ---------------------------------------------------------------- settings tab

    private JPanel buildSettingsTab() {
        Thresholds t = monitor.getThresholds();
        tempField.setText(String.valueOf(t.getMaxTemperatureC()));
        humField.setText(String.valueOf(t.getMaxHumidityPct()));
        sustainField.setText(String.valueOf(t.getSustainedSamples()));
        staleField.setText(String.valueOf(t.getStaleSeconds()));

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        form.add(new JLabel("Warning temperature above (C):"));
        form.add(tempField);
        form.add(new JLabel("Warning humidity above (%):"));
        form.add(humField);
        form.add(new JLabel("Readings in a row before an incident:"));
        form.add(sustainField);
        form.add(new JLabel("Stale after (seconds without data):"));
        form.add(staleField);
        JButton apply = new JButton("Apply");
        form.add(new JLabel("Demonstration values, not certified safety limits."));
        form.add(apply);

        apply.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                applySettings();
            }
        });

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(form);
        return panel;
    }

    private void applySettings() {
        Thresholds t = monitor.getThresholds();
        try {
            double temp = Double.parseDouble(tempField.getText().trim());
            double hum = Double.parseDouble(humField.getText().trim());
            int sustain = Integer.parseInt(sustainField.getText().trim());
            int stale = Integer.parseInt(staleField.getText().trim());
            t.setMaxTemperatureC(temp);
            t.setMaxHumidityPct(hum);
            t.setSustainedSamples(sustain);
            t.setStaleSeconds(stale);
            configVersion++;
            String message = "Settings applied.";
            // Hardware helmets get the new limits so their local alarm matches.
            if (source != null && source.isRunning() && !source.isSimulated()) {
                String command = "C," + configVersion + "," + temp + "," + hum;
                message = message + (source.send(command) ? "\nSent to helmet: " + command
                        : "\nCould not send settings to the helmet.");
            }
            JOptionPane.showMessageDialog(this, message);
            refreshAll();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please type numbers only (whole numbers for the last two).");
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    // ---------------------------------------------------------------- refresh

    public void monitorChanged() {
        refreshAll();
        int count = monitor.getIncidents().size();
        if (count > lastIncidentCount) {
            Toolkit.getDefaultToolkit().beep();
        }
        lastIncidentCount = count;
    }

    private void refreshAll() {
        refreshHelmets();
        refreshIncidents();

        StringBuilder text = new StringBuilder();
        for (String line : monitor.getLog()) {
            text.append(line).append("\n");
        }
        logArea.setText(text.toString());

        String problem = monitor.getStorageProblem();
        if (problem != null) {
            storageLabel.setText("STORAGE ERROR: " + problem);
            storageLabel.setForeground(Color.RED);
        } else {
            storageLabel.setText("Storage: " + monitor.getStore().getDescription()
                    + "   |   rejected: " + monitor.getInvalidCount()
                    + "   duplicates: " + monitor.getDuplicateCount());
            storageLabel.setForeground(Color.BLACK);
        }

        int high = monitor.filterIncidents(true, Incident.HIGH, "").size();
        int open = monitor.countOpen();
        if (high > 0) {
            alertLabel.setText("  " + high + " HIGH incident(s) need attention  (" + open + " open in total)");
            alertLabel.setBackground(new Color(255, 120, 120));
        } else if (open > 0) {
            alertLabel.setText("  " + open + " open incident(s)");
            alertLabel.setBackground(DeviceCard.WARNING_COLOR);
        } else {
            alertLabel.setText("  No open incidents");
            alertLabel.setBackground(DeviceCard.OK_COLOR);
        }
    }
}
