# Firmware / Java protocol v1: draft

Ismoiljon and Jakhongir must agree on this contract during S00, before implementing separate transport adapters. This document proposes the wire format; it is not a tested implementation.

## Shared envelope

Use UTF-8 JSON objects. For serial input, send one JSON object per line. For MQTT, publish one object per message. Reject payloads larger than 4 KiB before parsing.

| Field | Meaning |
| --- | --- |
| `schema_version` | Integer `1`; reject unsupported versions explicitly |
| `kind` | `telemetry` or `event` for device-to-Java messages |
| `device_id` | Stable non-personal device identity, such as `helmet-001` |
| `boot_id` | New unique session identity for each firmware boot |
| `seq` | Increasing integer within one boot; retries keep the original sequence |
| `uptime_ms` | Non-negative monotonic device time within this boot |
| `simulated` | Boolean; true for synthetic input |

Use `(device_id, boot_id, seq)` as the source identity. Do not deduplicate on wall-clock time or sequence number alone. The live MQTT adapter must not trust a payload's `simulated` flag to authenticate its sender; adapter configuration and broker credentials determine accepted device sources.

## Telemetry example

```json
{
  "schema_version": 1,
  "kind": "telemetry",
  "device_id": "helmet-001",
  "boot_id": "demo-boot-a",
  "seq": 10,
  "uptime_ms": 10000,
  "simulated": true,
  "temperature_c": 27.4,
  "humidity_pct": 58.2,
  "accel_mps2": {"x": 0.1, "y": 0.0, "z": 9.8},
  "sensor_status": {"environment": "ok", "motion": "ok"}
}
```

All examples are synthetic. Units are Celsius, percent relative humidity, and metres per second squared. Humidity must be between 0 and 100. Use `null` for an unavailable sensor value and a matching `fault` sensor status; never reuse an old value while labeling it fresh. Reject invalid combinations. A field being numerically valid does not make it safe for a worker.

Firmware should publish current telemetry about once per second. Motion sampling is separate and faster; a short movement must not disappear between telemetry samples. Java records receive time separately and handles out-of-order sequences without moving a device's current reading backward.

## Event example

```json
{
  "schema_version": 1,
  "kind": "event",
  "device_id": "helmet-001",
  "boot_id": "demo-boot-a",
  "seq": 11,
  "uptime_ms": 10420,
  "simulated": true,
  "event_type": "SOS"
}
```

Baseline hardware event types are `SOS` and `MOTION`. The canonical event identity in this example is `helmet-001/demo-boot-a/11`. Motion events mean sudden movement exceeded the demonstration rule, not that a fall or injury occurred. Environmental incidents are evaluated by Java from readings; do not also create them from duplicate firmware messages.

## Java receipt

```json
{
  "schema_version": 1,
  "kind": "receipt",
  "event_id": "helmet-001/demo-boot-a/11",
  "status": "stored"
}
```

Java sends `stored` only after the event and resulting incident transaction commit, or after confirming that the same event was already committed. A receipt is distinct from an operator acknowledgement. Firmware matches it to a pending event and measures elapsed time using its own monotonic clock. Replaying an old event must not create another incident or enter a live-latency sample.

If storage fails, do not send `stored`. Preserve the local helmet alarm and show the storage error in Java. The first wired test can report a missing receipt directly; baseline delivery is not guaranteed during disconnects.

## MQTT topic plan

| Topic | Publisher | Purpose |
| --- | --- | --- |
| `siteguard/v1/devices/{device_id}/telemetry` | ESP32 | Current readings |
| `siteguard/v1/devices/{device_id}/events` | ESP32 | SOS and motion events |
| `siteguard/v1/devices/{device_id}/receipts` | Java | Durable event receipts |
| `siteguard/v1/devices/{device_id}/commands` | Java | Versioned configuration commands |
| `siteguard/v1/devices/{device_id}/command-results` | ESP32 | Accepted configuration version or rejection |

Plan for QoS 0 telemetry and QoS 1 events/receipts, without retained telemetry or event messages. The selected firmware client must support outbound QoS 1; confirm this in S07. Protocol-level delivery does not replace database deduplication or the Java receipt.

Reject a message when its payload device ID does not match its topic. Last-will messages may help connection display but cannot prove current sensor data is valid. Configure allowed devices explicitly for the single-device baseline.

## Configuration commands

Agree on the command schema before S07. It must include a command ID, device ID, configuration version, and supported threshold values with units. Firmware validates the full request before applying it and reports acceptance or rejection. Retrying the same command must not produce a second configuration change.

Changing thresholds must not disable the SOS button. A remote incident acknowledgement must not automatically silence a local alarm. Local reset behavior must be specified and demonstrated separately.

## Optional queue and replay

Do not claim offline delivery until S13 is complete. Choose a bounded queue capacity and explicit overflow policy, store original event identities, and retry until a matching durable receipt is received. Historical replay must not make the dashboard report that the device was connected throughout an outage.

Document whether events are held in RAM or durable storage. RAM buffering during a network outage does not preserve events across power loss. Queue capacity, priority of SOS events, and replay behavior need fault tests before being described as reliable.
