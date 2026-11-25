package siteguard.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;

import javax.swing.JPanel;

import siteguard.service.DeviceStatus;

/**
 * Draws a simple line chart of the last 60 temperature readings of every helmet.
 * paintComponent is overridden from JPanel (Chapter 10: overriding).
 */
public class TrendPanel extends JPanel {

    private static final Color[] LINE_COLORS = {Color.BLUE, Color.MAGENTA, new Color(0, 140, 0),
            Color.ORANGE, Color.CYAN, Color.BLACK};
    private static final double MIN_C = 10;
    private static final double MAX_C = 50;

    private ArrayList<DeviceStatus> devices = new ArrayList<DeviceStatus>();
    private double limit = 35;

    public TrendPanel() {
        setPreferredSize(new Dimension(700, 220));
        setBackground(Color.WHITE);
    }

    public void setData(ArrayList<DeviceStatus> devices, double limit) {
        this.devices = devices;
        this.limit = limit;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int left = 45;
        int top = 20;
        int width = getWidth() - left - 20;
        int height = getHeight() - top - 30;

        g.setColor(Color.BLACK);
        g.drawString("Temperature trend (C), last " + DeviceStatus.HISTORY_SIZE + " readings", left, 14);
        g.drawRect(left, top, width, height);
        for (int c = 10; c <= 50; c += 10) {
            int y = toY(c, top, height);
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(left, y, left + width, y);
            g.setColor(Color.BLACK);
            g.drawString(String.valueOf(c), 15, y + 5);
        }

        int limitY = toY(limit, top, height);
        g.setColor(Color.RED);
        g.drawLine(left, limitY, left + width, limitY);
        g.drawString("limit " + limit, left + width - 70, limitY - 3);

        for (int d = 0; d < devices.size(); d++) {
            ArrayList<Double> values = devices.get(d).getTemperatureHistory();
            g.setColor(LINE_COLORS[d % LINE_COLORS.length]);
            g.drawString(devices.get(d).getDeviceId(), left + 10 + d * 90, top + height + 20);
            for (int i = 1; i < values.size(); i++) {
                int x1 = left + (i - 1) * width / (DeviceStatus.HISTORY_SIZE - 1);
                int x2 = left + i * width / (DeviceStatus.HISTORY_SIZE - 1);
                g.drawLine(x1, toY(values.get(i - 1), top, height), x2, toY(values.get(i), top, height));
            }
        }
    }

    private int toY(double value, int top, int height) {
        if (value < MIN_C) {
            value = MIN_C;
        }
        if (value > MAX_C) {
            value = MAX_C;
        }
        return top + (int) ((MAX_C - value) / (MAX_C - MIN_C) * height);
    }
}
