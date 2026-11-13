# Build stages for team and AI-agent work

This is the execution breakdown of [ROADMAP.md](ROADMAP.md), not a new scope. Every stage below is **planned**. No implementation, build, or hardware result is implied. Ismoiljon accepts stage gates as TL; Jakhongir owns firmware and hardware decisions. The third member remains unconfirmed.

The November 13 budget redesign supersedes earlier part quantities/models. Read [HARDWARE.md](HARDWARE.md) and the exact [cart list](SHOPPING_LIST.md): S3 N16R8, onboard RGB/BOOT, AHT20/BMP280, LIS3DH, and active-low buzzer. Preserve Java scope and the draft wire schema. S3 GPIO/USB behavior and control access require A00/A08 verification; shopping does not accept a build gate.

Give an agent one stage card using [AGENT_TASK_TEMPLATE.md](AGENT_TASK_TEMPLATE.md). Read [AGENTS.md](../AGENTS.md) first. Agents assist the accountable team member; using an agent does not change human ownership or the requirement to explain the code.

## Build order

| Wave | Stages | Result before moving on |
| --- | --- | --- |
| 0 | A00, then A01, then A02 | Decisions, reproducible Java build, and shared Java contracts |
| 1 | A03, A04, A05, A06 | Simulator, SQLite, rules, and UI built against the same contracts |
| Hardware lane | A08 after A00 and parts arrival | ESP32 reads sensors and runs local alarms without Java |
| 2 | A07; A09 can follow A02 independently | Complete simulated Java app and tested serial adapter |
| 3 | A10 | One physical SOS travels through USB to a saved, manageable Java incident |
| 4 | A11 and A12, then A13 | Same system works over Wi-Fi/MQTT and handles disconnects |
| 5 | A14, then A15 | Remaining baseline features, measured evidence, and presentation release |
| Optional | A16 after A15 | One separately chosen extension at a time |

A08 can run alongside Java work; hardware delivery must not block simulation. Stages A03-A06 can run independently after A02 **only when different contributors have isolated branches/worktrees and disjoint files**. Otherwise do them sequentially. A11 and A12 can run separately because Java and firmware use separate paths. Integration is always a separate gate.

## Contracts and file ownership

Use a single Maven desktop application initially. Proposed root package: `com.siteguard`. A00/A01 confirm the exact package and Java release before code is split among contributors.

| Path | Owner of changes | Dependency rule |
| --- | --- | --- |
| `desktop/pom.xml`, Maven wrapper, CI | A01; later TL-coordinated changes | Pin build and dependency versions; no parallel build-file edits |
| `desktop/src/main/java/com/siteguard/domain/` | A02; later reviewed changes | Java types only; no UI, transport, or persistence libraries |
| `desktop/src/main/java/com/siteguard/application/port/` | A02; later reviewed changes | Contracts use domain types; no concrete adapters |
| `desktop/src/main/java/com/siteguard/application/service/` | A05, A14 | Depend on domain and ports |
| `desktop/src/main/java/com/siteguard/adapter/simulation/` | A03, A14 | Feed the same application path as live sources |
| `desktop/src/main/java/com/siteguard/adapter/sqlite/`, migration resources | A04 | Implement persistence ports; JDBC and transactions stay here |
| `desktop/src/main/java/com/siteguard/ui/` | A06, A14 | Consume view state and application commands; no SQL or wire parsing |
| `desktop/src/main/java/com/siteguard/adapter/serial/` | A09 | Framing and transport only |
| `desktop/src/main/java/com/siteguard/adapter/mqtt/` | A11 | Broker lifecycle and transport only |
| `desktop/src/main/java/com/siteguard/adapter/protocol/` | A02 | Shared bounded JSON codec used by serial and MQTT |
| `desktop/src/main/java/com/siteguard/bootstrap/` | A01, A07, A10, A13 | Construct and connect services; no business rules |
| `firmware/`, `config/firmware/` | A08, A12 | Jakhongir owns hardware pin map and firmware |
| `fixtures/protocol/` | A00, A02 | Shared contract fixtures; changes require review |
| `fixtures/scenarios/` | A03, A14 | Deterministic simulation/replay fixtures |
| `config/broker/`, `config/desktop/` | A11, A13 | Sanitized examples only |
| `evidence/` | A10, A13-A16 | Actual sanitized results with date and revision |

