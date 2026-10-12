# 0003: Recon engine: one manager and many workers, connected by Kafka

| Status | Accepted |
|---|---|
| Date | 2026-10-12 |

## Context

Runs must handle up to 100 million rows per side within about 40 minutes, survive crashes, and scale by adding machines. A messaging system between the manager and the workers is a stated requirement.

## Decision

A **manager** splits each run into LOAD jobs (one per file) and MATCH jobs (one per bucket), stores them in PostgreSQL and publishes them on topic `reconark.engine.jobs.v1`. **Workers** in consumer group `recon-workers` take jobs, process them and publish results on `reconark.engine.job-results.v1`. The manager tracks job states, retries failures (3 attempts) and re-sends jobs without an answer (watchdog).

## Consequences

- Scale = start more workers (up to the number of partitions).
- Kafka redelivers unacknowledged jobs when a worker dies; jobs are idempotent.
- Kafka is one more system to run (managed offerings exist on every cloud).

## Alternatives considered

- RabbitMQ: simpler queue semantics, a good alternative where Kafka is not already used.
- A PostgreSQL job table with `SELECT … FOR UPDATE SKIP LOCKED`: no broker at all, fine at moderate scale; rejected because a messaging system was required and Kafka scales further.
- Spring Batch remote partitioning: more concepts for the same result.
