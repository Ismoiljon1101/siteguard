package com.siteguard.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.siteguard.ui.Presentation.Tone;
import java.awt.Color;
import java.awt.Window;
import java.util.Collections;
import javax.swing.SwingUtilities;

/** FlatLaf setup and the colour tokens from docs/UI_SPEC.md section 2. Call on the Swing thread. */
public final class Theme {
    private static volatile boolean dark;

    private Theme() { }

    public static boolean dark() { return dark; }

    public static void install(boolean darkMode) {
        dark = darkMode;
        FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", darkMode ? "#7AA7FF" : "#1F5FBF"));
        if (darkMode) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
    }

    /** Switches theme and restyles every open window, including dialogs. */
    public static void switchTo(boolean darkMode) {
        install(darkMode);
        for (Window w : Window.getWindows()) SwingUtilities.updateComponentTreeUI(w);
    }

    public static Color accent() { return hex(dark ? "7AA7FF" : "1F5FBF"); }

    public static Color secondaryText() { return hex(dark ? "9AA4B1" : "5A6470"); }

    /** Text colour for a tone, on the panel surface. */
    public static Color text(Tone tone) {
        return switch (tone) {
            case ONLINE -> hex(dark ? "5FD38D" : "1B7F3B");
            case STALE, WARNING -> hex(dark ? "F2B84B" : "8A5A00");
            case OFFLINE -> hex(dark ? "A3ADBA" : "4A5260");
            case SENSOR_FAULT -> hex(dark ? "C9A0F0" : "7A3E9D");
            case NO_DATA -> hex(dark ? "8FB3E0" : "2F5D8C");
            case UNASSIGNED -> hex(dark ? "B8C2CC" : "3F4A56");
            case SOS_HIGH -> hex(dark ? "FF6B6B" : "B3001B");
            case ESCALATION_1 -> hex(dark ? "F97316" : "C2410C");
            case ESCALATION_2 -> hex(dark ? "FF5C5C" : "8B0000");
            case NEUTRAL -> secondaryText();
        };
    }

    /** Background for an alert strip. SOS and escalation tones have a fill; others use the panel colour. */
    public static Color fill(Tone tone) {
        return switch (tone) {
            case SOS_HIGH -> hex(dark ? "FF6B6B" : "B3001B");
            case ESCALATION_1 -> hex(dark ? "F97316" : "C2410C");
            case ESCALATION_2 -> hex(dark ? "FF5C5C" : "8B0000");
            default -> hex(dark ? "1E2228" : "FFFFFF");
        };
    }

    /** Text on an alert strip fill. */
    public static Color stripText() { return hex(dark ? "1E2228" : "FFFFFF"); }

    private static Color hex(String rgb) { return new Color(Integer.parseInt(rgb, 16)); }
}
