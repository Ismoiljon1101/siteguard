# Proposals for the TL (DATA-ENG)

Implemented as placeholders so the build works. Each needs a TL decision before it counts as accepted.

## 1. OFFLINE definition (open item 2)

Implemented: OFFLINE = no valid data for 30 s (`HealthPolicy.OFFLINE_AFTER`). STALE = 5 s to under 30 s (D8). The simulator and tests exercise this. "Transport lost" is not modelled because no transport adapter exists yet. Alternative: OFFLINE when the serial or MQTT link reports loss (needs A09/A11). The 30 s value is not measured.

## 2. ENVIRONMENT recovery rule (open item 3)

Implemented: the condition is cleared when a sample below the temperature limit (35 C) arrives. This only sets a read-only flag, `environmentConditionCleared`, on the incident detail for an unresolved ENVIRONMENT incident. It never resolves or changes the incident. No new incident while one is unresolved for that device. After an operator resolves it, the next hot sample can open a new one (tested). Not implemented: hysteresis (for example, below 33 C for N samples) or auto-resolve.

## 3. Other choices made in code (please confirm)

- Storage rate limit: at most one stored reading per device per wall-clock second (server clock). Extra readings in the same second still refresh "last seen" but are not stored and run no rules. A live 1 Hz source with jitter may lose an occasional sample.
- "3 consecutive samples" means three consecutive stored readings in the same boot. A sensor fault sample or a cooler sample breaks the run. A reboot resets it.
- Out-of-order readings (lower seq in the current boot, or an older boot) are stored for history but never become the current reading and run no rules.
- Simulated message for a live device (or the reverse) is rejected as `SOURCE_MISMATCH`, so a simulation cannot mark a physical helmet live.
- MOTION events always open a WARNING incident. The config threshold stays NULL (D10); the Java side has no magnitude to compare.
- Escalation (D20, still PROPOSED): `IncidentService.escalateOverdue()` raises `escalation_level` on OPEN SOS at 30 s and 120 s. Acknowledged SOS stops escalating. No notifications.
- Sites: SUPERVISOR and SITE_MANAGER are scoped by `operator_sites`; SAFETY_DIRECTOR sees all sites. Role-specific permissions beyond site scope (for example, only SITE_MANAGER registers helmets) are not enforced yet.
- A stored resolving row from the old prototype repository may lack a category. The database trigger requires the category only on rows written with an `operator_id`, so the prototype code keeps working until the UI moves to `IncidentService`.
- Unregistered prototype helmets in an old database (for example `sim-helmet-001`) are adopted under a site named "Prototype (legacy)" during migration 3.
- Credentials: see `docs/CHANGE_REQUEST_CREDENTIALS.md`.
