# ADR-0006: Bulk load through unlogged staging and set-based merge

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../architecture/reconArk-architecture.md` §7.1, §8 |
| Build prompt | `../reference/reconArk-build-prompt.md` |

## Context
Write and WAL minimisation (R4 — The database is the shared, finite IO resource).

## Decision
Bulk load = binary `COPY` into `UNLOGGED` run staging, then set-based logged merge.

## Consequences
Staging rebuilt after a crash.

## Alternatives considered and rejected
Row inserts; logged staging.

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
