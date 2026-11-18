package com.siteguard.ui;

import com.siteguard.ui.Presentation.Tone;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.siteguard.domain.StoredReading;
import javax.swing.JPanel;
import javax.swing.Scrollable;

/**
 * Stacked line charts sharing one time axis. Each row is its own axis (never overlaid). A null value is a sensor
 * fault and draws as a dashed gap. The vertical line marks the incident time. Labelled SIMULATED.
 */
final class ChartPanel extends JPanel implements Scrollable {
    /** One row. values[i] pairs with times[i]; null means a fault gap. */
    record Row(String title, String unit, List<Double> values, Tone tone) { }

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private final List<Instant> times;
    private final List<Row> rows;
    private final Instant marker;
    private final String caption;

    ChartPanel(String caption, List<Instant> times, List<Row> rows, Instant marker) {
        this.caption = caption;
        this.times = List.copyOf(times);
        this.rows = List.copyOf(rows);
        this.marker = marker;
        setPreferredSize(new Dimension(560, 150 + 120 * Math.max(1, rows.size())));
        setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setFont(getFont().deriveFont(12f));
            g2.setColor(Theme.text(Tone.NEUTRAL));
            g2.drawString(caption, 8, 16);
            if (times.isEmpty()) {
                g2.drawString("No stored readings in this window (SIMULATED).", 8, 40);
                return;
            }
            int top = 28;
            int bandHeight = (getHeight() - top - 34) / Math.max(1, rows.size());
            for (int i = 0; i < rows.size(); i++) {
                paintRow(g2, rows.get(i), top + i * bandHeight, bandHeight);
            }
            paintTimeAxis(g2, getHeight() - 8);
        } finally {
            g2.dispose();
        }
    }

    private void paintRow(Graphics2D g2, Row row, int y, int height) {
        int left = 130;
        int right = getWidth() - 12;
        int plotTop = y + 18;
        int plotBottom = y + height - 8;
        g2.setColor(Theme.text(row.tone()));
        g2.drawString(row.title() + " (" + row.unit() + ")", 8, y + 13);
        g2.setColor(Theme.secondaryText());
        Double min = null;
        Double max = null;
        for (Double v : row.values()) {
            if (v == null) continue;
            min = min == null ? v : Math.min(min, v);
            max = max == null ? v : Math.max(max, v);
        }
        if (min == null) {
            min = 0.0;
            max = 1.0;
        }
        if (max - min < 1e-6) max = min + 1;
        g2.drawString(format(max), 8, plotTop + 10);
        g2.drawString(format(min), 8, plotBottom);
        g2.drawString("gap = fault", 8, plotTop + 26);
        g2.setColor(Theme.secondaryText());
        g2.drawRect(left, plotTop, right - left, plotBottom - plotTop);

        long t0 = times.get(0).toEpochMilli();
        long t1 = times.get(times.size() - 1).toEpochMilli();
        if (t1 == t0) t1 = t0 + 1;
        Color color = Theme.text(row.tone());
        g2.setColor(color);
        Stroke solid = new BasicStroke(1.6f);
        Stroke dashed = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {3f, 3f}, 0f);
        Integer prevX = null;
        Integer prevY = null;
        for (int i = 0; i < times.size(); i++) {
            Double v = row.values().get(i);
            int x = left + (int) Math.round((double) (times.get(i).toEpochMilli() - t0) / (t1 - t0) * (right - left));
            if (v == null) {
                g2.setStroke(dashed);
                g2.setColor(Theme.secondaryText());
                g2.drawLine(x, plotBottom, x, plotBottom - 4);
                prevX = null;
                prevY = null;
                g2.setColor(color);
                continue;
            }
            int py = plotBottom - (int) Math.round((v - min) / (max - min) * (plotBottom - plotTop));
            if (prevX != null) {
                g2.setStroke(solid);
                g2.drawLine(prevX, prevY, x, py);
            }
            prevX = x;
            prevY = py;
        }
        if (marker != null) {
            long mt = marker.toEpochMilli();
            if (mt >= t0 && mt <= t1) {
                int mx = left + (int) Math.round((double) (mt - t0) / (t1 - t0) * (right - left));
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(Theme.text(Tone.SOS_HIGH));
                g2.drawLine(mx, plotTop, mx, plotBottom);
            }
        }
    }

    private void paintTimeAxis(Graphics2D g2, int baseline) {
        g2.setColor(Theme.secondaryText());
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
        g2.drawString(CLOCK.format(times.get(0)), 130, baseline);
        String end = CLOCK.format(times.get(times.size() - 1));
        int w = g2.getFontMetrics().stringWidth(end);
        g2.drawString(end, getWidth() - 12 - w, baseline);
        if (marker != null) {
            String m = "red line = incident " + CLOCK.format(marker);
            g2.setColor(Theme.text(Tone.SOS_HIGH));
            g2.drawString(m, 130 + (getWidth() - 142) / 2 - g2.getFontMetrics().stringWidth(m) / 2, baseline);
        }
    }

    private static String format(double v) {
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    /** Three rows from stored readings: air temp, humidity and acceleration magnitude. Faults become gaps. */
    static ChartPanel forReadings(String caption, List<StoredReading> readings, Instant marker) {
        List<Instant> times = readings.stream().map(StoredReading::receivedAt).toList();
        Row temp = new Row("Air temp near helmet", "°C",
                readings.stream().map(StoredReading::temperatureC).toList(), Tone.ONLINE);
        Row humidity = new Row("Humidity", "%",
                readings.stream().map(StoredReading::humidityPct).toList(), Tone.NO_DATA);
        Row accel = new Row("Acceleration magnitude", "sensor units",
                readings.stream().map(r -> r.acceleration() == null ? null
                        : Math.sqrt(r.acceleration().x() * r.acceleration().x()
                        + r.acceleration().y() * r.acceleration().y()
                        + r.acceleration().z() * r.acceleration().z())).toList(), Tone.WARNING);
        return new ChartPanel(caption, times, List.of(temp, humidity, accel), marker);
    }

    /** Fills the viewport in both directions, so the time axis stays visible inside a scroll pane. */
    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(java.awt.Rectangle r, int orientation, int direction) { return 10; }
    @Override public int getScrollableBlockIncrement(java.awt.Rectangle r, int orientation, int direction) { return 40; }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return true; }
}
