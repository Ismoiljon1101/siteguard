# Delivery roadmap

All tasks start as **planned**. The initial repository contains documentation only. Work proceeds by completion gates, not by invented dates; the course presentation target is Week 15.

Use [BUILD_STAGES.md](BUILD_STAGES.md) to execute these roadmap tasks as smaller AI-agent assignments. Its A00-A16 cards specify dependencies, allowed files, checks, and human acceptance. Use [AGENT_TASK_TEMPLATE.md](AGENT_TASK_TEMPLATE.md) for each assignment; the S00-S13 tasks below remain the milestone-level plan.

Ismoiljon is TL and the temporary owner of unfilled third-member tasks. The team can reassign those tasks after the roster is confirmed.

## Milestones

| Milestone | Demonstration required | Tasks |
| --- | --- | --- |
| M0: agree on the plan | Confirm ownership and roster; approve protocol v1 and exact parts | S00 |
| M1: first working path | Simulated reading reaches the Java UI; ESP32 independently reads sensors and triggers SOS | S01-S03 |
| M2: wired integration | A physical SOS becomes a saved incident with acknowledgement and resolution | S04-S06 |
| M3: wireless system | Same behavior over MQTT; portable local alarms and visible connection loss | S07-S09 |
| M4: presentation release | Reproducible tests, screenshots, report, and live demo | S10-S12 |
| M5: optional extensions | Demonstrate each chosen extension and its limits | S13 |

## Task list

| ID | Task | Owner | Depends on | Done when |
| --- | --- | --- | --- | --- |
| S00 | Confirm roster, scope, parts, and protocol | Ismoiljon + Jakhongir | None | Roster is confirmed or instructor approval is recorded; module variants and protocol decisions are documented. |
| S01 | Scaffold the Java application | Ismoiljon | Protocol discussion in S00 | Maven wrapper, supported JDK, Swing shell, test runner, and CI instructions are committed; a clean checkout builds and tests. |
| S02 | Bring up the ESP32 and sensors | Jakhongir | Parts selection in S00 | Record exact board/toolchain, pin map, and wiring; read both sensors over serial; test buzzer, LEDs, and debounced SOS without Java. |
| S03 | Build domain types and simulator | Ismoiljon; proposed third-member handoff | S01 | Typed readings/events, input interface, and deterministic normal/warning/fault scenarios feed the same processing pipeline. |
| S04 | Implement the serial protocol | Ismoiljon + Jakhongir | S01, S02; protocol v1 frozen | Java parses bounded JSON lines, validates payloads, and displays the physical device; malformed lines and reconnects are tested. |
| S05 | Add incident storage and state transitions | Ismoiljon; proposed third-member handoff | S03 | SQLite migration and repository layer store deduplicated source events, incidents, and action history; transitions survive restart. |
| S06 | Complete the wired dashboard | Ismoiljon | S04, S05 | Physical SOS creates one incident; an operator can acknowledge, add a note, resolve, filter, and export; UI remains responsive. |
| S07 | Add MQTT and local broker setup | Jakhongir: publisher; Ismoiljon: Java adapter | S06 | Document a private LAN broker with credentials; physical events and durable Java receipts work over MQTT; chosen firmware client supports the agreed publish QoS. |
| S08 | Handle stale devices, faults, and recovery | Ismoiljon + Jakhongir | S07 | Stale data is visibly marked, invalid data is rejected, reconnects do not duplicate incidents, and local SOS still works during an outage. |
| S09 | Mount and power the prototype | Jakhongir | S02, S07 | Electronics are enclosed with strain relief and removable mounting; power-bank behavior and an unworn motion demonstration are checked. |
| S10 | Add trends, replay, and CSV export checks | Ismoiljon; proposed third-member handoff | S05, S06 | Trend history uses stored samples; recorded replay exercises rules without operating a live helmet; CSV rows match saved incidents. |
| S11 | Run acceptance and fault tests | Ismoiljon coordinates; all members participate | S08-S10 | Complete the test matrix, including 100 SOS trials; publish actual results and conditions with no unmarked synthetic evidence. |
| S12 | Prepare the presentation release | Ismoiljon coordinates; all members contribute | S11 | Tag a working revision, verify clean setup, prepare report, two GUI-state screenshots, architecture diagram, demo script, and short video. |
| S13 | Add selected extensions | Assign at M4 review | S12 | Each chosen extension has its own test evidence; incomplete work is documented and not included in release claims. |

S01 and S02 can proceed independently after the protocol and parts discussions. The simulator and storage work can continue while hardware is in transit. Do not wait for a complete UI before testing one physical message end to end.

## Completion gates

### M1: first working path

The Java app receives a deterministic simulated reading and keeps the UI responsive. Independently, the ESP32 reports valid sensor data and triggers a local SOS alarm. This proves both sides can be developed before they are integrated.

### M2: wired integration

Connect the actual helmet. Trigger SOS, observe one incident in Java, acknowledge it, resolve it with a note, and restart the app to recover its history. Reject a malformed message without crashing. This is the minimum integrated demonstration.

### M3: wireless system

Repeat M2 over a local MQTT broker. Disconnect the network and show both the stale/disconnected UI and the working local SOS. Reconnect and confirm new events are handled. Historical event delivery during an outage is not a baseline guarantee.

### M4: release

Use [TEST_PLAN.md](TEST_PLAN.md) to record pass/fail results. Report missed latency targets and other failures directly. Every teammate should be able to explain their contribution and trace a sensor message through the overall system.

## Optional work, after the release gate

- A bounded offline event queue with replay and Java receipts. Specify its capacity, overflow policy, and behavior after power loss.
- Ten clearly labeled simulated device identities, including collisions, reconnects, and independent incident histories.
- TLS and per-device topic permissions, with a reproducible certificate setup and rejected-access tests.

## Before course submission

- Confirm the required 3-4-member roster or the instructor's exception.
- Check names and student IDs in the private proposal.
- Have the team review task ownership and the planned scope.
- Submit the current proposal through eCampus and retain the submission confirmation.

The team must perform the eCampus submission; this repository does not represent a submitted proposal.
