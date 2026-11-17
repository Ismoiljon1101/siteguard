package com.siteguard.ui;

import java.util.function.Consumer;
import com.siteguard.adapter.simulation.Scenario;

/** Application-facing commands. Implementations must return promptly and run I/O off the EDT. */
public interface DashboardController extends AutoCloseable {
    void start(Consumer<DashboardSnapshot> updates);
    void acknowledge(String id, String operator);
    void addNote(String id, String operator, String note);
    void resolve(String id, String operator, String note);
    void triggerSos();
    void triggerMotion();
    void setScenario(Scenario scenario);
    @Override void close();
}
