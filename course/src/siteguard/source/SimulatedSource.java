package siteguard.source;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import javax.swing.Timer;

/**
 * Makes fake helmet lines in Java, once per second, for three simulated helmets.
 * The scenario only changes SIM-01; SIM-02 and SIM-03 stay normal.
 */
public class SimulatedSource implements DataSource {

    public static final String NORMAL = "Normal";
    public static final String HEAT = "Heat warning";
    public static final String HUMID = "High humidity";
    public static final String FAULT = "Sensor fault";
    public static final String SILENT = "Connection lost";

    private String[] deviceIds = {"SIM-01", "SIM-02", "SIM-03"};
    private int[] seq = new int[3];
    private String bootId;
    private long startMs;
    private String scenario = NORMAL;
    private Random random = new Random();
    private Timer timer;
    private LineListener listener;

    public String getName() { return "Simulation"; }
    public boolean isSimulated() { return true; }
    public boolean isRunning() { return timer != null && timer.isRunning(); }

    public void start(LineListener l) {
        this.listener = l;
        bootId = "sim" + (System.currentTimeMillis() % 100000);
        startMs = System.currentTimeMillis();
        seq = new int[3];
        timer = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tick();
            }
        });
        timer.start();
        listener.sourceStatus(this, "started 3 simulated helmets (" + bootId + ")");
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
            listener.sourceStatus(this, "stopped");
        }
    }

    /** The simulator has no device to send to; receipts are shown in the log instead. */
    public boolean send(String line) {
        return false;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public String getScenario() {
        return scenario;
    }

    /** Simulates pressing the SOS button on one helmet. */
    public void pressSos(int helmet) {
        sendEvent(helmet, "SOS");
    }

    /** Simulates the helmet's own sudden-motion detection. */
    public void suddenMotion(int helmet) {
        sendEvent(helmet, "MOTION");
    }

    private void sendEvent(int helmet, String type) {
        if (!isRunning()) {
            return;
        }
        String line = "E," + deviceIds[helmet] + "," + bootId + "," + (seq[helmet]++) + "," + uptime() + ",1," + type;
        listener.lineReceived(line, this);
    }

    private void tick() {
        for (int i = 0; i < deviceIds.length; i++) {
            if (i == 0 && scenario.equals(SILENT)) {
                continue;
            }
            listener.lineReceived(makeReading(i), this);
        }
    }

    private String makeReading(int i) {
        double temp = 24 + random.nextDouble() * 4;
        double hum = 45 + random.nextDouble() * 15;
        if (i == 0 && scenario.equals(HEAT)) {
            temp = 37 + random.nextDouble() * 2;
        }
        if (i == 0 && scenario.equals(HUMID)) {
            hum = 88 + random.nextDouble() * 6;
        }
        double ax = random.nextDouble() * 0.4 - 0.2;
        double ay = random.nextDouble() * 0.4 - 0.2;
        double az = 9.7 + random.nextDouble() * 0.2;

        String env = round(temp) + "," + round(hum);
        String envStatus = "ok";
        if (i == 0 && scenario.equals(FAULT)) {
            env = "NA,NA";
            envStatus = "fault";
        }
        return "T," + deviceIds[i] + "," + bootId + "," + (seq[i]++) + "," + uptime() + ",1,"
                + env + "," + round(ax) + "," + round(ay) + "," + round(az) + "," + envStatus + ",ok";
    }

    private long uptime() {
        return System.currentTimeMillis() - startMs;
    }

    private double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
