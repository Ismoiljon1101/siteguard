package siteguard.source;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Receives the same text lines over local Wi-Fi.
 * The laptop listens on a TCP port; the ESP32 connects to it and sends one line per message.
 * Java sends receipts back on the same connection.
 *
 * This replaces the MQTT broker from the proposal with a plain socket from the JDK,
 * so no extra library is needed. The lines are identical to USB serial.
 */
public class WifiSource implements DataSource {

    private int portNumber;
    private ServerSocket server;
    private Socket client;
    private PrintWriter toHelmet;
    private Thread acceptThread;
    private volatile boolean running;
    private LineListener listener;

    public WifiSource(int portNumber) {
        this.portNumber = portNumber;
    }

    public String getName() { return "Wi-Fi port " + portNumber; }
    public boolean isSimulated() { return false; }
    public boolean isRunning() { return running; }

    public void start(LineListener l) throws IOException {
        this.listener = l;
        server = new ServerSocket(portNumber);
        running = true;
        acceptThread = new Thread(new Runnable() {
            public void run() {
                acceptLoop();
            }
        });
        acceptThread.setDaemon(true);
        acceptThread.start();
        listener.sourceStatus(this, "waiting for a helmet on port " + portNumber);
    }

    /** Accepts one helmet at a time; when it disconnects, waits for the next connection. */
    private void acceptLoop() {
        while (running) {
            try {
                client = server.accept();
                toHelmet = new PrintWriter(client.getOutputStream(), true);
                status("helmet connected from " + client.getInetAddress().getHostAddress());
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
                String line = in.readLine();
                while (line != null && running) {
                    deliver(line);
                    line = in.readLine();
                }
                status("helmet disconnected");
            } catch (IOException e) {
                if (running) {
                    status("connection lost: " + e.getMessage());
                }
            } finally {
                closeClient();
            }
        }
    }

    private void closeClient() {
        toHelmet = null;
        try {
            if (client != null) {
                client.close();
            }
        } catch (IOException e) {
            System.out.println("Could not close helmet connection");
        }
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
        PrintWriter out = toHelmet;
        if (out == null) {
            return false;
        }
        out.println(line);
        return !out.checkError();
    }

    public void stop() {
        running = false;
        try {
            if (server != null) {
                server.close();
            }
        } catch (IOException e) {
            System.out.println("Could not close server socket");
        }
        closeClient();
        listener.sourceStatus(this, "stopped");
    }
}
