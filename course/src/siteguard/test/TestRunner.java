package siteguard.test;

import java.io.File;

import siteguard.exceptions.InvalidActionException;
import siteguard.exceptions.InvalidMessageException;
import siteguard.exceptions.StorageException;
import siteguard.model.Incident;
import siteguard.model.Message;
import siteguard.model.Reading;
import siteguard.rules.Thresholds;
import siteguard.service.MessageParser;
import siteguard.service.MonitorService;
import siteguard.storage.FileIncidentStore;

/**
 * Runs the SiteGuard tests without JUnit.
 * Run: java -cp <classes> siteguard.test.TestRunner
 */
public class TestRunner {

    static int passed = 0;
    static int failed = 0;

    public static void main(String[] args) {
        runTest("parserAcceptsValidTelemetry");
        runTest("parserRejectsMalformed");
        runTest("temperatureAtLimitIsNotWarning");
        runTest("sustainedHeatCreatesOneIncident");
        runTest("shortSpikeIsIgnored");
        runTest("sosCreatesIncidentAndReceipt");
        runTest("duplicateSosIgnored");
        runTest("invalidLineCounted");
        runTest("simulatedSourceLabelsData");
        runTest("staleAfterTimeout");
        runTest("sensorFaultSustained");
        runTest("incidentWorkflow");
        runTest("historySurvivesRestart");
        runTest("thresholdsValidate");
        runTest("filterIncidents");

        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS " + name);
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }

    /** Calls one test by name; an unexpected exception counts as a failure. */
    static void runTest(String name) {
        try {
            if (name.equals("parserAcceptsValidTelemetry")) {
                parserAcceptsValidTelemetry();
            } else if (name.equals("parserRejectsMalformed")) {
                parserRejectsMalformed();
            } else if (name.equals("temperatureAtLimitIsNotWarning")) {
                temperatureAtLimitIsNotWarning();
            } else if (name.equals("sustainedHeatCreatesOneIncident")) {
                sustainedHeatCreatesOneIncident();
            } else if (name.equals("shortSpikeIsIgnored")) {
                shortSpikeIsIgnored();
            } else if (name.equals("sosCreatesIncidentAndReceipt")) {
                sosCreatesIncidentAndReceipt();
            } else if (name.equals("duplicateSosIgnored")) {
                duplicateSosIgnored();
            } else if (name.equals("invalidLineCounted")) {
                invalidLineCounted();
            } else if (name.equals("simulatedSourceLabelsData")) {
                simulatedSourceLabelsData();
            } else if (name.equals("staleAfterTimeout")) {
                staleAfterTimeout();
            } else if (name.equals("sensorFaultSustained")) {
                sensorFaultSustained();
            } else if (name.equals("incidentWorkflow")) {
                incidentWorkflow();
            } else if (name.equals("historySurvivesRestart")) {
                historySurvivesRestart();
            } else if (name.equals("thresholdsValidate")) {
                thresholdsValidate();
            } else if (name.equals("filterIncidents")) {
                filterIncidents();
            }
        } catch (Exception e) {
            failed++;
            System.out.println("FAIL " + name + " (unexpected exception: " + e + ")");
        }
    }

    // ---------- helpers ----------

    static File freshFolder() {
        return new File(System.getProperty("java.io.tmpdir"), "siteguard-test-" + System.nanoTime());
    }

    static MonitorService newService(File folder) throws StorageException {
        return new MonitorService(new FileIncidentStore(folder));
    }

    /** Telemetry line with a working environment sensor and working motion sensor. */
    static String heat(String device, int seq, String temp) {
        return "T," + device + ",b1," + seq + "," + (seq * 1000) + ",0," + temp + ",50.0,0.1,0.0,9.8,ok,ok";
    }

    static String sos(String device, int seq) {
        return "E," + device + ",b1," + seq + "," + (seq * 1000) + ",0,SOS";
    }

