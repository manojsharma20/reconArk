---
description: Create a new reconArk microservice from the ingestion-service template, with database schema, tests and deployment wiring
argument-hint: <service-name, e.g. exception-service> <port>
---

Create a service. Arguments: $ARGUMENTS. First confirm with the user why it must be a separate service (it needs a
decision record in `docs/decisions/`; run `/new-adr`).

1. Copy `code/backend/services/ingestion-service` to `code/backend/services/<name>`; rename package
   `io.reconark.<shortname>`, application class, `spring.application.name`, port, schema
   (`spring.flyway.schemas`, `db/migration/<schema>/V1__<what>.sql`). Remove what you don't need.
2. Keep the pattern Controller → Service → Repository (`JdbcClient`, plain SQL). Errors as `ResponseStatusException`.
3. `include(":backend:services:<name>")` in `code/settings.gradle.kts`; keep the `bootJar { archiveFileName = "app.jar" }` block.
4. Integration test with Testcontainers PostgreSQL (copy `UploadTest`).
5. Wiring: `code/deploy/docker-compose.yml` (service block, profile `app`), `code/deploy/docker/nginx.conf` and
   `code/frontend/vite.config.ts` (its `/api/...` path), `code/deploy/prometheus/prometheus.yml` (target),
   `code/deploy/helm/values/<name>.yaml`, and `.github/workflows/ci.yml` only if needed (ask).
6. Docs: container table and diagram in `docs/02-architecture.md` §2.2, APIs and tables in `docs/03-detailed-design.md`.
7. Run `cd code && ./gradlew :backend:services:<name>:build` and `docker compose -f deploy/docker-compose.yml --profile app config --quiet`.
