---
name: sync-branches
description: Propagate a branch's commits upward through its parent chain (feature/* -> develop -> main; release/* -> main), never sideways into siblings and never down into children. Use any time a change has landed on a branch and needs to be carried forward to its parent(s). Args (optional): a branch name to sync instead of the current branch.
---

# Sync branches upward

This repo's parent chain (parent = the next branch toward `main`):

    feature/*  ->  develop  ->  main
    release/*  ->  main

Only ever merge a branch **into its parent**. Never merge a parent back
into a child, and never merge one branch into an unrelated sibling
(e.g. `feature/US-05-x` must never be merged into `feature/US-06-y`, and
`develop`'s state must never be pushed down into a feature branch). If
the branch in `args` (or the current branch, if none given) doesn't match
one of the two patterns above, stop and ask what its parent is instead of
guessing.

Each branch carries its own `branch-notes/<branch-name>.md` (written by
`feature-builder`) at a path unique to that branch, so merging never
conflicts on it and never needs to overwrite or delete it — by the time a
chain reaches `main`, `branch-notes/` holds the full accumulated history
of every branch that fed into it. Don't touch these files yourself; just
let them merge forward.

## Steps

1. `git fetch origin`. Make sure the starting branch's local state
   actually matches its remote tracking branch before propagating
   anything — if it has unpushed-only local commits, say so rather than
   silently carrying them up.
2. Walk the chain one level at a time. For each step from branch `X` into
   parent `P`:
   a. Check out local `P`, fast-forward it to `origin/P` first.
   b. `git merge --no-ff X -m "Merge X into P"` — match this repo's
      existing merge-commit phrasing (see `git log --oneline --graph
      --all` for the style already in use).
   c. On a conflict: stop immediately, leave the merge unresolved exactly
      as git leaves it, and report which files conflict. Never
      auto-resolve a conflict.
   d. Run the fast gate for this step, from `transaction-service/`:
      `mvn -q -DskipTests test-compile`. This is the compile-only check
      for in-progress branches — not the full suite.
3. `P` becomes the new `X`; find P's own parent and repeat step 2, until
   the walk reaches `main`.
4. Once a merge lands on `main`, run the full suite there before anything
   is pushed: `mvn test` (from `transaction-service/`). This is the one
   point in the chain that gets the full 27-scenario run, matching this
   repo's "test only the final branch" policy. If it fails, stop, report
   the failure, and leave every merge in this chain local/unpushed rather
   than publishing a broken `main`.
5. Report the exact chain walked and the result at each step. Ask once,
   listing every branch that changed, before pushing any of them —
   matching the existing rule from the `release` skill / `release-manager`
   agent of never pushing without the user's go-ahead in this session.
   Push only the branches actually in this chain; touch no sibling branch.

## Output

Report: starting branch, full parent chain walked, compile-check result at
each intermediate step, full-suite result at `main`, and which branches
are staged locally awaiting a push (and which, if any, still need it).