    static String faultLine(int seq) {
        return "T,helmet-001,b1," + seq + "," + (seq * 1000) + ",0,NA,NA,0.1,0.0,9.8,fault,ok";
    }

    static boolean parseFails(String line) {
        try {
            new MessageParser().parse(line);
            return false;
        } catch (InvalidMessageException e) {
            return true;
        }
    }

    // ---------- tests ----------

    static void parserAcceptsValidTelemetry() throws Exception {
        Message m = new MessageParser().parse("T,helmet-001,b1,1,1000,0,27.0,50.0,0.1,0.0,9.8,ok,ok");
        check("parser returns a Reading", m instanceof Reading);
        if (m instanceof Reading) {
            Reading r = (Reading) m;
            check("parser reads temperature 27.0", r.getTemperatureC() == 27.0);
        }
        check("parser sourceId is helmet-001/b1/1", m.getSourceId().equals("helmet-001/b1/1"));
    }

    static void parserRejectsMalformed() throws Exception {
        StringBuilder longLine = new StringBuilder("E,helmet-001,b1,1,1000,0,SOS");
        while (longLine.length() <= 300) {
            longLine.append("x");
        }
        check("parser rejects empty line", parseFails(""));
        check("parser rejects unknown kind X,1,2", parseFails("X,1,2"));
        check("parser rejects bad seq abc",
                parseFails("T,helmet-001,b1,abc,1000,0,27,50,0,0,9.8,ok,ok"));
        check("parser rejects humidity 120",
                parseFails("T,helmet-001,b1,1,1000,0,27.0,120,0.1,0.0,9.8,ok,ok"));
        check("parser rejects 12 fields",
                parseFails("T,helmet-001,b1,1,1000,0,27,50,0,0,9.8,ok"));
        check("parser rejects unknown event PANIC",
                parseFails("E,helmet-001,b1,1,1000,0,PANIC"));
        check("parser rejects fault status with numbers instead of NA",
                parseFails("T,helmet-001,b1,5,5000,0,27,50,0.1,0.0,9.8,fault,ok"));
        check("parser rejects sim flag 2",
                parseFails("T,helmet-001,b1,1,1000,2,27,50,0,0,9.8,ok,ok"));
        check("parser rejects line longer than 256 chars", parseFails(longLine.toString()));
    }

    static void temperatureAtLimitIsNotWarning() throws Exception {
        File folder = freshFolder();
        MonitorService service = newService(folder);
        FakeSource fake = new FakeSource(false);
        long t = 1000000L;
        for (int seq = 1; seq <= 5; seq++) {
            service.handleLine(heat("helmet-001", seq, "35.0"), fake, t + seq * 1000);
        }
        check("temperature exactly at limit creates no incident", service.getIncidents().size() == 0);
    }

    static void sustainedHeatCreatesOneIncident() throws Exception {
        File folder = freshFolder();
        MonitorService service = newService(folder);
        FakeSource fake = new FakeSource(false);
        long t = 1000000L;
        service.handleLine(heat("helmet-001", 1, "36.0"), fake, t);
        service.handleLine(heat("helmet-001", 2, "36.0"), fake, t + 1000);
        check("two hot readings create no incident yet", service.getIncidents().size() == 0);
        service.handleLine(heat("helmet-001", 3, "36.0"), fake, t + 2000);
        check("third hot reading creates one incident", service.getIncidents().size() == 1);
        if (service.getIncidents().size() == 1) {
            Incident i = service.getIncidents().get(0);
            check("heat incident cause is High temperature", i.getCause().equals("High temperature"));
            check("heat incident severity is MEDIUM", i.getSeverity().equals(Incident.MEDIUM));
        }
        service.handleLine(heat("helmet-001", 4, "36.0"), fake, t + 3000);
        service.handleLine(heat("helmet-001", 5, "36.0"), fake, t + 4000);
        service.handleLine(heat("helmet-001", 6, "36.0"), fake, t + 5000);
        check("more hot readings do not add a second incident", service.getIncidents().size() == 1);
    }

