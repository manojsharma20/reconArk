# 4. Developer guide

**Summary.** Install JDK 25, Node 22 and Docker. Start the infrastructure with one command, run the services from
your IDE (or everything in Docker), and open http://localhost:5173. To extend reconArk you usually add a **plugin**
(a small module implementing one interface); sections 4.5–4.7 walk through it step by step.

## 4.1 Prerequisites

| Tool | Version | Check |
|---|---|---|
| JDK | 25 | `java -version` |
| Node.js | 22 or newer | `node -v` |
| Docker + Docker Compose | recent | `docker compose version` |
| IDE | IntelliJ IDEA (recommended) or VS Code with Java extensions | |

Gradle is not needed: use the wrapper `./gradlew` in `code/`.

## 4.2 Run it

### Option A: everything in Docker (quickest)

```bash
cd code
deploy/start.sh                 # builds jars + frontend, starts all containers
deploy/smoke-test.sh            # optional: proves it works end to end
```

Open http://localhost:8080 (UI) and http://localhost:3000 (Grafana).

### Option B: infrastructure in Docker, services in your IDE (best for development)

```bash
cd code
docker compose -f deploy/docker-compose.yml up -d     # PostgreSQL, Kafka, Prometheus, Grafana, Kafka UI
```

Then start the five Spring Boot applications from the IDE (each has a `main` method), or from a terminal:

```bash
./gradlew :backend:services:config-service:bootRun
./gradlew :backend:services:ingestion-service:bootRun
./gradlew :recon-engine:manager:bootRun                 # start before report-service (creates the engine tables)
./gradlew :backend:services:report-service:bootRun
./gradlew :recon-engine:worker:bootRun
```

And the UI with hot reload:

```bash
cd frontend && npm install && npm run dev               # http://localhost:5173
```

The defaults in each `application.yaml` point at `localhost`, so nothing needs configuring. Uploaded files go to
`reconark/files` in your system's temp folder, which ingestion-service and the workers share; set `STORAGE_ROOT` to
use another folder.

To run two workers locally, start the second one with another port: `PORT=8092 ./gradlew :recon-engine:worker:bootRun`.

## 4.3 Find your way in the code

Start with these files; each is short and commented:

| To understand… | Read |
|---|---|
| The business model | `backend/common/.../model/ReconDefinition.java`, `ResultRow.java` |
| What a plugin is | `backend/common/.../plugin/FileFormat.java`, `PluginRegistry.java` |
| A complete plugin | `backend/plugins/format-csv/` (3 files) |
| The whole run lifecycle | `recon-engine/manager/.../run/RunService.java` |
| What a worker does | `recon-engine/worker/.../LoadJob.java`, `MatchJob.java` |
| Matching | `recon-engine/engine-core/.../matching/Matcher.java` |
| A typical REST service | `backend/services/config-service/` (controller → validator → repository) |
| The UI shell | `frontend/src/App.tsx`, `frontend/src/shell/modules.ts` |

Conventions:
- **Controller → Service → Repository**. Controllers only translate HTTP; rules live in services; SQL lives in
  repositories (Spring `JdbcClient`, plain SQL).
- **Records** for data, constructor injection, no field injection, no Lombok.
- Errors to the client: `ResponseStatusException` with a clear message; Spring turns it into Problem Details JSON.
- Tables are created by Flyway scripts in `src/main/resources/db/migration/<schema>/V<n>__<what>.sql`. Never edit a
  released script; add a new one.
- Log what happened and ids, never file contents or customer data.

## 4.4 Build and test

```bash
cd code
./gradlew build                                   # compile + all tests (integration tests need Docker running)
./gradlew :recon-engine:engine-core:test          # one module
./gradlew test --tests '*MatcherTest'             # one test class
cd frontend && npm run typecheck && npm test && npm run build && npm run format:check
cd ../../docs/_lint && npm ci && npm run lint     # check the Mermaid diagrams in docs/
```

## 4.5 Add a plugin (example: a JSON-lines file format)

A plugin is a small Gradle module with three files. Follow `format-csv` as the template.

**1. Create the module** `code/backend/plugins/format-jsonl/build.gradle.kts`:

```kotlin
description = "FileFormat plugin 'jsonl'"
dependencies {
    api(project(":backend:common"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-jackson")
}
```

and add `include(":backend:plugins:format-jsonl")` to `code/settings.gradle.kts`.

