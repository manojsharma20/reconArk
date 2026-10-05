---
description: Review the current branch against the Lego rules, ADRs and security baseline using reviewer subagents
---

1. Collect the diff: `git diff $(git merge-base HEAD origin/main)...HEAD` and untracked files.
2. Launch the `architecture-reviewer` and `security-reviewer` subagents in parallel with that diff and the list of
   changed files.
3. Merge their findings into one list ordered by severity (blocker, major, minor). Each finding names its file and
   line, the rule broken (CLAUDE.md section, ADR or LLD section) and a concrete fix.
4. Don't change code during the review. Ask the user which findings to fix.
