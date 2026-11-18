# Desktop build

Install JDK 21 (decision D1). The scripts look for JDK 21 in this order: `SITEGUARD_JAVA_HOME`, `JAVA_HOME`, then `C:\Program Files\Microsoft\jdk-21*`, `C:\Program Files\Eclipse Adoptium\jdk-21*`, and `C:\Program Files\Java\jdk-21*`. They fail if no JDK 21 is found. The prototype used JDK 17 before A01.

This machine uses Microsoft Build of OpenJDK 21.0.13 at `C:\Program Files\Microsoft\jdk-21.0.13.21-hotspot`, installed with `winget install --id Microsoft.OpenJDK.21 -e --source winget`.

From PowerShell in `desktop`, run:

```powershell
.\mvnw.cmd '-Djavax.net.ssl.trustStoreType=Windows-ROOT' clean verify
java -jar .\target\siteguard-0.1.0-SNAPSHOT.jar
```

The first command is needed on this Windows machine: without it, Maven fails with `PKIX path building failed` while downloading plugins. `Windows-ROOT` makes Maven trust the Windows certificate store. TLS certificates are still verified.

The Windows bootstrap downloads Apache Maven 3.9.9 and verifies its published SHA-512 on first use. Maven and its downloaded plugins and dependencies are cached under the ignored `desktop/.maven-cache` directory. The test runner uses headless mode. The executable JAR includes the SQLite driver and FlatLaf.

Dependencies added in A01:

- `com.formdev:flatlaf:3.8`, a Swing look-and-feel library for the dashboard (pinned; latest stable on Maven Central at the time of the change). No application code uses it yet.

For an offline launch without Maven or the SQLite driver, run:

```powershell
.\run-offline.ps1
```

This compiles the main sources with `javac --release 21` and launches the application. Persistent SQLite storage requires the Maven-built application; the offline path uses the application's in-memory fallback and does not run tests.

Run the offline simulation workflow smoke test separately with:

```powershell
.\test-offline.ps1
```

It checks simulated SOS, acknowledgement, resolution with a note, invalid transitions, warning, and sensor faults without a database driver.

The prototype stores SQLite data in the ignored `data` directory under the working directory. Run commands from `desktop` to keep it in `desktop/data`. If the driver or storage is unavailable, the dashboard reports that history is temporary. The three-sample 35 °C warning is a provisional simulation rule, not a validated safety threshold. Hardware, serial/MQTT, protocol receipts, and full course-stage gates are not implemented or verified.
