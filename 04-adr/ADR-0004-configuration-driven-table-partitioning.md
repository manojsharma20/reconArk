# ADR-0004: Configuration-driven table partitioning

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../02-architecture/reconArk-architecture.md` §6.2 |
| Build prompt | `../01-prompt/reconArk-build-prompt.md` |

## Context
Driven by R10 — Tables partitioned, with partitioning configured per table before setup, pruning, cheap retention.

## Decision
Configuration-driven partitioning, fixed before install; defaults per 6.2.

## Consequences
DDL generation; drift check at startup.

## Alternatives considered and rejected
Hand-written partition DDL.

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
