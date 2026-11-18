package com.siteguard.ui;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/**
 * QA hook. Start the app with -Dsiteguard.screenshotDir=DIR and it writes PNGs of the login, overview and
 * incident detail screens by painting the components (no Robot, no screen capture). Each name is written once.
 */
public final class ScreenshotHook {
    public static final String PROPERTY = "siteguard.screenshotDir";
    private static final Set<String> taken = new HashSet<>();

    private ScreenshotHook() { }

    /** Call on the Swing thread. Does nothing unless the property is set. */
    public static void capture(String name, Component component) {
        String dir = System.getProperty(PROPERTY);
        if (dir == null || dir.isBlank()) return;
        String shot = Boolean.getBoolean("siteguard.dark") ? name + "-dark" : name;
        if (!taken.add(shot)) return;
        Path target = Path.of(dir);
        SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> write(target, shot, component)));
    }

    private static void write(Path dir, String name, Component component) {
        int w = component.getWidth();
        int h = component.getHeight();
        if (w <= 0 || h <= 0) {
            System.out.println("screenshot skipped (not laid out): " + name);
            return;
        }
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            component.printAll(g);
        } finally {
            g.dispose();
        }
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(name + ".png");
            ImageIO.write(image, "png", file.toFile());
            System.out.println("screenshot saved: " + file.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("screenshot failed: " + name + ": " + e.getMessage());
        }
    }
}