    static void shortSpikeIsIgnored() throws Exception {
        File folder = freshFolder();
        MonitorService service = newService(folder);
        FakeSource fake = new FakeSource(false);
        long t = 1000000L;
        String[] temps = {"36.0", "36.0", "27.0", "36.0", "36.0"};
        for (int k = 0; k < temps.length; k++) {
            service.handleLine(heat("helmet-001", k + 1, temps[k]), fake, t + k * 1000);
        }
        check("short spikes (2 hot, cool, 2 hot) create no incident", service.getIncidents().size() == 0);
    }

    static void sosCreatesIncidentAndReceipt() throws Exception {
        File folder = freshFolder();
        MonitorService service = newService(folder);
        FakeSource fake = new FakeSource(false);
        long t = 1000000L;
        service.handleLine(sos("helmet-001", 7), fake, t);
        check("SOS creates one incident", service.getIncidents().size() == 1);
        if (service.getIncidents().size() == 1) {
            Incident i = service.getIncidents().get(0);
            check("SOS incident severity is HIGH", i.getSeverity().equals(Incident.HIGH));
            check("SOS incident state is NEW", i.getState().equals(Incident.NEW));
        }
        check("SOS sends receipt R,helmet-001/b1/7,stored",
                fake.sent.contains("R,helmet-001/b1/7,stored"));
    }

    static void duplicateSosIgnored() throws Exception {
        File folder = freshFolder();
        MonitorService service = newService(folder);
        FakeSource fake = new FakeSource(false);
        long t = 1000000L;
        service.handleLine(sos("helmet-001", 7), fake, t);
        service.handleLine(sos("helmet-001", 7), fake, t + 1000);
        check("duplicate SOS creates no second incident", service.getIncidents().size() == 1);
        check("duplicate SOS is counted", service.getDuplicateCount() == 1);
        check("receipt is sent again for the duplicate", fake.sent.size() == 2);
    }

    static void invalidLineCounted() throws Exception {
        File folder = freshFolder();
        MonitorService service = newService(folder);
        FakeSource fake = new FakeSource(false);
        service.handleLine("garbage", fake, 1000000L);
        check("invalid line is counted", service.getInvalidCount() == 1);
        check("invalid line creates no incident", service.getIncidents().size() == 0);
    }

    static void simulatedSourceLabelsData() throws Exception {
        File folder = freshFolder();
        MonitorService service = newService(folder);
        FakeSource fake = new FakeSource(true);
        service.handleLine(sos("helmet-001", 7), fake, 1000000L);
        check("simulated source makes incident simulated", service.getIncidents().size() == 1
                && service.getIncidents().get(0).isSimulated());
    }

    static void staleAfterTimeout() throws Exception {
        File folder = freshFolder();
        MonitorService service = newService(folder);
        FakeSource fake = new FakeSource(false);
        long t = 1000000L;
        service.handleLine(heat("helmet-001", 1, "27.0"), fake, t);
        check("not stale 3 s after the last reading", service.checkStale(t + 3000) == 0);
        check("stale 6 s after the last reading", service.checkStale(t + 6000) == 1);
    }

    static void sensorFaultSustained() throws Exception {
        File folder = freshFolder();
        MonitorService service = newService(folder);
        FakeSource fake = new FakeSource(false);
        long t = 1000000L;
        service.handleLine(faultLine(1), fake, t);
        service.handleLine(faultLine(2), fake, t + 1000);
        service.handleLine(faultLine(3), fake, t + 2000);
        check("three fault readings create one incident", service.getIncidents().size() == 1);
        if (service.getIncidents().size() == 1) {
            check("fault incident cause is Sensor fault",
                    service.getIncidents().get(0).getCause().equals("Sensor fault"));
        }
    }

