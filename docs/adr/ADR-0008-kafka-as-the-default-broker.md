# ADR-0008: Kafka as the default broker

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../architecture/reconArk-architecture.md` §9 |
| Build prompt | `../reference/reconArk-build-prompt.md` |

## Context
Partitioned parallelism, ordering per key, replay, throughput.

## Decision
Kafka is the default broker.

## Consequences
RabbitMQ/ActiveMQ remain supported via adapters.

## Alternatives considered and rejected
None of significance.

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
