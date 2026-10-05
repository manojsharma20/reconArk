---
description: Create the next-numbered architecture decision record
argument-hint: "<decision title>"
---

Create a new ADR titled: $ARGUMENTS

1. Find the highest number in `docs/adr/ADR-*.md` and use the next one (4 digits). The slug is the title in
   kebab-case.
2. Use the exact structure of `docs/adr/ADR-0026-hybrid-plugin-kernel.md`:
   - the header table (Status `Proposed`, today's date, Deciders, Details links, Relates to);
   - Context;
   - Decision;
   - Consequences;
   - Alternatives considered and rejected;
   - Revisit when.
3. If it supersedes an accepted ADR:
   - say so in "Relates to";
   - ask the user before changing the old record's Status line to `Superseded by ADR-NNNN`. The guard hook
     protects accepted ADRs, so the user must approve that one edit.
4. Add a row to `docs/adr/README.md`.
5. Reference the ADR from the HLD/LLD sections it affects.