Tests mirror the relevant packages under `desktop/src/test/java/`. An agent changes its own tests, not another stage's implementation. When a frozen interface must change, the TL records the decision, updates the affected fixtures, and identifies downstream work before agents continue.

## Stage cards

### A00 — Resolve and freeze the first contract

- **Roadmap:** S00. **Owner:** Ismoiljon with Jakhongir. **Depends on:** none.
- **Scope:** protocol/design documents and `fixtures/protocol/`; no application implementation.
- **Deliver:** agree message validation, null/fault combinations, supported event types, units, ordering, duplicate identity, receipt semantics, and serial reconnect behavior. Specify local alarm/reset behavior, environment thresholds/durations, motion demonstration rule, and configuration-command/result examples. Distinguish UI acknowledgement from local alarm reset. Choose the Java release and package root, build commands to establish in A01, and sensor/library candidates to verify in A08. Preserve unresolved purchasing checks honestly.
- **Gate:** TL and firmware owner record accepted decisions; valid telemetry, missing-sensor telemetry, SOS, motion, receipt, configuration request/result, duplicate, and invalid examples have unambiguous expected outcomes. Roster approval is tracked separately and cannot be silently marked complete by an agent.
- **Read:** [PROTOCOL.md](PROTOCOL.md), [SYSTEM_DESIGN.md](SYSTEM_DESIGN.md), [HARDWARE.md](HARDWARE.md), [TEAM_PLAN.md](TEAM_PLAN.md).

### A01 — Create the Java build and empty application

- **Roadmap:** S01. **Owner:** Ismoiljon. **Depends on:** A00's Java/build decisions.
- **Scope:** Maven wrapper, `desktop/pom.xml`, bootstrap, minimal window, smoke tests, build instructions, CI.
- **Deliver:** reproducible Java desktop build, test runner, a minimal Swing window, and explicit application shutdown. Select and pin dependencies needed by the approved stages; no unused frameworks. Document Windows PowerShell commands and a headless test path.
- **Gate:** clean checkout passes the documented build/test command; the packaged application opens and closes locally. CI builds/tests on the chosen JDK. Commit actual commands so later cards can refer to them without inventing flags.
- **Do not add:** sensors, MQTT, database schema, or finished dashboard.

### A02 — Implement domain types, ports, and the wire codec

- **Roadmap:** S03/S04 contract foundation. **Owner:** Ismoiljon. **Depends on:** A01.
- **Scope:** `domain/`, `application/port/`, `adapter/protocol/`, matching tests, shared protocol fixtures.
- **Deliver:** typed readings/events/source identities, source mode and health, incident states/actions, validated configuration, application input/output contracts, persistence/transaction contracts, view-state/command contracts, and an injectable clock. Implement the shared JSON encoder/decoder with bounded input. Define how the application receives events and returns an event receipt after persistence confirmation.
- **Gate:** fixtures parse or reject as specified; invalid units/ranges/null combinations and unsupported versions fail clearly. State transitions reject invalid actions. Source identity includes device, boot, and sequence. Ports can be implemented by later stages without importing a concrete adapter. Store a short interface reference for downstream agents.
- **Do not add:** working database, transport connections, UI behavior, or rule service implementations.

### A03 — Build deterministic simulation

- **Roadmap:** S03. **Owner:** Ismoiljon; handoff possible to confirmed third member. **Depends on:** A02.
- **Scope:** `adapter/simulation/`, `fixtures/scenarios/`, matching tests.
- **Deliver:** start/stop/reset and deterministic normal, environmental-warning, SOS, motion, missing-sensor, and stopped-telemetry scenarios. Inject time/scheduling where tests need control. Emit domain inputs through the agreed port with explicit simulated provenance.
- **Gate:** tests produce repeatable sequences; stop/reset do not leak background tasks. Simulation does not create a separate rule engine or mark a physical helmet live. Expose controls for A06/A07 without editing UI code.

