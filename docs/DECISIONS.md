# Accepted decisions (A00)

Source: `docs/SITEGUARD_BUILD_BRIEF.md`, Part 1. Thresholds are placeholders until A05 tests and A08 bench measurements exist. Nothing here is a measured result.

## Accepted by the TL (Ismail, chat, 2026-11-18)

| # | Decision | Record |
|---|---|---|
| D1 | Java release: JDK 21 (LTS) | Build moved to JDK 21 in A01 (`desktop/pom.xml`, `desktop/*.ps1`, CI workflow) |
| D9 | Environment rule: at or above 35 °C for 3 consecutive samples | Placeholder until bench measurement |
| D10 | Motion rule: single sample above a threshold | Threshold empty (NULL) until measured in A08; labelled "sudden movement", never "fall" |
| D11 | Local SOS alarm latches until a deliberate hold of the SOS button | Hold duration chosen in A08 |
| D13 | Storage: local SQLite file on the server machine; demo data is fictional | |
| D14 | Stored telemetry at most 1 Hz; no automatic purge | Fast motion sampling is never stored continuously |

## TL instruction, 2026-11-18 (chat)

The TL instructed that all PROPOSED items in Part 1 are built. Status of those items is "built on proposal by TL instruction". They are not measured or verified, and the firmware owner review (Jakhongir) is still pending for D4, D7, D11, D15 and the helmet network section.

| # | Decision | Status |
|---|---|---|
| D2 | Package root `com.siteguard` | Built on proposal by TL instruction |
| D3 | Maven wrapper in `desktop/` | Built on proposal by TL instruction |
| D4 | Arduino-ESP32 core (confirm in A08) | Built on proposal by TL instruction; firmware review pending |
| D5 | USB serial first, then MQTT | Built on proposal by TL instruction |
| D6 | Protocol v1 as in `docs/PROTOCOL.md`, fixtures in `fixtures/protocol/` | Built on proposal by TL instruction |
| D7 | About 1 Hz telemetry; motion sampled separately and faster | Built on proposal by TL instruction; firmware review pending |
| D8 | Stale after 5 s with no valid reading | Built on proposal by TL instruction |
| D12 | Remote acknowledgement never silences the local alarm | Built on proposal by TL instruction |
| D15 | Firmware receives `stored` only after event and incident commit | Built on proposal by TL instruction; firmware review pending |
| D16 | Helmet Wi-Fi: 2.4 GHz, WPA2/WPA3-Personal, authenticated Mosquitto, secrets file ignored by git | Built on proposal by TL instruction |
| D18 | Data model v3 (site, zone, operator, assignment, health, config version tables) | Built on proposal by TL instruction |
| D19 | Manager dashboard per Part 3 | Built on proposal by TL instruction |
| D20 | SOS escalation at 30 s and 120 s (placeholders) | Built on proposal by TL instruction |
| D21 | Resolution categories: False alarm, Test, Assistance given, No action needed, Other | Built on proposal by TL instruction |
| D22 | Roles: SAFETY_DIRECTOR, SITE_MANAGER, SUPERVISOR | Built on proposal by TL instruction |
| D23 | Zones defined per site; no GPS | Built on proposal by TL instruction |

## Replaced

| # | Brief proposal | Replacement |
|---|---|---|
| D17 | Operator picks a name from a list, no password (password login only when split, A11B) | Operator login with ID and password, per TL instruction. Replaces the brief's no-password picker. See `docs/CONFLICTS.md`. |
