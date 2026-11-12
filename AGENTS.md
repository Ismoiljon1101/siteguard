# Working with AI agents

This repository is a Java course project. Ismoiljon is the team lead. Start with [docs/BUILD_STAGES.md](docs/BUILD_STAGES.md); use one stage card per implementation task. The repository currently contains plans, not a working application.

## Before editing

1. Read the assigned stage, its dependencies, and its linked requirements. Read the existing code and build instructions before proposing replacements.
2. Check Git status. Preserve unrelated work. Use a separate branch/worktree when another contributor is editing the repository.
3. Check that prerequisite stages actually passed their gates. A document describing a feature is not proof it exists.
4. State the files you expect to change and the checks you will run. Work only inside the assigned scope. Report a necessary cross-scope change to the TL with a concrete proposal.

## Implementation rules

- Java owns the desktop UI, environmental incident rules, incident workflow, and persistence. Firmware owns sampling, debounce, and independent local alarms.
- Keep domain and application code independent of Swing, MQTT, serial libraries, and JDBC implementations. Use the agreed ports instead of inventing a second processing path.
- Follow `docs/PROTOCOL.md`. Do not silently change wire fields, units, event identities, database semantics, or shared Java interfaces. Propose the change and its affected stages to the TL first.
- Do not add frameworks, services, cloud dependencies, sensors, or optional features outside the assigned stage.
- Use the existing build, formatter, dependency versions, and test conventions. Pin new dependencies and explain why each is needed. Never guess a command or claim it ran.
- Test behavior and failure cases appropriate to the stage. Keep clocks, fixtures, and simulator scenarios deterministic where needed.
- All Swing updates run on the event thread; serial/network/database/file I/O runs off that thread.
- Label simulation and replay. Never fabricate physical measurements, passing checks, screenshots, team contributions, or latency results.
- Do not include credentials, personal course data, local databases, or unsanitized device captures in commits.
- Do not flash hardware, change wiring, buy items, publish, push, merge, or claim stage approval unless the active user task authorizes that action. A stage card does not grant that permission by itself.

## Finish the handoff

Use [docs/AGENT_TASK_TEMPLATE.md](docs/AGENT_TASK_TEMPLATE.md). Report the stage ID, changes, exact checks and results, remaining limitations, and any changed contracts. Keep a stage at `review` until its gate has been demonstrated and the TL accepts it. Do not edit another contributor's status or contribution record.
