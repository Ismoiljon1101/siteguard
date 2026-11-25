package siteguard.storage;

import java.util.ArrayList;

import siteguard.model.Incident;
import siteguard.model.Reading;

/**
 * Used only when no file or database can be opened.
 * Nothing is kept after the application closes, and the GUI says so.
 */
public class MemoryIncidentStore implements IncidentStore {

    public String getDescription() {
        return "TEMPORARY memory only (history is lost on exit)";
    }

    public void saveNewIncident(Incident incident) { }

    public void saveAction(int incidentId, String action, long timeMs, String note) { }

    public void saveReading(Reading reading) { }

    public ArrayList<Incident> loadIncidents() {
        return new ArrayList<Incident>();
    }
}
