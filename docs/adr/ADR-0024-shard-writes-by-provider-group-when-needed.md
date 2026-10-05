# ADR-0024: Shard writes by provider group when needed

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../architecture/reconArk-architecture.md` §8, §15 |
| Build prompt | `../reference/reconArk-build-prompt.md` |

## Context
Keeps run isolation; linear write scale.

## Decision
Scale writes by sharding per provider group (separate PostgreSQL clusters) when one primary can't meet the budget.

## Consequences
Routing by provider configuration; cross-shard reports go through replicas.

## Alternatives considered and rejected
Single giant primary; immediate move to distributed SQL.

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
