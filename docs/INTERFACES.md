# Data and service interfaces for the UI

All methods block (SQLite, PBKDF2). Call them from a worker thread, never the Swing event thread. Domain and application code has no Swing, JDBC, MQTT or serial imports. Everything is simulated data.

## Bootstrap

```java
AppContext ctx = AppContext.open(Path.of("data"));   // blocks about 1 s on first run (hashes 3 passwords)
// package com.siteguard.bootstrap
ctx.queries()     // SiteQueryService
ctx.incidents()   // IncidentService
ctx.assignments() // AssignmentService
ctx.health()      // HealthService
ctx.auth()        // AuthService
ctx.ingest()      // IngestService (the simulation uses it; live sources will too)
ctx.simulation()  // SimulationRunner
ctx.demoSiteId()
ctx.firstRunCredentials() // plaintext, only on the first run; never log or show it in the UI
ctx.close()
```

`open` migrates the database (`data/siteguard.db`, schema v4), seeds operators on first run (console plus `data/demo-credentials.txt`), seeds Demo Site A (idempotent), and builds the services. Tests can use `AppContext.open(dir, clock, new PasswordHasher(1000, new SecureRandom()), printStream)`.

The old prototype `SimulationController` and `SqliteIncidentRepository` still exist and still run (the repository migrates the same file). New screens should use `AppContext` only.

## Login (`AuthService`)

- `Operator login(String loginId, char[] password)` returns the operator, or throws `AuthenticationException` (always the same message). The array is cleared.
- Login list: `ctx.queries().operators()` gives `Operator(operatorId, loginId, displayName, role)`. Roles: SAFETY_DIRECTOR, SITE_MANAGER, SUPERVISOR.
- Every write method below takes the logged-in `operatorId`.

## SiteQueryService (reads)

| Method | Returns |
|---|---|
| `sites(operatorId)` | `List<Site>` this operator may see |
| `zones(siteId)` | `List<Zone>` |
| `operators()` | `List<Operator>` |
| `workers()` | `List<Worker>` |
| `helmets(operatorId, siteId)` | `List<HelmetRow(Device device, String workerName, HealthState health, Instant lastSeen)>` |
| `overview(operatorId, siteId)` | `SiteOverview(siteId, siteName, workersOnShift, Map<HealthState,Integer> statusCounts, unassignedHelmets, List<ZoneCount> zones, openSos, openWarnings, Duration oldestUnacknowledged, liveHelmets, simulatedHelmets)` |
| `workers(operatorId, WorkerQuery)` | `WorkerPage(rows, total, page, pageSize)` |
| `workerDetail(operatorId, workerId)` | `Optional<WorkerDetail(worker, current, history, incidents, lastTenMinutes)>` |

- `statusCounts` has ONLINE, STALE, OFFLINE, SENSOR_FAULT, NO_DATA and sums to `workersOnShift`. Zone counts also sum to it (`ZoneCount.zoneId == null` is "No zone"). Unassigned helmets are counted separately.
- `WorkerQuery(siteId, zoneId, noZoneOnly, trade, search, Set<HealthState> health, WorkerSort sort, page /*0-based*/, pageSize)`; `WorkerQuery.firstPage(siteId)` is the default. `WorkerSort.NEEDS_ATTENTION` orders: unresolved SOS, then OFFLINE/STALE/SENSOR_FAULT, then unresolved warnings, then the rest.
- `WorkerRow(assignmentId, workerId, workerName, trade, deviceId, deviceLabel, simulated, zoneId, zoneName, zoneSetAt, zoneSetBy, health, airTempC, humidityPct, updatedAge, incidentIdentity, incidentType, incidentState)`. `airTempC`, `humidityPct` and `updatedAge` are null when unavailable. Show `--`. Values are present only while ONLINE (never old values). Temperature is air near the helmet. `simulated` means show the `SIM` label.
- Health is computed at call time from the clock, so STALE and OFFLINE show up without a sweep.

## IncidentService

| Method | Notes |
|---|---|
| `list(operatorId, IncidentFilter)` | `List<IncidentRow(IncidentView view, Duration unacknowledgedFor)>`, newest first. Site-limited operators must set `siteId`. |
| `detail(operatorId, identity)` | `Optional<IncidentDetail(view, actions, playback, unacknowledgedFor, environmentConditionCleared)>`. `playback` is stored readings 5 min before and after (1 Hz). |
| `acknowledge(identity, operatorId)` | OPEN to ACKNOWLEDGED |
| `addNote(identity, operatorId, note)` | any state, note required |
| `resolve(identity, operatorId, ResolutionCategory, note)` | needs ACKNOWLEDGED, category and note |
| `escalateOverdue()` | call about every second; SOS level 1 at 30 s, level 2 at 120 s (proposal D20) |

Filters: `IncidentFilter.openForSite(siteId)`, `IncidentFilter.all()`, or the full constructor `(siteId, states, type, zoneId, workerId, deviceId, from, to, limit, offset)`. `IncidentView` = `incident` (type, severity, state, occurredAt, escalation, assignmentId, ...) plus `deviceLabel`, `workerName`, `zoneName` as recorded at the time (null if the helmet was unassigned).

Exceptions (all extend `ApplicationException`, unchecked, message safe to show): `ValidationException` (missing category or note, unknown operator), `InvalidTransitionException` (skip, reopen, repeat), `NotFoundException`, `AuthorizationException` (no access to the site). Storage faults throw `StorageException` (show "storage fault"). Acknowledging does not silence a helmet alarm.

## AssignmentService

- `assign(operatorId, deviceId, workerId, Long zoneId /*nullable*/)` returns the new `HelmetAssignment`. Ends the helmet's and worker's earlier assignments in one transaction.
- `endAssignment(operatorId, assignmentId)`
- `setZone(operatorId, assignmentId, zoneId)` appends a zone row; the zone must belong to the helmet's site.
- `history(workerId)`

## HealthService

- `sweep()` records STALE/OFFLINE transitions. Call about every second (the simulation runner already does).
- `current()` gives `Map<deviceId, HealthState>`; `history(deviceId, limit)` gives recorded changes.

## Simulation (`ctx.simulation()`, class `SimulationRunner`)

`start()`, `stop()`, `running()`, `reset()`, `tick()` (one simulated second, synchronous), `helmets()`, `scenarioOf(id)`, `setScenario(id, HelmetScenario)`, `trigger(id, EventType.SOS|MOTION)`, `lastError()`. Scenarios: NORMAL, ENVIRONMENT_WARNING, SOS, MOTION, SENSOR_FAULT, STOPPED. The runner ticks on its own thread at 1 Hz through `IngestService`. Defaults: SIM-007 warning, SIM-008 SOS (after 5 s), SIM-009 motion (after 8 s), SIM-010 sensor fault, SIM-011 stopped (STALE after 5 s, OFFLINE after 30 s, placeholder), the rest normal. Demo data: Demo Site A, 6 zones, SIM-001..SIM-013, Worker A..L, SIM-013 unassigned, Worker L without a zone.

## Ingest (for adapters)

`IngestService.ingest(TelemetryReading)` and `ingest(DeviceEvent)` return `IngestResult(status, incidentIdentity)`; statuses: STORED, STORED_OUT_OF_ORDER, DUPLICATE, RATE_LIMITED, UNKNOWN_DEVICE, SOURCE_MISMATCH. `receiptStored()` is true when the protocol receipt may be sent.

## Refresh pattern for the UI

Run a `javax.swing.Timer` (about 1 s) that starts a `SwingWorker` calling `overview`, `workers` and `list`, then updates the models in `done()`. Do not call these inside the timer callback directly.
