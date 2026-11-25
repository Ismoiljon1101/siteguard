package siteguard.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import siteguard.model.Reading;
import siteguard.rules.Thresholds;
import siteguard.service.DeviceStatus;

/**
 * A status card for one helmet: identity, readings, age of the latest reading and a coloured status.
 */
public class DeviceCard extends JPanel {

    public static final Color OK_COLOR = new Color(200, 240, 200);
    public static final Color WARNING_COLOR = new Color(255, 220, 150);
    public static final Color STALE_COLOR = new Color(210, 210, 210);
    public static final Color FAULT_COLOR = new Color(255, 190, 190);

    private JLabel nameLabel = new JLabel();
    private JLabel tempLabel = new JLabel();
    private JLabel humLabel = new JLabel();
    private JLabel accelLabel = new JLabel();
    private JLabel ageLabel = new JLabel();
    private JLabel sourceLabel = new JLabel();
    private JLabel statusLabel = new JLabel();

    public DeviceCard() {
        setLayout(new GridLayout(7, 1));
        setPreferredSize(new Dimension(220, 170));
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        add(nameLabel);
        add(tempLabel);
        add(humLabel);
        add(accelLabel);
        add(ageLabel);
        add(sourceLabel);
        add(statusLabel);
    }

    public void show(DeviceStatus d, Thresholds t, long nowMs) {
        String label = d.isSimulated() ? "  [SIMULATED]" : "  [HARDWARE]";
        nameLabel.setText(" " + d.getDeviceId() + label);
        Reading r = d.getLatest();
        String status = "OK";
        Color color = OK_COLOR;

        if (r == null) {
            tempLabel.setText(" Temperature: no reading yet");
            humLabel.setText(" Humidity: -");
            accelLabel.setText(" Acceleration: -");
        } else {
            if (r.hasEnvironment()) {
                tempLabel.setText(" Temperature: " + r.getTemperatureC() + " C");
                humLabel.setText(" Humidity: " + r.getHumidityPct() + " %");
                if (r.getTemperatureC() > t.getMaxTemperatureC() || r.getHumidityPct() > t.getMaxHumidityPct()) {
                    status = "WARNING";
                    color = WARNING_COLOR;
                }
            } else {
                tempLabel.setText(" Temperature: SENSOR FAULT");
                humLabel.setText(" Humidity: SENSOR FAULT");
                status = "SENSOR FAULT";
                color = FAULT_COLOR;
            }
            if (r.hasMotion()) {
                accelLabel.setText(" Acceleration: " + Math.round(r.getAccelMagnitude() * 10) / 10.0 + " m/s2");
            } else {
                accelLabel.setText(" Acceleration: SENSOR FAULT");
                status = "SENSOR FAULT";
                color = FAULT_COLOR;
            }
        }

        long age = d.getAgeSeconds(nowMs);
        ageLabel.setText(" Last message: " + (age < 0 ? "never" : age + " s ago"));
        sourceLabel.setText(" Source: " + d.getSourceName());
        if (d.isStale(nowMs, t.getStaleSeconds())) {
            status = "STALE - no data";
            color = STALE_COLOR;
        }
        statusLabel.setText(" " + status);
        setBackground(color);
    }
}