**2. Implement the interface** (`src/main/java/io/reconark/plugins/jsonl/JsonLinesFileFormat.java`):

```java
public class JsonLinesFileFormat implements FileFormat {
    private final JsonMapper json = JsonMapper.builder().build();

    public String id() { return "jsonl"; }
    public String description() { return "One JSON object per line."; }

    public void read(InputStream content, Map<String, String> options, Consumer<Map<String, String>> onRow)
            throws IOException {
        var reader = new BufferedReader(new InputStreamReader(content, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) continue;
            Map<String, Object> object = json.readValue(line, new TypeReference<>() {});
            Map<String, String> row = new LinkedHashMap<>();
            object.forEach((k, v) -> row.put(k, v == null ? null : v.toString()));
            onRow.accept(row);
        }
    }
}
```

**3. Register it** with an auto-configuration class and one line in
`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```java
@AutoConfiguration
@ConditionalOnProperty(name = "reconark.plugins.format-jsonl.enabled", havingValue = "true", matchIfMissing = true)
public class JsonLinesFormatPlugin {
    @Bean JsonLinesFileFormat jsonLinesFileFormat() { return new JsonLinesFileFormat(); }
}
```

```
io.reconark.plugins.jsonl.JsonLinesFormatPlugin
```

**4. Test it** (`src/test/...`): feed a small string, check the rows, check an error case. See `CsvFileFormatTest`.

**5. Plug it in**: add `implementation(project(":backend:plugins:format-jsonl"))` to the services that need it: the
**worker** (to read files) and **config-service** (so definitions may use it and the UI offers it).

That's all. No engine, service or UI code changes: the definition editor lists `jsonl` automatically because it comes
from `GET /api/plugins`.

The same steps make a **comparator** (`FieldComparator`, add to worker + config-service), a **report format**
(`ReportWriter`, add to report-service) or a **storage** (`FileStore`, add to ingestion-service + worker, and switch
`storage-local` off).

## 4.6 Add a microservice

1. Copy `backend/services/ingestion-service` as a template (smallest service with a database).
2. Rename the package and application class; pick a free port; give it its own schema
   (`spring.flyway.schemas: <name>`, scripts in `db/migration/<name>`).
3. `include(":backend:services:<name>")` in `settings.gradle.kts`.
4. Add it to `deploy/docker-compose.yml` (copy a service block), to `deploy/prometheus/prometheus.yml`, to
   `deploy/docker/nginx.conf` and `frontend/vite.config.ts` (its `/api/...` path) and a Helm values file.
5. Write an integration test with Testcontainers (copy `UploadTest`).
6. Add a decision record in `docs/decisions` explaining why it is a separate service.

## 4.7 Add a UI module

1. Create `frontend/src/modules/<name>/` with a page component and an `index.ts`:
   ```ts
   export const auditModule: UiModule = {
     id: 'audit', title: 'Audit', home: '/audit',
     routes: [{ path: '/audit', Page: AuditPage }],
   };
   ```
2. Add it to `allModules` in `frontend/src/shell/modules.ts`.
3. Switch it on: add `"audit"` to `modules` in `frontend/public/config.json` (and in each environment's config).

## 4.8 Working with Claude Code

`CLAUDE.md` at the repository root tells Claude Code (and humans) the rules, commands and layout. Slash commands in
`.claude/commands/` scaffold plugins, services, UI modules and decision records; hooks block risky commands and run
the fast checks before a session ends.

## 4.9 Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Service fails at start: `Connection refused … 5432` | PostgreSQL not running | `docker compose -f deploy/docker-compose.yml up -d` |
| report-service: `relation "engine.recon_run" does not exist` | manager hasn't created the engine tables yet | start the manager first |
| Run stays in LOADING | no worker running, or worker can't reach Kafka | start a worker; check Kafka UI → consumer group `recon-workers` |
| LOAD job fails: `NoSuchFileException` | ingestion-service and worker use different storage folders | point both at the same `STORAGE_ROOT` |
| LOAD job fails: `csv: line N has X values…` | file and format options don't agree (delimiter, header) | fix the definition's format options |
| Integration tests fail: `Could not find a valid Docker environment` | Docker not running | start Docker Desktop |
| Kafka: `LEADER_NOT_AVAILABLE` warnings at start | topics being created | harmless, disappears after a few seconds |
