---
name: trace-check
description: Audit DESIGN.md's traceability matrix against the actual Cucumber feature files and real test results, in both directions — no code or doc changes. Use anytime to sanity-check the repo, or before a release. Args (optional): a single User Story ID to scope the audit; omit for a full audit.
---

# Audit traceability (read-only)

1. Launch the `test-verifier` subagent. If `args` names a single User
   Story ID, pass it through to scope the audit to that US; otherwise run
   a full audit.
2. It will run `mvn test` in `transaction-service/` for real pass/fail
   counts, then check every `AC-xx.y` in `DESIGN.md` has a matching
   `@AC-xx.y`-tagged scenario, and every such tag in the feature files has
   a matching AC in `DESIGN.md` — both directions.
3. Report exactly what it reports: real test counts, ACs with no scenario,
   scenarios with no matching AC. A clean result is "0 gaps, N/N passing"
   — state that plainly if that's what came back, don't add hedging.

This skill never edits anything. If gaps are found, tell the user which
skill fixes which gap: `/iterate-feature <US-id>` for a missing scenario,
`/update-design` for a scenario that tests something undocumented.
