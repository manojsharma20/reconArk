# CLAUDE.md: working on reconArk

reconArk reconciles two transaction files (left vs right) and explains every difference. Keep it **simple**: the
code must stay readable for a developer with 2–3 years of experience, and the docs for a business consultant and a
tester. Read `docs/README.md`, then `docs/02-architecture.md` and `docs/03-detailed-design.md` before changing code.

## 1. Layout

| Path | What |
|---|---|
| `docs/` | All documentation (Markdown + Mermaid). `docs/decisions/` holds decision records |
| `code/backend/common` | Model records + plugin interfaces (`FileFormat`, `FieldComparator`, `ReportWriter`, `FileStore`, `PluginRegistry`). Plain Java |
| `code/backend/plugins/*` | One plugin per module: implementation + `@AutoConfiguration` + `AutoConfiguration.imports` + tests |
| `code/backend/services/*` | `config-service` :8081, `ingestion-service` :8082, `report-service` :8083 |
| `code/recon-engine/engine-core` | Kafka messages, `Matcher`, `MatchKeys`, engine SQL migrations. Plain Java |
| `code/recon-engine/manager` | :8090 run lifecycle (`RunService`), Kafka publisher/listener, watchdog |
| `code/recon-engine/worker` | :8091 `LoadJob`, `MatchJob`, `RecordStore` |
| `code/frontend` | React + TypeScript; modules in `src/modules/*`, registry `src/shell/modules.ts`, switches in `public/config.json` |
| `code/deploy` | docker compose, Dockerfiles, nginx, Prometheus, Grafana, Helm, sample data, `start.sh`, `smoke-test.sh` |

## 2. Rules

1. **Two folders only at the top:** documentation in `docs/`, code in `code/`. No new top-level folders.
2. **New behaviour is a plugin, not an `if`.** A new format, comparator, report or storage is a new module under
   `code/backend/plugins/` implementing an interface from `backend/common`. Never add partner- or format-specific
   branches to services or the engine.
3. **`common` and `engine-core` stay plain Java** (no Spring, no database, no Kafka). Plugins never depend on each
   other or on a service. Services never depend on each other's code; they talk REST (or Kafka for the engine).
4. **Controller → Service → Repository.** SQL lives in repositories (Spring `JdbcClient`/`JdbcTemplate`, plain SQL).
   No JPA. Records for data. Constructor injection. No Lombok.
5. **Each service owns one database schema** and creates it with Flyway (`db/migration/<schema>/V<n>__<what>.sql`).
   Never edit a released migration; add a new one. report-service only reads `engine`.
6. **Engine safety:** change the database first, send Kafka messages after the commit; jobs must be idempotent
   (a retry must give the same result); the manager ignores stale or duplicate results. Keep it that way.
7. **Messages:** topics `reconark.<area>.<what>.v<N>`; JSON; key = run id. Changing a message shape needs a new
   version or a backwards-compatible change.
8. **Never log file contents or customer data.** Error messages name lines and fields, not values.
9. **Docs change with the code.** If behaviour in `docs/` changes, update the doc and its diagrams in the same PR.
   New services, plugin kinds or technologies need a decision record in `docs/decisions/`.
10. **Keep it simple.** Prefer fewer classes and plain code. If a reviewer with 2–3 years of experience would not
    understand it in one read, simplify or comment it.

## 3. Commands (run from `code/` unless noted)

```bash
./gradlew build                                     # everything; integration tests need Docker
./gradlew :recon-engine:engine-core:test            # one module
./gradlew test --tests '*MatcherTest'               # one class
cd frontend && npm ci && npm run typecheck && npm test && npm run build && npm run format:check
cd ../docs/_lint && npm ci && npm run lint          # from code/: Mermaid diagrams in docs/ must parse
docker compose -f deploy/docker-compose.yml up -d   # infrastructure for local development
deploy/start.sh && deploy/smoke-test.sh             # whole system + end-to-end check
```

Toolchain: Java 25 (Gradle toolchain), Gradle wrapper in `code/`, Node 22+. Versions: Spring Boot in
`code/gradle/libs.versions.toml` (it manages Kafka, Flyway, Jackson, Testcontainers…), npm in
`code/frontend/package.json`.

## 4. Recipes

Slash commands in `.claude/commands/`:

| Command | Does |
|---|---|
| `/new-plugin <kind> <name>` | Plugin module + auto-configuration + tests, wired into the right services |
| `/new-service <name>` | Service from the ingestion-service template, with compose, nginx, Prometheus, Helm wiring |
| `/new-ui-module <name>` | Frontend module, registered and switched on |
| `/new-adr <title>` | Next decision record in `docs/decisions/` |
| `/verify` | Full local verification with a summary |
| `/design-review` | Architecture and security reviewer subagents on the current diff |

## 5. Working agreement

- Never commit to `main`; work on `claude/<short-name>` or `feature/<short-name>` and open a PR. CI must be green
  (Java, frontend, deploy files, end-to-end, docs, secret scan).
- Definition of done: tests for new behaviour (plugins: unit tests; services: Testcontainers integration test),
  `./gradlew build` and frontend checks pass, docs updated, Mermaid parses.
- Ask the user before: dependency major upgrades, changes to `.github/workflows`, `.claude/`, `CLAUDE.md`, Helm
  values for production, anything that deletes data.
- Never: weaken idempotency or retries, log customer data, commit secrets, disable TLS verification, use
  `--no-verify`, force-push.

## 6. Gotchas

- **Spring Boot 4 / Jackson 3:** Jackson classes are in `tools.jackson.*` (annotations stay in
  `com.fasterxml.jackson.annotation`). Starters were renamed: `spring-boot-starter-webmvc`, `-kafka`, `-flyway`.
  Many online examples are for Boot 3.
- **Testcontainers 2:** `org.testcontainers.postgresql.PostgreSQLContainer`, artifacts `testcontainers-postgresql`.
- **Env var names drop dashes:** `reconark.manager.bucket-count` → `RECONARK_MANAGER_BUCKETCOUNT`.
- **Kafka topics** are declared by the manager *and* the workers; consumers must not auto-create topics (a topic
  auto-created by a consumer has one partition, so only one worker would get jobs).
- **Storage:** ingestion-service and workers must share the same folder (`STORAGE_ROOT`); default
  `<temp>/reconark/files` works when both run on one machine.
- **Start order on an empty database:** manager before report-service (the manager creates the engine tables).

## 7. Hooks (`.claude/settings.json`)

| Hook | What |
|---|---|
| `session-start.sh` | Cloud sessions: JDK 25 and frontend deps if missing; prints branch and reminders |
| `guard-files.py` | Blocks edits to accepted decision records, lockfiles, the Gradle wrapper, secret files |
| `guard-bash.py` | Blocks force-push, push to `main`, `reset --hard`, `--no-verify`, TLS bypass, `curl \| sh` |
| `post-edit.py` | Prettier on frontend files; secret scan on every edited file |
| `stop-verify.sh` | Fast checks for what changed (Gradle tests of changed modules, frontend, Mermaid); `RECONARK_SKIP_STOP_VERIFY=1` skips |

Cloud environment network access must allow Maven Central, the Gradle plugin portal/services and npm (or be
"Trusted").
