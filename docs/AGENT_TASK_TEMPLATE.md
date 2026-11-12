# Agent task and handoff template

Copy this into a new agent session or GitHub task. Replace the placeholders before sending. One task should implement one stage card or one explicitly bounded part of it.

```text
Implement SiteGuard stage <Axx: title> from docs/BUILD_STAGES.md.

Read AGENTS.md, the stage card, and the references named in that card.
Inspect the repository before editing. Do not assume planned code exists.

Starting revision: <commit>
Prerequisites accepted by TL: <stage IDs and evidence links>
Scope: <allowed paths from the stage card>
Additional constraints: <none, or specific constraints>

Deliver the stage's required behavior and checks. Preserve unrelated work.
Do not implement later stages or change frozen contracts without the TL's
decision. If a prerequisite is missing, explain it and complete independent
work that does not depend on that missing decision.

At handoff, report:
1. Stage ID and concrete behavior delivered.
2. Files changed and any contract/migration changes.
3. Exact commands run, their results, and evidence paths.
4. Manual checks still needed; do not mark them passed.
5. Limitations or blockers and the next compatible stage.

Leave the stage ready for review. Do not merge or publish unless this task
explicitly authorizes it.
```

## TL acceptance record

The TL records this in the task issue or pull request after reviewing the result:

```text
Stage: Axx
Implementation revision:
Reviewer:
Automated checks and results:
Manual demonstration and evidence:
Unresolved limitations:
Decision: accepted / changes required
Next tasks unblocked:
```

Do not put real passwords, student IDs, private logs, or full local configuration into an agent prompt or a public task. Use sanitized examples and fictional device IDs.
