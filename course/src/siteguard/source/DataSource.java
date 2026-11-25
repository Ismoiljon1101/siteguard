package siteguard.source;

import java.io.IOException;

/**
 * The common data-source interface from the proposal.
 * Simulation, recorded replay, USB serial and Wi-Fi all implement it,
 * so their lines go through exactly the same rules and storage.
 */
public interface DataSource {

    String getName();

    /** Simulated and replayed data is always labelled as simulated. */
    boolean isSimulated();

    void start(LineListener listener) throws IOException;

    void stop();

    boolean isRunning();

    /** Sends a line back to the helmet (receipts, settings). Returns false if this source cannot send. */
    boolean send(String line);
}
