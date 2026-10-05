# Architecture decision records

MADR-style. Accepted records are immutable; a change is a new record that supersedes the old one.
ADR-0001 to ADR-0025 are the v0.1 baseline. ADR-0026 to ADR-0037 add the v0.2 composable ("Lego") architecture; they
extend the baseline and supersede none of it.

| ADR | Title | Status |
|---|---|---|
| [0001](ADR-0001-hexagonal-cloud-portable-core.md) | Hexagonal, cloud-portable core | Accepted (baseline) |
| [0002](ADR-0002-postgresql-18-as-the-single-system-of-record.md) | PostgreSQL 18 as the single system of record | Accepted (baseline) |
| [0003](ADR-0003-writers-on-the-primary-all-other-reads-on-replicas.md) | Writers on the primary, all other reads on replicas | Accepted (baseline) |
| [0004](ADR-0004-configuration-driven-table-partitioning.md) | Configuration-driven table partitioning | Accepted (baseline) |
| [0005](ADR-0005-batch-recon-via-database-bucket-joins-and-worker-rule-evaluation.md) | Batch recon via database bucket joins and worker rule evaluation | Accepted (baseline) |
| [0006](ADR-0006-bulk-load-through-unlogged-staging-and-set-based-merge.md) | Bulk load through unlogged staging and set-based merge | Accepted (baseline) |
| [0007](ADR-0007-messagebus-port-one-active-broker-transactional-outbox.md) | MessageBus port, one active broker, transactional outbox | Accepted (baseline) |
| [0008](ADR-0008-kafka-as-the-default-broker.md) | Kafka as the default broker | Accepted (baseline) |
| [0009](ADR-0009-database-claims-as-the-source-of-truth-for-work.md) | Database claims as the source of truth for work | Accepted (baseline) |
| [0010](ADR-0010-database-admission-control-per-run-class.md) | Database admission control per run class | Accepted (baseline) |
| [0011](ADR-0011-isolation-levels-per-write-path.md) | Isolation levels per write path | Accepted (baseline) |
| [0012](ADR-0012-cel-for-all-expressions.md) | CEL for all expressions | Accepted (baseline) |
| [0013](ADR-0013-java-25-lts-spring-boot-4-and-virtual-threads.md) | Java 25 LTS, Spring Boot 4 and virtual threads | Accepted (baseline) |
| [0014](ADR-0014-gradle-kotlin-dsl-build-written-from-scratch.md) | Gradle Kotlin DSL build written from scratch | Accepted (baseline) |
| [0015](ADR-0015-purpose-built-orchestration-instead-of-spring-batch.md) | Purpose-built orchestration instead of Spring Batch | Accepted (baseline) |
| [0016](ADR-0016-jooq-and-copy-for-data-access.md) | jOOQ and COPY for data access | Accepted (baseline) |
| [0017](ADR-0017-identity-authorization-and-partner-authentication.md) | Identity, authorization and partner authentication | Accepted (baseline) |
| [0018](ADR-0018-secrets-only-in-the-cloud-secret-manager.md) | Secrets only in the cloud secret manager | Accepted (baseline) |
| [0019](ADR-0019-asynchronous-reports-from-replicas.md) | Asynchronous reports from replicas | Accepted (baseline) |
| [0020](ADR-0020-opentelemetry-end-to-end.md) | OpenTelemetry end to end | Accepted (baseline) |
| [0021](ADR-0021-terraform-helm-and-gitops.md) | Terraform, Helm and GitOps | Accepted (baseline) |
| [0022](ADR-0022-aws-primary-azure-secondary-gcp-supported.md) | AWS primary, Azure secondary, GCP supported | Accepted (baseline) |
| [0023](ADR-0023-valkey-redis-never-a-system-of-record.md) | Valkey/Redis never a system of record | Accepted (baseline) |
| [0024](ADR-0024-shard-writes-by-provider-group-when-needed.md) | Shard writes by provider group when needed | Accepted (baseline) |
| [0025](ADR-0025-clean-room-naming-conventions.md) | Clean-room naming conventions | Accepted (baseline) |
| [0026](ADR-0026-hybrid-plugin-kernel.md) | Hybrid plugin kernel: in-process SPI bricks plus out-of-process gRPC bricks | Proposed (v0.2) |
| [0027](ADR-0027-extension-point-cardinality-and-bindings.md) | Extension-point cardinality and explicit bindings | Proposed (v0.2) |
| [0028](ADR-0028-configuration-declared-pipelines.md) | Pipelines declared in configuration as stage graphs | Proposed (v0.2) |
| [0029](ADR-0029-microservices-by-bounded-context-with-topologies.md) | Microservices by bounded context, with switchable deployment topologies | Proposed (v0.2) |
| [0030](ADR-0030-schema-per-context-on-shared-postgresql.md) | Schema per bounded context on the shared PostgreSQL cluster | Proposed (v0.2) |
| [0031](ADR-0031-bff-token-handler-for-the-spa.md) | Backend-for-frontend token handler for the React SPA | Proposed (v0.2) |
| [0032](ADR-0032-react-shell-with-manifest-driven-modules.md) | React shell with manifest-driven feature modules | Proposed (v0.2) |
| [0033](ADR-0033-openfeature-for-runtime-flags.md) | OpenFeature for runtime feature flags | Proposed (v0.2) |
| [0034](ADR-0034-contracts-first-openapi-asyncapi-proto.md) | Contract-first: OpenAPI 3.1, AsyncAPI 3, Protobuf, schema registry | Proposed (v0.2) |
| [0035](ADR-0035-plugin-tck-trust-tiers-and-signing.md) | Plugin TCK, trust tiers and signing | Proposed (v0.2) |
| [0036](ADR-0036-platform-composition-in-git-business-config-in-database.md) | Platform composition in Git; business configuration in the database | Proposed (v0.2) |
| [0037](ADR-0037-claude-code-cloud-sessions-development-workflow.md) | AI-assisted development in Claude Code cloud sessions | Proposed (v0.2) |

