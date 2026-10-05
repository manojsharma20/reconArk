---
description: Scaffold a new bounded-context microservice with composition, Helm values, OpenAPI contract and BFF route
argument-hint: <service-name> <port>
---

Create a new reconArk service: $ARGUMENTS. Follow LLD §15 "Add a service".

1. Confirm with the user:
   - the bounded context;
   - which schema it owns (ADR-0030);
   - whether it is browser-facing.

   A new service changes the architecture, so draft an ADR with `/new-adr` if it isn't covered by ADR-0029.
2. Create `services/<name>/build.gradle.kts` with `id("reconark.spring-service")`:
   - add `implementation(project(":platform:api-support"))` for business APIs;
   - add the engine modules it needs;
   - add plugins as `runtimeOnly` only.
3. Package `io.reconark.services.<namenodash>`. Create:
   - `<Name>Application` (`@SpringBootApplication @ConfigurationPropertiesScan`);
   - controllers under `/api/<context>/v1`, with `@PreAuthorize` roles;
   - records for DTOs;
   - no business logic in controllers.
4. Create `src/main/resources/application.yaml` with these properties:
   - `server.port`;
   - virtual threads;
   - problem details;
   - actuator exposure (`health,info,plugins,prometheus`);
   - `reconark.composition` with an `enabled` list.

   Copy the shape from `services/recon-api`.
5. Write `contracts/openapi/<name>.yaml`, reusing `common.yaml` components.
6. If the service is browser-facing:
   - add a route in `services/web-bff/src/main/resources/application.yaml` (`reconark.bff.routes`);
   - add a UI module if needed (`/new-ui-module`).
7. Create `deploy/helm/environments/prod-aws/<name>.yaml`, modelled on `etl-worker.yaml`.
8. Add a `@SpringBootTest` with `reconark.security.dev-mode=true` that proves the composition boots.
9. Update the HLD §5 table, LLD §8 table and `settings.gradle.kts`. Then run `./gradlew :services:<name>:build`.
