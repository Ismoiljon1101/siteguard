package siteguard.storage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import siteguard.model.Incident;
import siteguard.model.TimeText;

/**
 * Writes incidents to a CSV file that opens in Excel.
 */
public class CsvExporter {

    public static void export(ArrayList<Incident> incidents, File file) throws IOException {
        PrintWriter out = new PrintWriter(new FileWriter(file));
        try {
            out.println("id,device,cause,severity,state,created,simulated,source_id,history");
            for (Incident i : incidents) {
                String history = "";
                for (String entry : i.getHistory()) {
                    history = history + entry + " ; ";
                }
                out.println(i.getId() + "," + quote(i.getDeviceId()) + "," + quote(i.getCause()) + ","
                        + i.getSeverity() + "," + i.getState() + "," + TimeText.format(i.getCreatedAtMs()) + ","
                        + i.isSimulated() + "," + quote(i.getSourceId()) + "," + quote(history));
            }
        } finally {
            out.close();
        }
    }

    /** Puts text in double quotes so commas inside notes do not break the columns. */
    private static String quote(String text) {
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
