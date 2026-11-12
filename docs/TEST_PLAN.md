# Verification plan

All tests below are **not run**. Targets describe planned acceptance criteria, not measured results. Update each result only after running the scenario on a recorded revision.

## Test matrix

| ID | Scenario | Expected result | Initial owner |
| --- | --- | --- | --- |
| T01 | Run deterministic normal, warning, and fault inputs | Same rules operate for simulation and hardware-normalized input; source is labeled | Ismoiljon |
| T02 | Send malformed, oversized, or unsupported-version messages | Reject safely, report an input fault, and keep UI responsive | Ismoiljon |
| T03 | Trigger physical SOS without the laptop/network | Local buzzer and LED operate; button debounce avoids duplicate presses | Jakhongir |
| T04 | Send the same event repeatedly, including after Java restart | One incident and its original identity remain; duplicate delivery can get another durable receipt | Ismoiljon |
| T05 | Acknowledge, annotate, and resolve an incident | Valid transitions and notes are stored; previous actions remain; invalid transitions are rejected | Ismoiljon |
| T06 | Stop valid telemetry while leaving the UI open | Stale indicator appears within 6 seconds; no old reading is displayed as fresh | Ismoiljon + Jakhongir |
| T07 | Remove the network for 30 seconds and press SOS | Local alarm continues; Java shows the device offline/stale; baseline does not imply delivery of outage events | Jakhongir |
| T08 | Restore connection and send a new physical event | Device health recovers on new valid live input; event is stored without duplicate incidents | Ismoiljon + Jakhongir |
| T09 | Run 100 SOS round-trip trials on the test LAN | Target: at least 95 receive a matching Java durable receipt within 1 second; missing receipts count as misses | Ismoiljon + Jakhongir |
| T10 | Disconnect a sensor or supply invalid values | Fault is explicit; unrelated valid readings and local SOS remain usable | Jakhongir |
| T11 | Restart Java after committed incidents and actions | History and incident states recover from SQLite | Ismoiljon |
| T12 | Simulate a database write failure | No false stored receipt; UI shows the storage fault and remains usable | Ismoiljon |
| T13 | Replay an old capture while a physical device is disconnected | Replayed data is labeled and cannot mark the physical device as live | Ismoiljon |
| T14 | Test configuration and threshold boundaries | Unsupported settings are rejected; accepted version is visible; sustained-warning rule avoids repeated warnings | Ismoiljon + Jakhongir |
| T15 | Compare CSV export to stored incidents | Values, IDs, states, timestamps, and notes agree with the database; quoting is correct | Ismoiljon |
| T16 | Run on power bank with mounted electronics | Record runtime and any shutoff/restart; button remains accessible and mounts remain secure | Jakhongir |

The third member may take over storage, replay, and evidence tasks after an explicit handoff. Do not assign a GitHub user or claim a contribution before confirming it.

## Measuring SOS delivery

The firmware records a monotonic start time when it accepts an SOS input. Java commits the source event and incident, then sends the matching receipt. Firmware records the elapsed time on receipt using the same clock. This includes delivery to Java, persistence, and return delivery; it is not a one-way latency measurement.

Run 100 distinct events under documented LAN conditions. Define the timeout before testing. Retried copies keep the original event identity and do not become additional trials. Missing or late receipts count against the 95-of-100 target. Report median, 95th percentile with its calculation method, maximum observed completed round trip, and the number of timeouts. Do not calculate the success rate using only the successful deliveries.

Measure one local threshold rule at a time and record its configuration. Motion events are controlled movements, not a real injury/fall evaluation. Do not use unsafe impact tests.

## Evidence record

For each test, retain:

- Test ID, operator, date, and Git commit.
- Exact ESP32/module variants, firmware configuration, Java environment, broker setup, and connection type.
- Whether the source was the physical helmet, simulator, or recorded replay.
- Procedure, target, observed result, and pass/fail status.
- Sanitized logs, screenshots, measurements, and a description of any failure.

Publish only sanitized captures in `evidence/`. Keep local databases, passwords, and identifying course data outside Git. Do not change a target after seeing results without recording the decision and reason.

## Optional extension tests

Before claiming offline replay, fill the queue past its configured capacity, disconnect/reconnect repeatedly, replay duplicates, and test the specified behavior after power loss. Record dropped events and the overflow policy. A RAM queue must not be described as power-loss durable.

Before claiming multiple-device support, run ten clearly labeled simulated devices with overlapping sequence numbers but separate device and boot identities. Verify independent health and incident histories. This does not demonstrate ten physical helmets.

Before claiming TLS or topic isolation, show valid clients connecting and invalid certificates, credentials, or topic access being rejected under the documented configuration.

## Presentation checklist

Use a clean checkout of the release revision and the recorded setup instructions. Demonstrate normal input, a physical SOS, incident acknowledgement and resolution, saved history after restart, and local alarms during a disconnect. Keep simulation available as a labeled fallback and show at least two GUI states in the report.

Each teammate must explain their work and trace one event through the overall system. Portfolio and CV descriptions should use only features and measurements demonstrated by that release.
