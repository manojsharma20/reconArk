---
description: Run the full local verification (Java, frontend, deploy files, docs) and summarise the results
---

Run in order and report a short pass/fail table with the first error of each failure:

1. `cd code && ./gradlew build` (needs Docker for integration tests)
2. `cd code/frontend && npm ci && npm run typecheck && npm test && npm run build && npm run format:check`
3. `cd code && docker compose -f deploy/docker-compose.yml --profile app config --quiet`
4. `cd docs/_lint && npm ci && npm run lint`
5. Optional, if Docker can run containers and the user agrees: `cd code && deploy/start.sh && deploy/smoke-test.sh`
6. `git status --short`: list files that should not be committed (build output, data, secrets).

Fix failures your current change caused. Report pre-existing failures; don't fix them silently.