### A04 — Implement SQLite persistence and transactions

- **Roadmap:** S05. **Owner:** Ismoiljon until third-member handoff. **Depends on:** A02.
- **Scope:** `adapter/sqlite/`, numbered migrations, database tests.
- **Deliver:** devices, sessions, sampled readings, source events, incidents, and append-only actions according to the storage plan. Implement the agreed transaction boundary. Enable foreign keys for every connection and enforce source uniqueness durably.
- **Gate:** temporary-database tests prove migration, restart recovery, rollback on failed writes, duplicate protection across restart, and valid incident/action persistence. The event and resulting incident commit atomically. No transport receipt originates inside the database adapter.
- **Do not add:** handwritten SQL in UI/application services or local database files to Git.

### A05 — Implement application rules and incident workflow

- **Roadmap:** S03/S05/S08 logic. **Owner:** Ismoiljon. **Depends on:** A02; use test doubles until A04 integration.
- **Scope:** `application/service/` and matching tests; avoid export/replay extension work reserved for A14.
- **Deliver:** validate ingestion context, environmental warning duration/recovery rules, SOS/motion incident creation, deduplication coordination, acknowledge/note/resolve commands, and device health. Publish immutable view updates and receipt decisions through ports. Handle out-of-order readings without moving the current display backward.
- **Gate:** deterministic tests cover threshold boundaries, sustained violations without incident spam, allowed/rejected transitions, 5-second stale threshold, offline state distinct from incidents, transaction failure with no `stored` receipt, and replay/simulation provenance. Duplicate committed events can receive another receipt without another incident.
- **Do not add:** Swing, broker clients, JDBC implementation, or firmware logic.

### A06 — Build the Swing operator interface

- **Roadmap:** S06 UI. **Owner:** Ismoiljon. **Depends on:** A02; develop against view/command fakes.
- **Scope:** `ui/`, UI resources, focused presenter/view-model tests.
- **Deliver:** readings, source label, last-update age, device/connection/storage health, incident table, filters, acknowledge/note/resolve controls, and simulator controls. Include clear empty, disconnected, fault, and invalid-action states.
- **Gate:** run a manual demo with fake view data; capture normal and warning/fault states. Controls invoke application commands; UI contains no SQL or JSON parsing. Swing changes run on the event thread and slow fake work does not freeze the window. Handle close without leaving workers running.
- **Do not add:** polished charts, CSV, or replay controls until A14.

### A07 — Integrate the simulated Java application

- **Roadmap:** S01/S03/S05/S06 integration. **Owner:** Ismoiljon. **Depends on:** A03-A06.
- **Scope:** bootstrap, integration tests, setup instructions; focused fixes in another package require its owner/TL coordination.
- **Deliver:** wire simulator, real rules, SQLite, and Swing into one runnable application. Provide sanitized local configuration and reproducible startup. Persist application history while retaining source labels.
- **Gate:** without hardware, start normal simulation, trigger warning/SOS, acknowledge and resolve with a note, restart Java, and recover history. Run baseline checks plus T01, T04, T05, T11, and the applicable simulated portion of T12/T13. Record what remains untested physically.
- **Milestone:** Java can be demonstrated while hardware is in transit.

### A08 — Bring up firmware and bench hardware

- **Roadmap:** S02. **Owner:** Jakhongir. **Depends on:** A00, actual boards, and lab tools; independent of Java stages.
- **Scope:** `firmware/`, `config/firmware/`, wiring/pin-map documentation, sanitized bench evidence.
- **Deliver:** record the actual S3 N16R8 target, flash/PSRAM settings, USB interface, toolchain, libraries, GPIOs, power, I2C addresses, motion sampling, and ranges. Read both sensors; debounce runtime BOOT SOS; drive onboard RGB and active-low buzzer without Java. Verify bootloader behavior and SOS access; add an external button if access fails. Emit protocol-valid JSON lines with unique boot/sequence identity; parse Java receipts without blocking sampling. Include sensor faults and a safe buzzer startup state.
- **Gate:** compile for the selected board; human-supervised bench checks read each sensor, show disconnected-sensor behavior, and trigger local SOS without laptop connectivity. Confirm serial messages match A00 fixtures. Record real results and local reset behavior. Agents must not claim wiring/measurements based on compilation alone.
- **Read:** [SHOPPING_LIST.md](SHOPPING_LIST.md), hardware plan, protocol, test plan. Solder loose sensor headers. Spare buzzers stay disconnected; no duplicate controller/sensor set is in the cart.

