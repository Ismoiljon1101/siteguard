package siteguard.test;

import java.util.ArrayList;

import siteguard.source.DataSource;
import siteguard.source.LineListener;

/**
 * A data source for tests: it does not open any hardware.
 * Every line the monitor sends back to the helmet is kept in sent.
 */
public class FakeSource implements DataSource {

    public ArrayList<String> sent = new ArrayList<String>();
    private boolean simulated;

    public FakeSource(boolean simulated) {
        this.simulated = simulated;
    }

    public String getName() {
        return "Fake";
    }

    public boolean isSimulated() {
        return simulated;
    }

    public void start(LineListener listener) {
    }

    public void stop() {
    }

    public boolean isRunning() {
        return true;
    }

    public boolean send(String line) {
        sent.add(line);
        return true;
    }
}
