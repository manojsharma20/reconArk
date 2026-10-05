# Architecture decision records

MADR-style. Accepted records are immutable; a change is a new record that supersedes the old one.

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
