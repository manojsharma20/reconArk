---
description: Run the full local verification (Gradle build, frontend, diagrams) and summarise the results
---

Run, in order, and report a concise pass/fail table with the first error of each failure:

1. `./gradlew build`
2. `cd frontend && npm ci && npm run typecheck && npm test && npm run build && npm run format:check`
3. `cd tools/docs-lint && npm ci && npm run lint`
4. `git status --short`: list any files that should not be committed (build outputs, secrets).

Fix failures that your current change caused. For pre-existing failures, report them; don't fix them silently.
