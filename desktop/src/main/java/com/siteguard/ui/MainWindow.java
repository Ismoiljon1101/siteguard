package com.siteguard.ui;

import com.siteguard.bootstrap.AppContext;
import com.siteguard.domain.Operator;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.Timer;

/**
 * Top-level window: header, login card, then the overview, people and shift screens after sign-in. Owns AppContext.
 * QA auto-run: with -Dsiteguard.screenshotDir and -Dsiteguard.autoLogin=LOGIN, the app signs in, walks the screens
 * and writes PNGs (overview, incident-detail, shift-report, people, worker-detail).
 */
public final class MainWindow extends JFrame {
    private static final String AUTO_LOGIN = "siteguard.autoLogin";
    private static final String AUTO_PASSWORD = "siteguard.autoPassword";

    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final LoginPanel login = new LoginPanel(this::attemptLogin);
    private final JPanel header = new JPanel(new BorderLayout(8, 0));
    private final JLabel siteLabel = new JLabel(" ");
    private final JLabel userLabel = new JLabel(" ");
    private final JLabel clockLabel = new JLabel(" ");
    private final JLabel simBadge = new JLabel(" " + Presentation.SIMULATED_BADGE + " ");
    private final JToggleButton themeToggle = new JToggleButton("Dark theme");
    private final JButton logout = new JButton("Log out");
    private final JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
    private final Timer clock = new Timer(1000, e -> clockLabel.setText(Presentation.clock(Instant.now())));
    private AppContext ctx;
    private Operator operator;
    private DashboardPanel dashboard;
    private PeoplePanel people;
    private ShiftReportPanel shift;
    private String screen = "login";
    private boolean loginShot;

