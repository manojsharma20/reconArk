# ADR-0009: Database claims as the source of truth for work

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../architecture/reconArk-architecture.md` §7.6, §9 |
| Build prompt | `../reference/reconArk-build-prompt.md` |

## Context
Broker loss or duplication never loses or doubles work.

## Decision
Database claim tables are the work source of truth; messages are triggers.

## Consequences
Claim writes on the primary (small, single-statement)

## Alternatives considered and rejected
Broker-only work tracking.

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
