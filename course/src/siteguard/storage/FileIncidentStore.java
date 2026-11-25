package siteguard.storage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import siteguard.exceptions.InvalidActionException;
import siteguard.exceptions.StorageException;
import siteguard.model.Incident;
import siteguard.model.Reading;

/**
 * Saves incidents in plain text files (Chapter 12: File and Scanner).
 *
 * incidents.txt is a log: one line per change, never rewritten.
 *   CREATE|id|deviceId|sourceId|cause|severity|createdAtMs|simulated
 *   ACTION|id|ACKNOWLEDGED|timeMs|note
 * Loading reads the log from the top and repeats every change.
 */
public class FileIncidentStore implements IncidentStore {

    private File incidentFile;
    private File readingFile;

    public FileIncidentStore(File folder) throws StorageException {
        if (!folder.exists() && !folder.mkdirs()) {
            throw new StorageException("Cannot create folder " + folder.getAbsolutePath());
        }
        incidentFile = new File(folder, "incidents.txt");
        readingFile = new File(folder, "readings.csv");
        try {
            incidentFile.createNewFile();
            if (readingFile.createNewFile()) {
                appendLine(readingFile, "received,device,boot,seq,simulated,temperatureC,humidityPct,accel,env,motion");
            }
        } catch (IOException e) {
            throw new StorageException("Cannot create data files: " + e.getMessage());
        }
    }

    public String getDescription() {
        return "Text files in " + incidentFile.getParentFile().getAbsolutePath();
    }

    public void saveNewIncident(Incident i) throws StorageException {
        appendLine(incidentFile, "CREATE|" + i.getId() + "|" + i.getDeviceId() + "|" + i.getSourceId()
                + "|" + clean(i.getCause()) + "|" + i.getSeverity() + "|" + i.getCreatedAtMs()
                + "|" + i.isSimulated());
    }

    public void saveAction(int incidentId, String action, long timeMs, String note) throws StorageException {
        appendLine(incidentFile, "ACTION|" + incidentId + "|" + action + "|" + timeMs + "|" + clean(note));
    }

    public void saveReading(Reading r) throws StorageException {
        appendLine(readingFile, r.getReceivedAtMs() + "," + r.getDeviceId() + "," + r.getBootId() + ","
                + r.getSeq() + "," + r.isSimulated() + ","
                + (r.hasEnvironment() ? r.getTemperatureC() + "," + r.getHumidityPct() : ",")
                + "," + (r.hasMotion() ? String.valueOf(Math.round(r.getAccelMagnitude() * 100) / 100.0) : "")
                + "," + r.getEnvironmentStatus() + "," + r.getMotionStatus());
    }

    public ArrayList<Incident> loadIncidents() throws StorageException {
        ArrayList<Incident> list = new ArrayList<Incident>();
        Scanner in = null;
        int lineNumber = 0;
        try {
            in = new Scanner(incidentFile, "UTF-8");
            while (in.hasNextLine()) {
                String line = in.nextLine();
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] p = line.split("\\|", -1);
                if (p[0].equals("CREATE") && p.length == 8) {
                    list.add(new Incident(Integer.parseInt(p[1]), p[2], p[3], p[4], p[5],
                            Long.parseLong(p[6]), Boolean.parseBoolean(p[7])));
                } else if (p[0].equals("ACTION") && p.length == 5) {
                    Incident incident = find(list, Integer.parseInt(p[1]));
                    if (incident != null) {
                        applyAction(incident, p[2], Long.parseLong(p[3]), p[4]);
                    }
                } else {
                    throw new StorageException("Damaged line " + lineNumber + " in " + incidentFile.getName());
                }
            }
        } catch (IOException e) {
            throw new StorageException("Cannot read " + incidentFile.getName() + ": " + e.getMessage());
        } catch (NumberFormatException e) {
            throw new StorageException("Damaged number on line " + lineNumber + " in " + incidentFile.getName());
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return list;
    }

    /** Repeats a saved action. Shared with JdbcIncidentStore. */
    static void applyAction(Incident incident, String action, long timeMs, String note) {
        try {
            if (action.equals(Incident.ACKNOWLEDGED)) {
                incident.acknowledge(timeMs, note);
            } else if (action.equals(Incident.RESOLVED)) {
                incident.resolve(timeMs, note);
            } else {
                incident.addNote(timeMs, note);
            }
        } catch (InvalidActionException e) {
            // The action was valid when it was saved, so this only happens if a file was edited by hand.
            System.out.println("Skipped saved action: " + e.getMessage());
        }
    }

    static Incident find(ArrayList<Incident> list, int id) {
        for (Incident incident : list) {
            if (incident.getId() == id) {
                return incident;
            }
        }
        return null;
    }

    /** Removes characters that would break the file format. */
    private String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("|", "/").replace("\n", " ").replace("\r", " ");
    }

    private void appendLine(File file, String line) throws StorageException {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(file, true));
            out.println(line);
            if (out.checkError()) {
                throw new StorageException("Writing to " + file.getName() + " failed");
            }
        } catch (IOException e) {
            throw new StorageException("Cannot write " + file.getName() + ": " + e.getMessage());
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
