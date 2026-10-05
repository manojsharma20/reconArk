# ADR-0011: Isolation levels per write path

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../architecture/reconArk-architecture.md` §6.4 |
| Build prompt | `../reference/reconArk-build-prompt.md` |

## Context
Driven by R5 — ACID, deliberate isolation levels and transaction management.

## Decision
Isolation per path (6.4); short transactions; ordered locking; no long replica queries.

## Consequences
Retry handling for `40001`/`40P01`.

## Alternatives considered and rejected
One global isolation level.

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
