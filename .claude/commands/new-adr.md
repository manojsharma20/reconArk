---
description: Create the next decision record in docs/decisions
argument-hint: <title>
---

Create a decision record titled: $ARGUMENTS

1. Find the highest number in `docs/decisions/` and use the next one (4 digits). File name:
   `<number>-<kebab-case-title>.md`.
2. Copy the structure of an existing record: title `# <number>: <Title>`, the Status/Date table (Status `Proposed`,
   today's date), then `## Context`, `## Decision`, `## Consequences`, `## Alternatives considered`. Plain language;
   one page.
3. Add a row to the table in `docs/decisions/README.md`.
4. If it replaces an older decision, write "Replaces NNNN" under Status, and ask the user before changing the old
   record's Status to "Replaced by <number>".
