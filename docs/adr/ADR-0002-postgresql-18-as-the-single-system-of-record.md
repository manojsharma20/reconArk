# ADR-0002: PostgreSQL 18 as the single system of record

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../architecture/reconArk-architecture.md` §6 |
| Build prompt | `../reference/reconArk-build-prompt.md` |

## Context
ACID, constraints, partitioning, replicas; team skills.

## Decision
PostgreSQL 18 is the single system of record, including recon results and reporting reads.

## Consequences
Database IO is the budget (ADR-010, 024)

## Alternatives considered and rejected
Separate warehouse for recon (sync complexity)

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
