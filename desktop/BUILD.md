# Desktop build

Install JDK 17 and ensure `java` is on PATH. From PowerShell in `desktop`, run:

```powershell
.\mvnw.cmd clean verify
java -jar .\target\siteguard-0.1.0-SNAPSHOT.jar
```

The Windows bootstrap downloads Apache Maven 3.9.9 and verifies its published SHA-512 on first use. Maven and its downloaded plugins and dependencies are cached under the ignored `desktop/.maven-cache` directory. The test runner uses headless mode; launching the application requires a graphical desktop. The executable JAR includes the SQLite driver.

If this Windows machine reports a certificate trust error while downloading Maven dependencies, use the Windows trusted certificate store for the build: `./mvnw.ps1 '-Djavax.net.ssl.trustStoreType=Windows-ROOT' verify`. This still verifies TLS certificates; it does not disable certificate checking.

For an offline launch without Maven or the SQLite driver, run:

```powershell
.\run-offline.ps1
```

This compiles the main sources with `javac` and launches the application. Persistent SQLite storage requires the Maven-built application; the offline path uses the application's in-memory fallback and does not run tests.

Run the offline simulation workflow smoke test separately with `./test-offline.ps1`. It checks simulated SOS, acknowledgement, resolution with a note, invalid transitions, warning, and sensor faults without a database driver.

The prototype stores SQLite data in the ignored `data` directory under the working directory. Run commands from `desktop` to keep it in `desktop/data`. If the driver or storage is unavailable, the dashboard reports that history is temporary. The three-sample 35 °C warning is a provisional simulation rule, not a validated safety threshold. Hardware, serial/MQTT, protocol receipts, and full course-stage gates are not implemented or verified.
