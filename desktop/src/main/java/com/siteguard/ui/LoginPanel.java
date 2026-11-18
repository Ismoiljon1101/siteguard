package com.siteguard.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/** Centred 360 px login card. Enter in either field submits. Errors are generic, never say which field was wrong. */
final class LoginPanel extends JPanel {
    private final BiConsumer<String, char[]> onSubmit;
    private final JTextField loginId = new JTextField(20);
    private final JPasswordField password = new JPasswordField(20);
    private final JButton signIn = new JButton("Sign in");
    private final JLabel message = new JLabel(" ");
    private final JLabel startup = new JLabel("Opening database...");

    LoginPanel(BiConsumer<String, char[]> onSubmit) {
        super(new BorderLayout());
        this.onSubmit = onSubmit;
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("SiteGuard sign in");
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 18f));
        JLabel sim = new JLabel("SIM  Simulated data only. No hardware is connected.");
        sim.setForeground(Theme.secondaryText());

        JPanel card = new JPanel(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(4, 0, 4, 0);

        card.add(title, c);
        c.gridy++;
        card.add(sim, c);
        c.gridy++;
        card.add(new JLabel("User name"), c);
        c.gridy++;
        card.add(loginId, c);
        c.gridy++;
        card.add(new JLabel("Password"), c);
        c.gridy++;
        card.add(password, c);
        c.gridy++;
        message.setForeground(Theme.text(Presentation.Tone.SOS_HIGH));
        card.add(message, c);
        c.gridy++;
        signIn.addActionListener(e -> submit());
        card.add(signIn, c);
        c.gridy++;
        startup.setForeground(Theme.secondaryText());
        card.add(startup, c);

        loginId.addActionListener(e -> submit());
        password.addActionListener(e -> submit());

        card.setPreferredSize(new Dimension(360, card.getPreferredSize().height));
        JPanel centre = new JPanel(new GridBagLayout());
        centre.add(card);
        add(centre, BorderLayout.CENTER);
        setReady(false);
    }

    /** Enables the form once the database is open. */
    void setReady(boolean ready) {
        loginId.setEnabled(ready);
        password.setEnabled(ready);
        signIn.setEnabled(ready);
        startup.setText(ready ? "Ready." : "Opening database...");
        if (ready) loginId.requestFocusInWindow();
    }

    void setStartupError(String text) {
        startup.setText(text);
        startup.setForeground(Theme.text(Presentation.Tone.SOS_HIGH));
    }

    void showError(String text) {
        message.setText(text);
        password.setText("");
        signIn.setEnabled(true);
        password.requestFocusInWindow();
    }

    void clear() {
        loginId.setText("");
        password.setText("");
        message.setText(" ");
        signIn.setEnabled(true);
        loginId.requestFocusInWindow();
    }

    private void submit() {
        if (!signIn.isEnabled()) return;
        char[] pw = password.getPassword();
        message.setText(" ");
        signIn.setEnabled(false);
        onSubmit.accept(loginId.getText().strip(), pw);
    }
}
