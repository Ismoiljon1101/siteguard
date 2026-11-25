package siteguard.source;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.Timer;

/**
 * Plays back a recorded file of helmet lines, one line per second.
 * Empty lines and lines starting with # are skipped.
 * Replayed data is labelled as simulated.
 */
public class ReplaySource implements DataSource {

    private File file;
    private ArrayList<String> lines = new ArrayList<String>();
    private int position;
    private Timer timer;
    private LineListener listener;

    public ReplaySource(File file) {
        this.file = file;
    }

    public String getName() { return "Replay " + file.getName(); }
    public boolean isSimulated() { return true; }
    public boolean isRunning() { return timer != null && timer.isRunning(); }
    public boolean send(String line) { return false; }

    public void start(LineListener l) throws IOException {
        this.listener = l;
        if (!file.exists()) {
            throw new IOException("Replay file not found: " + file.getAbsolutePath());
        }
        lines.clear();
        Scanner in = new Scanner(file, "UTF-8");
        try {
            while (in.hasNextLine()) {
                String line = in.nextLine().trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    lines.add(line);
                }
            }
        } finally {
            in.close();
        }
        position = 0;
        timer = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                next();
            }
        });
        timer.start();
        listener.sourceStatus(this, "replaying " + lines.size() + " recorded lines");
    }

    private void next() {
        if (position >= lines.size()) {
            timer.stop();
            listener.sourceStatus(this, "replay finished");
            return;
        }
        listener.lineReceived(lines.get(position), this);
        position++;
    }

    public void stop() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
            listener.sourceStatus(this, "stopped");
        }
    }
}