    static void incidentWorkflow() throws Exception {
        File folder = freshFolder();
        MonitorService service = newService(folder);
        FakeSource fake = new FakeSource(false);
        long t = 1000000L;
        service.handleLine(sos("helmet-001", 7), fake, t);
        int id = service.getIncidents().get(0).getId();

        service.acknowledge(id, "on my way", t);
        check("acknowledge sets ACKNOWLEDGED",
                service.getIncident(id).getState().equals(Incident.ACKNOWLEDGED));

        boolean threw = false;
        try {
            service.acknowledge(id, "again", t);
        } catch (InvalidActionException e) {
            threw = true;
        }
        check("acknowledging twice throws InvalidActionException", threw);

        threw = false;
        try {
            service.resolve(id, "", t);
        } catch (InvalidActionException e) {
            threw = true;
        }
        check("resolve with empty note throws InvalidActionException", threw);

        service.resolve(id, "worker ok", t);
        check("resolve sets RESOLVED", service.getIncident(id).getState().equals(Incident.RESOLVED));

        threw = false;
        try {
            service.resolve(id, "again", t);
        } catch (InvalidActionException e) {
            threw = true;
        }
        check("resolving twice throws InvalidActionException", threw);

        check("history has CREATED, ACKNOWLEDGED, RESOLVED",
                service.getIncident(id).getHistory().size() == 3);
    }

    static void historySurvivesRestart() throws Exception {
        File folder = freshFolder();
        FakeSource fake = new FakeSource(false);
        long t = 1000000L;

        MonitorService first = newService(folder);
        first.handleLine(sos("helmet-001", 7), fake, t);
        first.acknowledge(1, "on my way", t + 1000);
        first.resolve(1, "checked", t + 2000);

        MonitorService second = newService(folder);
        check("after restart there is 1 incident", second.getIncidents().size() == 1);
        if (second.getIncidents().size() == 1) {
            Incident i = second.getIncidents().get(0);
            check("after restart incident is RESOLVED", i.getState().equals(Incident.RESOLVED));
            check("after restart history has 3 entries", i.getHistory().size() == 3);
        }

        second.handleLine(sos("helmet-001", 7), fake, t + 3000);
        check("replayed SOS after restart creates no new incident", second.getIncidents().size() == 1);

        second.handleLine(sos("helmet-001", 8), fake, t + 4000);
        check("new SOS after restart has 2 incidents", second.getIncidents().size() == 2);
        if (second.getIncidents().size() == 2) {
            check("new SOS after restart gets id 2", second.getIncidents().get(1).getId() == 2);
        }
    }

    static void thresholdsValidate() throws Exception {
        Thresholds thresholds = new Thresholds();
        boolean threw = false;
        try {
            thresholds.setMaxTemperatureC(100);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        check("temperature limit 100 is rejected", threw);

        threw = false;
        try {
            thresholds.setSustainedSamples(0);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        check("sustained samples 0 is rejected", threw);
    }

    static void filterIncidents() throws Exception {
        File folder = freshFolder();
        MonitorService service = newService(folder);
        FakeSource fake = new FakeSource(false);
        long t = 1000000L;
        // SOS on helmet-001 and heat on helmet-002, so the device filter "002" matches only one.
        service.handleLine(sos("helmet-001", 7), fake, t);
        service.handleLine(heat("helmet-002", 1, "36.0"), fake, t + 1000);
        service.handleLine(heat("helmet-002", 2, "36.0"), fake, t + 2000);
        service.handleLine(heat("helmet-002", 3, "36.0"), fake, t + 3000);
        check("two incidents exist before filtering", service.getIncidents().size() == 2);

        check("HIGH filter gives 1", service.filterIncidents(false, "HIGH", "").size() == 1);
        check("device filter 002 gives 1", service.filterIncidents(false, "ALL", "002").size() == 1);

        service.resolve(1, "done", t + 4000);
        check("open-only filter after resolving SOS gives 1",
                service.filterIncidents(true, "ALL", "").size() == 1);
    }
}
