# 0004: One PostgreSQL database, one schema per service

| Status | Accepted |
|---|---|
| Date | 2026-10-12 |

## Context

Each microservice should own its data, but running one database per service is costly for a small team, and report-service must read results efficiently.

## Decision

One PostgreSQL database with schemas `config`, `ingestion` and `engine`. Each service creates its tables with Flyway in its own schema. Services never write another schema. report-service reads the `engine` schema (in production, from a read replica). Data access is plain SQL with Spring `JdbcClient`.

## Consequences

- One database to operate, back up and monitor.
- Ownership is still clear; a schema can move to its own database later.
- report-service is coupled to the engine tables (documented, read-only).

## Alternatives considered

- Database per service: stronger isolation, more cost and operations.
- JPA/Hibernate: less SQL to write, but hides queries and is slow for bulk inserts.
