package siteguard.source;

/**
 * Receives what a DataSource produces. Both methods are always called on the
 * Swing event thread, so the listener may update the GUI directly.
 */
public interface LineListener {

    void lineReceived(String line, DataSource source);

    void sourceStatus(DataSource source, String status);
}
