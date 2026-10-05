# CLAUDE.md — working on reconArk

reconArk is a **composable ("Lego") ETL and reconciliation platform**. Read this file fully before changing
anything. The design lives in `docs/`. Start with `docs/solution/reconArk-solution-document.md`, then
`docs/hld/reconArk-HLD.md` and `docs/lld/reconArk-LLD.md`.

## 1. The Lego rules (non-negotiable — CI and hooks enforce most of them)

1. **The kernel knows sockets, never bricks.**
   - `kernel/**` depends only on the JDK.
   - `domain/**` depends only on kernel + SPI.
   - Neither ever imports Spring, a plugin, or a cloud SDK.
2. **New behaviour is a brick, not an `if`.**
   - A new format, connector, comparator, stage, renderer, broker or cloud service is a new module under
     `plugins/` implementing an SPI in `domain/spi`.
   - Never add provider-specific or cloud-specific branches to engines or services.
3. **Plugins never depend on each other or on Spring.** They talk only through extension points.
4. **Every brick passes its TCK.** A plugin's tests extend `PluginContractTck` plus the TCK of each extension point
   it provides (`testing/plugin-tck`).
5. **Services compile against SPI only.** Plugins are `runtimeOnly`; which ones are active is decided by
   `reconark.composition.*`.
6. **Configuration has three layers:**
   - platform composition (Git);
   - business configuration (database, maker-checker);
   - runtime flags (OpenFeature).

   Business configuration may only reference active keys.
7. **Fail fast at boot.** Invalid compositions must raise a `KernelException` with an `RK-KRN-*` code. Don't
   weaken these checks.
8. **v0.1 invariants stay.** Keep all of these:
   - writes on the primary, reads on replicas (LSN fencing);
   - DB claims as the source of truth for work;
   - the transactional outbox and idempotent consumers;
   - `read = accepted + rejected + quarantined`;
   - no network calls inside a transaction;
   - the 40-minute run budget.
9. **Security:**
   - configuration holds secret *reference names* only;
   - sensitive values are masked (last 4) in logs, errors, diffs, reports and APIs;
   - `DEV_ONLY` bricks never run in `prod`;
   - no tokens in the browser (the BFF token handler).
10. **Clean-room naming (ADR-0025):**
    - packages `io.reconark.<context>.<layer>`;
    - properties `reconark.*`;
    - topics `reconark.<context>.<event>.v<N>`;
    - codes `RK-<AREA>-<NNNN>`;
    - nothing from the legacy platform (deny-list in `scripts/naming-denylist.txt`).

## 2. Repository map

| Path | What |
|---|---|
| `kernel/plugin-api` | `ReconArkPlugin`, `PluginDescriptor`, `ExtensionPoint`, `ConfigSpec`, `ExtensionLookup`, `KernelError` |
| `kernel/plugin-runtime` | `PluginCatalog`, `CompositionConfig`, `PluginRuntime.boot`, `ExtensionRegistry`, `Kernel`, `ExtensionInterceptor` |
| `kernel/pipeline` | `PipelineStage`, `PipelineEngine`, `RecordEnvelope`, `StagePayload`, `ErrorPolicy` |
| `domain/model`, `domain/spi`, `domain/engine` | Canonical model; `ExtensionPoints` catalogue + contracts; core stages plugin + `ReconEngine` |
| `plugins/*` | Bricks. One Gradle module each. Registered in `META-INF/services/io.reconark.kernel.api.ReconArkPlugin` |
| `platform/spring-boot-starter`, `platform/api-support` | Kernel auto-configuration, `/actuator/plugins`, metrics; JWT security, problem details, dev-mode |
| `services/*` | Spring Boot microservices, one bounded context each; `application.yaml` holds the local composition |
| `testing/plugin-tck`, `testing/architecture-tests` | TCK suites; ArchUnit Lego-boundary rules |
| `frontend/` | npm workspaces: `apps/shell`, `packages/module-sdk`, `modules/{admin,recon,reports,operations}` |
| `contracts/` | OpenAPI 3.1, AsyncAPI 3, `extension_service.proto` — update these *with* the code |
| `config/`, `deploy/` | Composition and provider examples; docker-compose, Helm chart, Terraform interface |
| `docs/` | Solution document, HLD, LLD, C4 (`docs/c4/workspace.dsl`), ADRs, v0.1 baseline (read-only) |

## 3. Commands

```bash
./gradlew verifyQuick                         # fast loop: kernel + domain + plugins tests (what the Stop hook runs)
./gradlew build                               # everything: compile, unit, TCK, ArchUnit, Spring context tests
./gradlew :plugins:<id>:test                  # one brick
RECONARK_DEV_MODE=true ./gradlew :services:admin-api:bootRun   # run an API with in-memory bricks
cd frontend && npm ci                         # once per session (the SessionStart hook does it in the cloud)
cd frontend && npm run typecheck && npm test && npm run build
cd frontend && npm run format                 # Prettier
cd tools/docs-lint && npm ci && npm run lint  # parse every Mermaid diagram in docs/
docker compose -f deploy/local/docker-compose.yml up -d        # PostgreSQL 18, Kafka, Valkey, MinIO, Keycloak, LGTM
```

