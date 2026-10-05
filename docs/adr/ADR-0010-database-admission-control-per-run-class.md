# ADR-0010: Database admission control per run class

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../architecture/reconArk-architecture.md` §7.5 |
| Build prompt | `../reference/reconArk-build-prompt.md` |

## Context
Driven by R1 — Any number of ETL and recon runs at once, fully isolated, R4 — The database is the shared, finite IO resource.

## Decision
Admission controller allocates database capacity per run class.

## Consequences
Central scheduler component.

## Alternatives considered and rejected
Unbounded worker writes.

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
