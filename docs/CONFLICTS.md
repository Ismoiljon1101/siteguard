# Conflicts and open items (A00 and A01)

Repo wins on the wire format (brief, Instructions item 1). Nothing below was changed silently. Items marked "TL" need the TL's decision.

## Conflicts

1. **Receipt kind is missing from the envelope.** `docs/PROTOCOL.md` lists `kind` as `telemetry` or `event` in its shared envelope table. Its Java receipt section and the brief's fixture `receipt_stored.json` use `"kind": "receipt"`. This fixture follows the receipt example. TL: add `receipt` to the PROTOCOL.md `kind` row. PROTOCOL.md was not edited (outside A00 scope).
2. **Operator identity (D17).** Brief Part 1 and A07 say operators are picked from a list with no password. TL instructed login with ID and password. Recorded in `docs/DECISIONS.md`. TL: confirm the A07 card wording and whether the password applies to the single-process demo or only after the split (A11B).
3. **Who evaluates motion.** `docs/PROTOCOL.md` says the ESP32 publishes MOTION events (`baseline hardware event types are SOS and MOTION`). Brief Part 2 says fast motion sampling "only produces MOTION events" in Java's storage rule, and D10 says the threshold is measured on the board in A08. Ownership of the threshold check (firmware or Java) is not stated. TL and Jakhongir: confirm firmware emits MOTION and Java only stores it.
4. **Event type ENVIRONMENT.** Brief Part 2 lists `ENVIRONMENT` as an incident type. `PROTOCOL.md` says environmental incidents come from Java readings, not from wire events. Consistent, but the incident type is absent from the wire `event_type` field. No change needed; noted so `EventType` (SOS, MOTION) stays as is.
5. **Build JDK.** `desktop/BUILD.md`, `run-offline.ps1` and `test-offline.ps1` said JDK 17 and `--release 17`. D1 requires JDK 21. Fixed in A01.
6. **Name spelling.** Brief uses "Ismail"; `AGENTS.md`, `README.md` and the stage cards use "Ismoiljon" for the team lead. Same person as far as the repo shows. TL: confirm which name the record should use.

## Open items (not conflicts, not resolved here)

- A05 OFFLINE definition and environment recovery rule: brief says propose to TL, not chosen silently.
- Helmet network credentials file (`firmware/secrets/`) must stay out of git.
- `docs/PROTOCOL.md` says SOS/MOTION are firmware events; fixtures use `simulated: true` on all examples.
