---
name: bootstrap-project
description: Scaffold a brand-new transaction-service project from true scratch (Step 01) — reuses this repo's DESIGN.md as the requirements source of truth, builds the Maven/Cucumber/Extent test harness and true-stub engine classes for every User Story, all currently failing (red baseline) until /iterate-feature implements each one. Use once, at the very start of a from-scratch agentic build, before running /iterate-feature for any User Story. Args: target directory path for the new project.
---

# Bootstrap a brand-new project (Step 01: red baseline)

1. `args` must be a target directory path. If missing, stop and ask the
   user for it — never pick a location on your own.
2. If the path already exists and is non-empty, stop and ask before
   touching it; never silently overwrite or merge into an existing
   folder's contents.
3. Create the target directory and `git init` inside it.
4. Launch the `project-bootstrapper` subagent, telling it the target path
   and that this repo's `DESIGN.md` (same User Stories/ACs) is the reused
   source of truth. It copies the test harness, interfaces, and spec
   (feature files, step definitions, DESIGN.md, reporting config)
   verbatim, and replaces every engine/business-logic class with a real
   stub that throws until implemented — see its own instructions for the
   exact list of what's copied vs. stubbed.
5. It will run the suite itself and confirm the result is red for the
   right reason (`UnsupportedOperationException` from a stub on every
   scenario, not a compile error, not an accidental pass).
6. Commit the scaffold as the project's first commit.
7. Report back: new project path, User Story/scenario counts, the real
   failing-test count, and the next step — `/iterate-feature US-01`, then
   each subsequent US in order, then `/trace-check`, then `/release`.

This skill never produces a passing test on its own — if it ever reports
green, that's a bug in the bootstrap, not a milestone.
