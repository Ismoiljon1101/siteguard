# SiteGuard course application

This folder is the version of SiteGuard built for the Java Programming course. It uses only the JDK and the topics from the lecture slides (Chapters 9 to 15), so it compiles with plain `javac` and needs no Maven, no JUnit and no extra libraries. The larger `desktop/` prototype in this repository is separate and unchanged.

It follows the project proposal: helmet readings come in through one common data-source interface, Java checks the rules, creates incidents, saves them so they survive a restart, and shows everything in a Swing dashboard.

## Run it

From this `course` folder, with JDK 17 or newer:

| Windows | Linux / macOS | What it does |
| --- | --- | --- |
| `run.bat` | `./run.sh` | Compile and open the dashboard |
| `test.bat` | `./test.sh` | Compile and run the 15 automatic tests (51 checks) |

Data is saved in `course/data/` (ignored by Git). Without anything extra it uses text files. To use SQLite as written in the proposal, put the SQLite JDBC driver jar in `lib/` (see `lib/README.txt`); the app then picks it up automatically.

## Demo steps

1. Choose **Simulation** and press **Start**. Three helmets labelled `[SIMULATED]` appear.
2. On the **Simulation** tab pick **Heat warning**. After three hot readings in a row, one *High temperature* incident is created (a single hot reading is ignored).
3. Press **SOS on SIM-02**. A HIGH incident appears and the red banner shows it.
4. On **Incidents**, type the number, a note, then **Acknowledge** and **Resolve**. **History** shows every step. **Export CSV** writes `data/incidents-export.csv`.
5. Pick **Connection lost**: SIM-01 turns grey (STALE) after 5 seconds.
6. Close and reopen the app: the incidents and their history are still there.
7. Choose **Replay file** with `samples/demo-replay.txt` to replay recorded lines, including a malformed line and a duplicate that are rejected.

## Connecting the ESP32

The helmet sends one text line per message. The same lines work over USB serial and Wi-Fi.

```
T,deviceId,bootId,seq,uptimeMs,sim,temperatureC,humidityPct,ax,ay,az,envStatus,motionStatus
E,deviceId,bootId,seq,uptimeMs,sim,SOS            (or MOTION)
```

Example: `T,helmet-001,boot7,10,10000,0,27.4,58.2,0.1,0.0,9.8,ok,ok`. A faulty sensor sends `NA` values with status `fault`. `sim` is `1` for simulated data and `0` for hardware. `(deviceId, bootId, seq)` identifies a message, so repeated messages are ignored.

Java answers with `R,deviceId/bootId/seq,stored` after an SOS or MOTION incident is saved, and sends `C,version,maxTemperatureC,maxHumidityPct` when the operator changes settings.

- **USB serial:** choose **USB serial** and type the port, for example `COM3`. Set the speed once first: `mode COM3 BAUD=115200 PARITY=n DATA=8 STOP=1` on Windows, `stty -F /dev/ttyUSB0 115200 raw` on Linux. Java opens the port like a file, so no serial library is needed.
- **Wi-Fi:** choose **Wi-Fi** and port `5050`. The ESP32 connects to the laptop's IP address on that port with a TCP client and sends the same lines.

This line format carries the same fields as the JSON draft in `docs/PROTOCOL.md`, and Wi-Fi uses a plain TCP socket instead of MQTT. Both choices keep the course version free of extra libraries; the team should agree them before the firmware is written.

## Where each lecture topic is used

| Chapter | Topic | Where in the code |
| --- | --- | --- |
| 9 Inheritance | `extends`, `super(...)`, `protected`, multilevel and hierarchical inheritance | `Message` is the parent of `Reading` and `HelmetEvent`; `Rule` → `ThresholdRule` → `TemperatureRule` / `HumidityRule`; `DeviceCard` and `TrendPanel` extend `JPanel`, `MainFrame` extends `JFrame` |
| 10 Polymorphism | overriding, abstract methods, interfaces, a superclass array | `describe()` overridden in each `Message`; `Rule[] rules` checked in one loop in `MonitorService`; `DataSource` has four implementations; `IncidentStore` has three; `paintComponent` overridden in `TrendPanel` |
| 11 Exceptions | `try` / `catch` / `finally`, `throw`, `throws`, multiple catch, your own exception classes | `InvalidMessageException`, `InvalidActionException`, `StorageException`; `MessageParser` turns `NumberFormatException` into `InvalidMessageException`; `Thresholds` throws `IllegalArgumentException`; `finally` closes files |
| 12 Files | `File`, `createNewFile`, `exists`, `Scanner` on a file, `IOException` | `FileIncidentStore` (saves and reloads history), `ReplaySource`, `CsvExporter` |
| 13-14 GUI | `JFrame`, `JPanel`, `JLabel`, `JButton`, `JTextField`, `ActionListener` as an anonymous inner class, `JOptionPane`, `Integer.parseInt` | `MainFrame` |
| 15 Advanced GUI | `JRadioButton`, `ButtonGroup`, `JCheckBox`, `BorderLayout`, `EventQueue.invokeLater(new Runnable() ...)` | source selection, severity filter, scenarios, `Main`, `SerialSource`, `WifiSource` |

A few things go slightly past the slides because the proposal needs them, each kept small: `ArrayList` for lists of helmets and incidents, `javax.swing.Timer` for the once-a-second update (suggested in the course project guidelines), one background `Thread` per hardware connection so the window does not freeze, `JTabbedPane` and `JTextArea` in the window, `FileWriter`/`PrintWriter` to write files, and `java.sql` (part of the JDK) for the optional SQLite storage.

## Files

```
src/siteguard/Main.java              starts the program
src/siteguard/model/                 Message, Reading, HelmetEvent, Incident
src/siteguard/exceptions/            the three custom exceptions
src/siteguard/rules/                 Rule interface and threshold / fault rules, Thresholds
src/siteguard/source/                DataSource: simulation, replay, USB serial, Wi-Fi
src/siteguard/service/               MonitorService (main logic), MessageParser, DeviceStatus
src/siteguard/storage/               text-file, SQLite and memory storage, CSV export
src/siteguard/gui/                   MainFrame, DeviceCard, TrendPanel
src/siteguard/test/                  TestRunner and FakeSource (tests without JUnit)
samples/demo-replay.txt              synthetic recorded data for replay
screenshots/                         two GUI states from simulated data
```

## Limits

This is a classroom prototype, not certified safety equipment. The thresholds are demonstration values. Motion alerts do not diagnose falls. The USB serial and Wi-Fi paths and the SQLite storage have not yet been tested with the real helmet or driver; only the simulation, replay, text-file storage and rules are covered by the automatic tests.