### A09 — Implement and test the Java serial adapter

- **Roadmap:** S04. **Owner:** Ismoiljon with Jakhongir review. **Depends on:** A02; A08 is needed only for the final physical check.
- **Scope:** `adapter/serial/`, matching tests, serial setup notes.
- **Deliver:** configurable port/baud, bounded newline framing, shared codec, connect/disconnect/reconnect, receipt output, and clean shutdown. Do not hard-code one developer's COM port. Route accepted messages into the application port.
- **Gate:** stream/transport-fake tests cover partial lines, multiple lines, oversized lines, malformed data, disconnect mid-frame, and reconnect. No unbounded buffer, duplicate parser, or blocking Swing call. Clearly mark real-COM-port verification pending until A10.

### A10 — Demonstrate the full wired system

- **Roadmap:** S04-S06; milestone M2. **Owner:** Ismoiljon + Jakhongir. **Depends on:** A07-A09.
- **Scope:** bootstrap, integration tests, configuration examples, sanitized evidence; coordinated integration fixes.
- **Deliver:** choose serial or simulation explicitly; connect the physical helmet to the existing application path. Return matching `stored` receipts only after SQLite commit. Use serial as a documented demo fallback.
- **Gate:** physical SOS appears once in Java, is saved, acknowledged, annotated, resolved, and recovered after Java restart. Repeat the event identity to prove deduplication. Remove a sensor, stop telemetry, and simulate a storage failure. Record results against T02-T06 and T10-T12 where applicable. The Java UI remains responsive.
- **Milestone:** this is the first complete physical project. Protect this working revision before adding wireless behavior.

### A11 — Implement the Java MQTT adapter and broker setup

- **Roadmap:** Java side of S07. **Owner:** Ismoiljon. **Depends on:** A10.
- **Scope:** `adapter/mqtt/`, `config/broker/`, `config/desktop/`, adapter tests and broker instructions.
- **Deliver:** authenticated local broker setup, explicit allowed device IDs, topic/payload identity checks, shared codec, receipt publication, command/result transport, reconnect, and clean shutdown. Implement the protocol's QoS/retention decisions. Keep passwords outside source control.
- **Gate:** adapter tests and a local-broker integration check cover accepted/rejected topics, malformed payloads, duplicate delivery, reconnect, and publish failures. No hardware claims are required yet; no public broker or internet-exposed service is added.

### A12 — Add firmware Wi-Fi and MQTT

- **Roadmap:** firmware side of S07/S08. **Owner:** Jakhongir. **Depends on:** A10 and A00's agreed command schema.
- **Scope:** `firmware/`, `config/firmware/`, firmware setup/bench evidence.
- **Deliver:** local-network credentials via ignored configuration, reconnect without blocking local alarms, protocol topics, telemetry, events, receipts, and versioned command acceptance/rejection. Choose and pin a client that actually supports outbound QoS 1. Keep a serial fallback.
- **Gate:** compile and human-run broker checks demonstrate event publication, matching receipt handling, idempotent configuration commands, and local SOS during a 30-second network outage. Do not claim historical delivery unless A16's queue work is completed.

### A13 — Integrate wireless behavior, health, and portable power

