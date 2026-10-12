---
description: Review the current branch with the architecture and security reviewer subagents
---

1. Get the diff: `git diff origin/main...HEAD` (and uncommitted changes).
2. Run the `architecture-reviewer` and `security-reviewer` subagents on it, in parallel.
3. Merge their findings into one list, most important first, each with file:line, the problem and the fix.
   Don't edit files unless the user asks.
