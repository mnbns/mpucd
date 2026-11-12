---
name: release
description: Finalize a release of transaction-service — full test run, RELEASE_NOTES.md and README.md sample-output update with real evidence, branch merge and tag. Use when the user says all intended User Story iterations are done and it's time to ship. Takes no args.
---

# Release

1. Launch the `test-verifier` subagent for a full (unscoped) audit. Do not
   continue if it reports any AC without a passing scenario, any failing
   scenario, or any untraced tag — surface the specific gap to the user
   instead and stop.
2. Only once that comes back clean, launch the `release-manager` subagent.
   It will run `mvn clean test` for real counts, capture a real sample run
   for the README, and update `RELEASE_NOTES.md` and `README.md` with that
   real evidence (never invented numbers) — `RELEASE_NOTES.md` must always
   cover Design, What's Implemented, % Complete, Pending, Known Defects,
   and Future Plans (see `release-manager`'s own instructions for what
   each section means). For the branch merge, use the `sync-branches`
   skill to walk the completed feature branch(es) up through `develop`
   into `main` rather than merging ad hoc, then tag the version. It will
   not push to any remote unless the user has already approved a push in
   this session.
3. Report back: the final tag, the real test counts, which branches were
   merged, and whether a push is still pending and needs the user's
   go-ahead.