- **Roadmap:** S08/S09; milestone M3. **Owner:** Ismoiljon + Jakhongir. **Depends on:** A11/A12.
- **Scope:** bootstrap/integration checks, mounting notes, sanitized network/power evidence; coordinated fixes.
- **Deliver:** same Java rules/database/UI over MQTT; explicit stale/offline recovery; current vs historical input handling; removable enclosure and power-bank demonstration. Keep operator acknowledgement separate from local alarm behavior.
- **Gate:** repeat the wired incident workflow wirelessly, interrupt network for 30 seconds, show local SOS and stale Java state, reconnect with new live readings, and confirm no duplicate incident. Record power-bank runtime/shutoff observations and an unworn motion demo. Cover T06-T08, T10, T14, and T16 as applicable. Confirm GUI configuration shows acceptance or rejection, rather than assuming a publish applied it.

### A14 — Complete baseline history, trends, export, and replay

- **Roadmap:** S10 and remaining baseline UI/configuration. **Owner:** Ismoiljon; third-member handoff possible. **Depends on:** A13.
- **Scope:** application query/export/replay services, UI history/trend/configuration controls, simulation/replay adapter, scenario fixtures and focused tests. Coordinate any port changes before editing.
- **Deliver:** bounded trend queries from sampled history, incident filters, correct CSV export, controlled recorded-data replay, and complete versioned configuration controls. Replay uses the same rules and persistence boundaries with separate provenance/identity so it cannot corrupt live history. A replay does not publish a live hardware receipt or enter physical latency measurements.
- **Gate:** CSV values and quoting match stored data, restart preserves history, malformed replay is rejected, replay is visibly labeled, and disconnected hardware stays disconnected during replay. Cover T13-T15 and rerun relevant incident/transaction checks after integration.
- **Do not add:** ten-device claims, offline hardware queues, or cloud deployment.

### A15 — Verify and prepare the course release

- **Roadmap:** S11/S12; milestone M4. **Owner:** Ismoiljon coordinates; every confirmed teammate participates. **Depends on:** A14.
- **Scope:** test fixes, setup/release docs, report materials, sanitized evidence. New features need a separate decision.
- **Deliver:** clean-checkout setup, full [TEST_PLAN.md](TEST_PLAN.md) results, two or more real GUI screenshots, architecture/class explanation, simulation explanation, hardware diagram, contributor responsibilities, live demo script, and short recorded demo. Keep private course identity data outside the public repo.
- **Gate:** run 100 physical SOS receipt trials under recorded conditions; include missing/late receipts and report actual percentiles without inventing success. Reproduce restart and failure cases. Each teammate explains their contribution and the full data flow. Record roster compliance, instructor scheduling exceptions, and actual course submission separately. No grade guarantee.
- **Release decision:** TL and teammate review the evidence. Tag or publish only when the active task authorizes it. List failures and unfinished work plainly.

### A16 — Optional extensions, separately assigned

- **Roadmap:** S13; milestone M5. **Owner:** explicitly assigned by TL. **Depends on:** A15.
- **Scope:** one approved extension per task: bounded offline queue/replay; ten simulated devices; or TLS/topic isolation.
- **Gate:** use the extension checks in the test plan. Document queue overflow and power-loss behavior, distinguish simulated from physical devices, or prove rejected unauthorized broker access. Rerun affected baseline checks. An unfinished extension must not destabilize the presentation release.

## Running a work session

1. TL checks accepted prerequisites and chooses the next unblocked card. Record the starting commit and accountable human owner.
2. Create a branch/worktree for that card. Give the agent the template, card ID, permitted paths, and evidence for prerequisites.
3. Agent implements and checks its scope. If the task grows too large, split it at a testable behavior boundary under the same card; do not silently expand scope.
4. Human owner reviews the code and can explain it. Hardware checks are performed by the team, not inferred by an agent.
5. TL/teammate runs the gate, records acceptance or requested changes, and integrates the branch. A passing branch alone does not complete an integration stage.
6. Update the task's status in its issue or acceptance record: `planned`, `in progress`, `review`, or `accepted`. Record an explicit blocker if needed. Keep this document's planned status until there is corresponding accepted evidence.

Do not ask an agent to “build the whole project” in one prompt. The next implementation task is **A00**, followed by **A01**; this planning update does not start either task or dispatch any agents.
