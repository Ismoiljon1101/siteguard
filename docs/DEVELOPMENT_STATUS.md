# Development status and parallel work

Updated November 14, 2026. Ismoiljon is team lead. The first Java prototype is available for review; it does not complete or accept the A00-A16 stage gates.

## Current runnable slice

The Swing dashboard shows synthetic temperature, humidity, acceleration, device status, and an incident table. Controls select normal, environmental-warning, and sensor-fault scenarios and trigger synthetic SOS or motion events. Incidents can be acknowledged and resolved with notes. The provisional environmental demonstration creates one warning after three consecutive samples at or above 35 °C; A00/A05 must agree and validate real thresholds and recovery rules.

The TL clarified that the target is a multi-worker system with a central server. The current screen still models one synthetic helmet, has no worker assignments or central server, and has no location/GPS support. The target and phased architecture are described in [SYSTEM_DESIGN.md](SYSTEM_DESIGN.md); these capabilities are planned, not implemented.

The offline launcher compiles with JDK 17 and runs without external libraries. It reports **memory-only** storage, so data is lost on exit. The Maven-built executable includes the SQLite driver; its dashboard displayed **SQLite connected**, and a synthetic SOS was verified in the local database. Database files live in the ignored working-directory data folder.

Actual checks for this slice: Java 17 offline compilation; a headless workflow smoke test covering synthetic SOS, acknowledgement, resolution with note, rejected transition, environmental warning, and sensor fault; Maven verify with three SQLite tests passing; and a manual dashboard check of SOS and acknowledgement. The SQLite tests cover migration/restart, duplicate identity, action history/transition validation, and rollback. No real sensor, physical SOS, device network, receipt, latency, or safety result has been measured.

## Parallel work streams

| Stream | Initial work | Next review or gate |
| --- | --- | --- |
| Build and tooling | Java 17 Maven configuration, offline launcher, and packaged JAR | Verify wrapper and CI on a clean machine; this Windows machine needed its trusted certificate store for Maven downloads |
| Domain and simulation | Synthetic readings, scenarios, and event identities | Review the draft A00 contract and add missing validation/fault semantics |
| Swing interface | Readings, scenario controls, incidents, and operator actions | Review usability, event-thread behavior, and visible error feedback |
| Persistence | SQLite incident and action adapter; three database tests passed | Implement the full event/incident transaction and required history; expand failure and migration coverage |
| Review and integration | Prototype coordination and offline smoke test | Reconcile interfaces and verify end-to-end failure paths before accepting a stage |

AI agents assisted these streams; human team ownership and acceptance remain as specified in [TEAM_PLAN.md](TEAM_PLAN.md). Parallel implementation does not override stage prerequisites or establish A00 acceptance. The TL and firmware owner must review shared contracts before hardware integration.

## Next steps

1. Agree A00 protocol, thresholds, Java/build decisions, and local alarm/reset behavior. Record decisions and review affected code and fixtures.
2. Expand database and integration coverage beyond the current three tests; verify full event/incident transactions and end-to-end storage-failure behavior.
3. Complete the domain/application boundaries and Java rules, then build independent simulated helmets, worker assignments, and a multi-worker dashboard on those interfaces. Test at least ten labeled simulated devices without claiming physical scale.
4. Once parts arrive, run A08 bench checks with the actual ESP32, sensors, external SOS button, buzzer, and borrowed meter. Record observed results.
5. Integrate USB serial first, followed by Wi-Fi/MQTT and a central Java service with a dashboard API. Validate and document the end-to-end path before making reliability or safety claims. Decide location requirements separately; GPS hardware was not ordered.

See [desktop build instructions](../desktop/BUILD.md), [stage cards](BUILD_STAGES.md), and [hardware plan](HARDWARE.md). Keep local databases and personal data outside version control.
