# ADR-0007: MessageBus port, one active broker, transactional outbox

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../architecture/reconArk-architecture.md` §9 |
| Build prompt | `../reference/reconArk-build-prompt.md` |

## Context
Driven by R7 — One of Kafka, RabbitMQ or ActiveMQ, exactly one active, R11 — Distributed, fault tolerant, self-healing, resilient, auto-recovering.

## Decision
`MessageBus` port; exactly one broker active; transactional outbox; at-least-once + idempotent consumers.

## Consequences
Outbox relay service.

## Alternatives considered and rejected
XA / 2PC.

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
