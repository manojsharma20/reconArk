# ADR-0003: Writers on the primary, all other reads on replicas

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../architecture/reconArk-architecture.md` §6.3 |
| Build prompt | `../reference/reconArk-build-prompt.md` |

## Context
Driven by R4 — The database is the shared, finite IO resource.

## Decision
Writers on the primary; every other read on replicas, with LSN fencing for read-your-writes.

## Consequences
Two data sources and ports; fence waits.

## Alternatives considered and rejected
Reads on the primary "for safety".

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
