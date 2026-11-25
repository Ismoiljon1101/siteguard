package siteguard.source;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Reads the ESP32 over USB serial without any extra library.
 *
 * The operating system lets a program open a serial port like a file:
 *   Windows: \\.\COM3   (first run: mode COM3 BAUD=115200 PARITY=n DATA=8 STOP=1)
 *   Linux:   /dev/ttyUSB0 (first run: stty -F /dev/ttyUSB0 115200 raw)
 * RandomAccessFile opened with "rw" lets us read lines and also write receipts back.
 *
 * Reading waits for data, so it runs on a background thread. Each line is handed
 * to the Swing event thread with EventQueue.invokeLater (the pattern from Chapter 15).
 */
public class SerialSource implements DataSource {

    private String portPath;
    private RandomAccessFile port;
    private Thread reader;
    private volatile boolean running;
    private LineListener listener;

    public SerialSource(String portName) {
        this.portPath = toPath(portName);
    }

    /** "COM3" becomes "\\.\COM3" on Windows. Other names are used as they are. */
    public static String toPath(String name) {
        String n = name.trim();
        if (n.toUpperCase().startsWith("COM")) {
            return "\\\\.\\" + n.toUpperCase();
        }
        return n;
    }

    public String getName() { return "USB serial " + portPath; }
    public boolean isSimulated() { return false; }
    public boolean isRunning() { return running; }

    public void start(LineListener l) throws IOException {
        this.listener = l;
        if (!portPath.startsWith("\\\\.\\") && !new File(portPath).exists()) {
            throw new IOException("Serial port not found: " + portPath);
        }
        port = new RandomAccessFile(portPath, "rw");
        running = true;
        reader = new Thread(new Runnable() {
            public void run() {
                readLoop();
            }
        });
        reader.setDaemon(true);
        reader.start();
        listener.sourceStatus(this, "connected");
    }

    private void readLoop() {
        try {
            while (running) {
                String line = port.readLine();
                if (line == null) {
                    break;
                }
                deliver(line);
            }
            status("connection closed");
        } catch (IOException e) {
            if (running) {
                status("connection lost: " + e.getMessage());
            }
        }
        running = false;
    }

    private void deliver(final String line) {
        final DataSource self = this;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                listener.lineReceived(line, self);
            }
        });
    }

    private void status(final String text) {
        final DataSource self = this;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                listener.sourceStatus(self, text);
            }
        });
    }

    public boolean send(String line) {
        if (!running) {
            return false;
        }
        try {
            port.write((line + "\n").getBytes("US-ASCII"));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void stop() {
        running = false;
        try {
            if (port != null) {
                port.close();
            }
        } catch (IOException e) {
            System.out.println("Could not close serial port: " + e.getMessage());
        }
        listener.sourceStatus(this, "stopped");
    }
}