    public MainWindow() {
        super("SiteGuard (SIMULATION ONLY)");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1280, 720));
        setSize(1366, 768);
        setLocationRelativeTo(null);

        header.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        siteLabel.setFont(siteLabel.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        left.add(siteLabel);
        left.add(userLabel);
        simBadge.setOpaque(true);
        simBadge.setBackground(Theme.text(Presentation.Tone.WARNING));
        simBadge.setForeground(Theme.stripText());
        simBadge.setVisible(false);
        left.add(simBadge);
        nav.setOpaque(false);
        nav.add(navButton("Overview", "dashboard"));
        nav.add(navButton("People and helmets (Alt+3)", "people"));
        nav.add(navButton("Shift report (Alt+4)", "shift"));
        nav.add(navButton("Simulation (Alt+5)", null));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(clockLabel);
        themeToggle.addActionListener(e -> Theme.switchTo(themeToggle.isSelected()));
        themeToggle.setSelected(Theme.dark());
        right.add(themeToggle);
        logout.addActionListener(e -> logOut());
        right.add(logout);
        header.add(left, BorderLayout.WEST);
        header.add(nav, BorderLayout.CENTER);
        header.add(right, BorderLayout.EAST);
        header.setVisible(false);

        body.add(login, "login");
        setLayout(new BorderLayout());
        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        cards.show(body, "login");

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                closeApp();
            }
        });
        bindGlobalKeys();
    }

    private JButton navButton(String text, String target) {
        JButton b = new JButton(text);
        b.addActionListener(e -> {
            if (target == null) {
                if (dashboard != null) dashboard.openSimulation();
            } else {
                showScreen(target);
            }
        });
        return b;
    }

    /** Opens the database off the EDT, then enables the login form. Auto-login only when QA asks for it. */
    public void openData(Path dataDir) {
        String autoLogin = System.getProperty(AUTO_LOGIN);
        boolean autoRun = System.getProperty(ScreenshotHook.PROPERTY) != null && autoLogin != null && !autoLogin.isBlank();
        Background.run(() -> {
            AppContext opened = AppContext.open(dataDir);
            char[] pw = autoRun ? autoPassword(dataDir, autoLogin) : null;
            return new Opened(opened, pw);
        }, opened -> {
            ctx = opened.ctx();
            ctx.simulation().start();
            login.setReady(true);
            if (!loginShot) {
                loginShot = true;
                ScreenshotHook.capture("login", login);
            }
            if (opened.password() != null) attemptLogin(autoLogin, opened.password(), true);
        }, err -> {
            System.err.println("SiteGuard could not open the database: " + err);
            login.setStartupError("Could not open the database. " + Presentation.errorText(err));
        });
    }

    private record Opened(AppContext ctx, char[] password) { }

    /** Password from -Dsiteguard.autoPassword, else the matching line of data/demo-credentials.txt. */
    private static char[] autoPassword(Path dataDir, String loginId) {
        String direct = System.getProperty(AUTO_PASSWORD);
        if (direct != null && !direct.isBlank()) return direct.toCharArray();
        try {
            for (String line : Files.readAllLines(dataDir.resolve(AppContext.CREDENTIALS_FILE))) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 3 && parts[0].equals(loginId)) return parts[2].toCharArray();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read demo credentials for auto-login", e);
        }
        return null;
    }

    private void attemptLogin(String loginId, char[] password) {
        attemptLogin(loginId, password, false);
    }

    private void attemptLogin(String loginId, char[] password, boolean tour) {
        if (ctx == null) return;
        Background.run(() -> ctx.auth().login(loginId, password), op -> {
            enterDashboard(op);
            if (tour) runTour();
        }, err -> login.showError(Presentation.errorText(err)));
    }

    private void enterDashboard(Operator op) {
        operator = op;
        login.clear();
        userLabel.setText(op.displayName() + "  (" + roleText(op) + ")");
        dashboard = new DashboardPanel(ctx, op, this::onSnapshot);
        people = new PeoplePanel(ctx, op);
        shift = new ShiftReportPanel(ctx, op);
        body.add(dashboard, "dashboard");
        body.add(people, "people");
        body.add(shift, "shift");
        header.setVisible(true);
        clock.start();
        dashboard.start();
        showScreen("dashboard");
    }

    private void showScreen(String name) {
        screen = name;
        if (dashboard == null) return;
        cards.show(body, name);
        if ("people".equals(name)) people.refresh();
        if ("shift".equals(name)) shift.refresh();
    }

    private void onSnapshot(DashboardPanel.Snapshot snap) {
        siteLabel.setText(snap.overview().siteName());
        boolean simulated = snap.simRunning() || snap.overview().simulatedHelmets() > 0;
        simBadge.setVisible(simulated);
    }

    private void logOut() {
        if (dashboard != null) {
            dashboard.stop();
            body.remove(dashboard);
            body.remove(people);
            body.remove(shift);
            dashboard = null;
            people = null;
            shift = null;
        }
        clock.stop();
        header.setVisible(false);
        siteLabel.setText(" ");
        userLabel.setText(" ");
        simBadge.setVisible(false);
        operator = null;
        screen = "login";
        cards.show(body, "login");
        login.clear();
    }

    private void closeApp() {
        if (dashboard != null) dashboard.stop();
        clock.stop();
        if (ctx != null) ctx.close();
        dispose();
        System.exit(0);
    }

    private void bindGlobalKeys() {
        JComponent root = getRootPane();
        bind(root, "ctrl F", "search", () -> { if (dashboard != null) dashboard.focusSearch(); });
        bind(root, "F5", "refresh", () -> { if (dashboard != null) dashboard.refreshNow(); });
        bind(root, "alt 1", "overview", () -> showScreen("dashboard"));
        bind(root, "alt 2", "incidents", () -> { showScreen("dashboard"); if (dashboard != null) dashboard.focusIncidents(); });
        bind(root, "alt 3", "people", () -> showScreen("people"));
        bind(root, "alt 4", "shift", () -> showScreen("shift"));
        bind(root, "alt 5", "simulation", () -> { if (dashboard != null) dashboard.openSimulation(); });
    }

    private static void bind(JComponent root, String keys, String name, Runnable action) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keys), name);
        root.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    // ---- QA screen tour (only with -Dsiteguard.screenshotDir and -Dsiteguard.autoLogin) -----------------------

    private record Step(int delayMs, Runnable action) { }

    private void runTour() {
        List<Step> steps = List.of(
                new Step(3000, () -> ScreenshotHook.capture("overview", dashboard)),
                new Step(500, () -> { if (dashboard != null) dashboard.openFirstSos(); }),
                new Step(2500, () -> closeDialogs("Incident")),
                new Step(500, () -> showScreen("shift")),
                new Step(2000, () -> ScreenshotHook.capture("shift-report", shift)),
                new Step(500, () -> showScreen("people")),
                new Step(2000, () -> ScreenshotHook.capture("people", people)),
                new Step(500, () -> { if (people != null) people.openFirstWorker(); }),
                new Step(2500, () -> closeDialogs("Worker")),
                new Step(500, () -> showScreen("dashboard")));
        runSteps(steps, 0);
    }

    private void runSteps(List<Step> steps, int index) {
        if (index >= steps.size()) return;
        Step step = steps.get(index);
        Timer t = new Timer(step.delayMs(), e -> {
            step.action().run();
            runSteps(steps, index + 1);
        });
        t.setRepeats(false);
        t.start();
    }

    private static void closeDialogs(String titlePrefix) {
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog d && d.getTitle() != null && d.getTitle().startsWith(titlePrefix)) d.dispose();
        }
    }

    private static String roleText(Operator operator) {
        return switch (operator.role()) {
            case SAFETY_DIRECTOR -> "Safety director";
            case SITE_MANAGER -> "Site manager";
            case SUPERVISOR -> "Supervisor";
        };
    }
}
