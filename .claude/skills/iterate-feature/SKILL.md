---
name: iterate-feature
description: Run one feature iteration of the transaction-service build — implement a User Story's engine classes and Cucumber tests, then verify them. Use when the user asks to build, automate, or continue a specific User Story (e.g. "/iterate-feature US-05" or "build US-06 now"). Args: <US-id>, e.g. US-05.
---

# Iterate on one User Story

Given a User Story ID in `args` (e.g. `US-05`):

1. Inside `transaction-service/`, create or check out a branch named
   `feature/<us-id-lowercase>-<short-slug>` off `develop` (create `develop`
   off `main` first if it doesn't exist yet in this repo).
2. Launch the `feature-builder` subagent with that User Story ID. It will
   look up the US's section in `DESIGN.md` via `scripts/design_lookup.py`
   before writing anything, implement the engine class(es) and the
   matching `.feature` file + step definitions, run `mvn test` itself
   before returning, and write `branch-notes/<branch-name>.md` (done /
   pending / defects / fixes / test evidence for this branch).
3. Launch the `test-verifier` subagent, scoped to that same US, to confirm
   independently: every AC under it has a passing tagged scenario, and
   nothing previously passing regressed.
4. If `test-verifier` finds a gap, hand the specific gap back to
   `feature-builder` for one correction pass. Don't loop a third time —
   surface the unresolved issue to the user instead.
5. Commit (including the new `branch-notes/<branch-name>.md`) with a
   message naming the US-id, the real scenario/step pass count from this
   run, and any defect found+fixed — matching the style of this repo's
   existing commits (e.g. `178d41d6`, `5bc393e`).
6. Report back to the user: which US, which files touched, pass count, any
   defect found. Do not merge the branch — that's `/release`'s job.
