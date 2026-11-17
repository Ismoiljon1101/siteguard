package com.siteguard.bootstrap;

import com.siteguard.ui.DashboardFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Launches the provisional simulation dashboard. */
public final class SiteGuardApp {
    private SiteGuardApp() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Swing's default look and feel remains usable.
            }
            new DashboardFrame(new SimulationController()).showDashboard();
        });
    }
}
