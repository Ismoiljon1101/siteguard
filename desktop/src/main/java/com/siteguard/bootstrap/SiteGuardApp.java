package com.siteguard.bootstrap;

import com.siteguard.ui.MainWindow;
import com.siteguard.ui.Theme;
import java.nio.file.Path;
import javax.swing.SwingUtilities;

/**
 * Launches SiteGuard (simulation only). Options: -Dsiteguard.dark=true for the dark theme,
 * -Dsiteguard.dataDir=DIR (default data), -Dsiteguard.screenshotDir=DIR for QA screenshots.
 */
public final class SiteGuardApp {
    private SiteGuardApp() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install(Boolean.getBoolean("siteguard.dark"));
            MainWindow window = new MainWindow();
            window.setVisible(true);
            window.openData(Path.of(System.getProperty("siteguard.dataDir", "data")));
        });
    }
}
