# Working on SiteGuard

Ismoiljon Masharipov is the team lead. The team follows the ownership and review rules in [TEAM_PLAN.md](docs/TEAM_PLAN.md).

## Before starting

Choose a task from [ROADMAP.md](docs/ROADMAP.md). Confirm the owner, dependencies, and completion criteria with the TL. A contributor can create a GitHub issue using the task template and reference the roadmap ID, such as S04. Keep one active implementation task per person unless a hardware delivery is blocking it.

For AI-assisted implementation, choose the corresponding card from [BUILD_STAGES.md](docs/BUILD_STAGES.md), read [AGENTS.md](AGENTS.md), and use the [agent task template](docs/AGENT_TASK_TEMPLATE.md). Assign one bounded stage per session. Isolate concurrent work in separate branches/worktrees and keep shared contracts under TL review. The human owner must understand, verify, and explain the resulting code.

## Branch and pull request process

1. Create a short branch, such as `feat/s04-serial-input` or `docs/protocol-v1`.
2. Keep the change focused on one task. Update the relevant design document if behavior changes.
3. Run the checks listed for that task and attach evidence to the pull request.
4. Request a teammate's review. Ismoiljon coordinates integration; another teammate reviews his own changes before merge.
5. Use a squash merge after review, then update the roadmap task's status in a follow-up change if needed.

These are team rules. Branch protection and required GitHub checks are not configured by this initial planning commit.

## What reviewers should check

- Does the change satisfy the linked task's completion criteria?
- Does it preserve the protocol, incident transitions, and offline behavior?
- Are failure cases tested, including duplicates, missing data, and disconnections where relevant?
- Are observed results distinguished from targets and simulated data?
- Can another teammate reproduce the change from the documented setup?

Keep credentials, local databases, student IDs, and raw personal data out of commits. Use fictional device IDs in examples. Add a sanitized configuration example when setup needs new values.

## Build and checks

There is no executable application or firmware in the repository yet. Task S01 will establish the Java build and test commands. S02 will record the firmware toolchain and board configuration. After those tasks, every behavior change must include the corresponding build/test result.

Do not add badges or performance claims until the repository contains evidence supporting them.
