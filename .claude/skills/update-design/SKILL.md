---
name: update-design
description: Update DESIGN.md (User Stories, ACs, HLD/LLD/flowcharts, Design Decisions, traceability matrix) when a requirement changes or a new design decision needs recording. Use when the user describes a design or requirement change, not a code change. Args: free-text description of the change.
---

# Update the design document

1. Launch the `design-architect` subagent with the change described in
   `args`, passed through verbatim.
2. It will use `scripts/design_lookup.py <id>` to pull the current text of
   anything it's about to touch before editing — require that it do this
   rather than rewriting from assumption.
3. It edits only `DESIGN.md` (and the `docs/` viewer if one is wired up to
   mirror it) — it must not touch anything under `transaction-service/src`.
4. Any new decision becomes a new `DD-xx` (next unused number, existing
   ones are never renumbered or deleted); any requirement/User Story
   change keeps the Requirement Catalog, the US's ACs, and its
   traceability-matrix row in sync in the same pass.
5. Report back: which IDs were added or changed, in one short summary —
   not a full diff dump.