Toolchain:
- Java 25 (Gradle toolchain);
- Gradle 9.8 (wrapper);
- Node 22+.

Versions live only in `gradle/libs.versions.toml` and `frontend/package.json`.

## 4. How to do common tasks

Use the slash commands in `.claude/commands/`. They follow the recipes in LLD §15:

| Command | Does |
|---|---|
| `/new-plugin <id> <extension-point>` | Scaffold a brick with descriptor, service registration, TCK tests, settings + composition wiring |
| `/new-service <name>` | Scaffold a bounded-context service with composition, Helm values, contract, BFF route |
| `/new-ui-module <id>` | Scaffold a frontend module, register it, add a manifest entry |
| `/new-adr <title>` | Next-numbered ADR from the template |
| `/verify` | Full local verification and a summary |
| `/design-review` | Run the architecture and security reviewer subagents on the current diff |

## 5. Working agreement

- **Branches:**
  - never commit to `main`;
  - work on `feature/<short-name>` or `claude/<short-name>`;
  - open a PR (CI and review are the gate).
- **One concern per PR.** Code, tests, contracts, docs and diagrams change together in the same PR. If you change
  behaviour described in the HLD or LLD, update them.
- **Decisions:**
  - any new extension point, service, technology or change to a rule above needs an ADR (`/new-adr`);
  - accepted ADRs are immutable, so supersede them with a new one. The hooks block edits to accepted ADRs.
- **Definition of done:**
  - `./gradlew build` is green;
  - the frontend is green (typecheck, test, build, format check);
  - Mermaid parses;
  - new bricks pass their TCK;
  - there are no new warnings in `kernel/`, `domain/` and `plugins/` (they compile with `-Werror`).
- **Tests first for bricks.** Extend the TCK; add focused tests for anything the TCK doesn't cover.
- **Error codes.** Add new `RK-*` codes to the tables in the LLD.
- **Never:**
  - weaken a kernel check;
  - add `@SuppressWarnings` without a reason;
  - log unmasked sensitive values;
  - commit secrets;
  - disable TLS verification;
  - use `--no-verify`;
  - force-push.
- **Ask the user before:**
  - dependency upgrades with breaking changes;
  - changes to `.github/workflows`, `.claude/`, `CLAUDE.md`;
  - production compositions or Helm environments;
  - anything that deletes data.

## 6. Hooks and permissions (`.claude/settings.json`)

| Hook | When | What |
|---|---|---|
| `session-start.sh` | Session start/resume | In cloud sessions: ensures JDK 25 and frontend deps; prints branch and reminders |
| `guard-files.py` | Before Edit/Write | Blocks accepted ADRs, the v0.1 reference docs, lockfiles, Gradle wrapper, secret-like files |
| `guard-bash.py` | Before Bash | Blocks force-push, push to `main`, `reset --hard`, `--no-verify`, TLS bypass, `curl \| sh`, destructive `rm` |
| `post-edit.py` | After Edit/Write | Prettier for frontend files; secret scan and legacy-name scan on the edited file |
| `stop-verify.sh` | Before finishing | Runs fast checks for what changed (Gradle `verifyQuick`, frontend typecheck + tests, Mermaid); blocks completion on failure. Set `RECONARK_SKIP_STOP_VERIFY=1` to skip in an emergency. |

Hooks are shell scripts with Python 3 for JSON. They run locally and in the cloud.

## 7. Claude Code cloud environment (one-time setup)

Network access must be **Trusted**. If you use **Custom**, allow:
- `repo.maven.apache.org`
- `repo1.maven.org`
- `plugins.gradle.org`
- `plugins-artifacts.gradle.org`
- `services.gradle.org`
- `registry.npmjs.org`
- `archive.ubuntu.com`
- `security.ubuntu.com`

Recommended **setup script** for the environment. It is cached, so put slow installs here:

```bash
#!/bin/bash
apt-get update -qq && apt-get install -y -qq openjdk-25-jdk-headless || true
```

Everything else (Gradle wrapper download, `npm ci`) is done by the SessionStart hook or on first build.

## 8. Gotchas

- Spring binds YAML lists inside `Map<String,Object>` as index-keyed maps. `CompositionConfig.fromMap` and
  `ConfigValidator` accept both. Keep it that way.
- An empty YAML map (`plugin: {}`) may not bind. Enable plugins with defaults through `enabled: [ ... ]`.
- A SINGLE extension point with more than one active brick needs `bindings.<point>`, or boot fails with
  `RK-KRN-0008`.
- Spring Boot 4 renamed several starters (`spring-boot-starter-webmvc`,
  `spring-boot-starter-security-oauth2-resource-server`/`-client`). Check the Boot 4.x docs before adding a starter.
- Business APIs are OAuth2 resource servers. They need `spring.security.oauth2.resourceserver.jwt.issuer-uri`
  (the bank IdP, or local Keycloak), or `RECONARK_DEV_MODE=true` locally. web-bff needs the `keycloak` profile or
  dev-mode. Without either they fail at startup, which is the intended secure default.
- `domain/engine` registers the `core-stages` plugin through `META-INF/services`. Every service that runs pipelines
  needs it enabled.
