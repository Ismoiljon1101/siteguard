# Team plan

Ismoiljon Masharipov is the team lead (TL). The course proposal lists Jakhongir as the hardware/firmware owner and leaves the third teammate unconfirmed. The course hardware category expects 3-4 members; confirming the roster or obtaining the instructor's approval is still outstanding.

## Ownership

| Area | Accountable owner | Review or support |
| --- | --- | --- |
| Scope, architecture, protocol approval, and release | Ismoiljon, TL | All confirmed members |
| Java domain model, rules, dashboard, and integration | Ismoiljon | Third member when confirmed; Jakhongir reviews hardware assumptions |
| Circuit assembly, firmware, local alarms, and power | Jakhongir | Ismoiljon reviews message behavior |
| Persistence, simulation, replay, and test evidence | Ismoiljon until reassigned | Proposed third-member workstream |
| Final report, screenshots, and demonstration | Ismoiljon coordinates | Every member explains their contribution and the overall system |

These are proposed work assignments for the team's first review. The third member gets an explicit handoff and manageable tasks after joining. Work already done remains attributed to its actual author.

## TL responsibilities

Ismoiljon keeps the roadmap current and resolves scope or integration decisions. He approves changes to the firmware/Java contract, makes sure tasks have an owner, and coordinates the release demonstration.

He also owns the integration build. A module being finished on a branch does not complete a milestone until the team can demonstrate it with the rest of the system. For his own pull requests, a teammate supplies the review.

## Coordination

At the start of each work session, each member records what they finished, what they will do next, and any blocker on their task issue or in the agreed team channel. Meet briefly at each milestone to run the demonstration together.

If a task is blocked, state what is needed and identify independent work that can continue. Hardware delivery must not stop work on the Java simulator, incident rules, or storage tests.

## Decisions to confirm in the first session

| Decision | Current plan | Owner |
| --- | --- | --- |
| Team roster | Two confirmed names; third member still needed | Ismoiljon |
| Desktop UI | Swing, matching the proposal | Ismoiljon |
| Persistence | Local SQLite through JDBC | Ismoiljon; later persistence owner |
| Initial transport | USB serial, then Wi-Fi/MQTT | Ismoiljon + Jakhongir |
| Firmware environment | Arduino IDE with ESP32 support; verify MQTT library capabilities | Jakhongir |
| Development network | Laptop and helmet on a shared local network | Jakhongir + Ismoiljon |
| Parts and mounting | Check exact module variants before purchase | Jakhongir |
| Budget | KRW 100,000 total; November 13 minimal AliExpress list with cash reserve | Ismoiljon + Jakhongir |
| GitHub access | Collect teammate GitHub handles before inviting collaborators | Ismoiljon |

## Scope decisions

Baseline work comes before offline event replay, multiple simulated devices, or TLS/topic permissions. Ismoiljon may defer extensions to protect the Week 15 demonstration. Completing work in Week 16 requires the instructor's permission.

If the team changes the baseline, update the proposal separately where required, along with the roadmap, protocol, and test plan. Do not report a planned feature as completed.

## Contribution evidence

Keep focused commits and reviewed pull requests. Each member should retain enough notes to explain their design, tests, and tradeoffs. The final report must distinguish team contributions, reused libraries, physical-device measurements, and simulation results.
