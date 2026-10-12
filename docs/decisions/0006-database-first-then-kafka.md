# 0006: Write to the database first, send to Kafka after commit; jobs are idempotent

| Status | Accepted |
|---|---|
| Date | 2026-10-12 |

## Context

A job must never be lost or applied twice, even if a process crashes between saving state and sending a message.

## Decision

The manager changes the database in a transaction and sends Kafka messages only after the commit. A job stays QUEUED until a worker's result arrives; a watchdog re-sends QUEUED jobs after `job-timeout`. Workers make jobs idempotent (LOAD deletes its earlier rows; MATCH replaces its bucket's results). The manager ignores results for jobs that are no longer QUEUED or for an older attempt.

## Consequences

- Same safety as a transactional outbox, with less code.
- A crash can delay a job by up to `job-timeout` (default 10 minutes).

## Alternatives considered

- Transactional outbox table + relay: no delay, more moving parts.
- Kafka transactions: complex, and they don't cover the database.
