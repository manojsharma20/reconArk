# ADR-0015: Purpose-built orchestration instead of Spring Batch

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../02-architecture/reconArk-architecture.md` §7 |
| Build prompt | `../01-prompt/reconArk-build-prompt.md` |

## Context
Distributed chunking across pods; the legacy platform's local-partition state bugs; job-repository write load on the primary.

## Decision
No Spring Batch; a purpose-built planner + claims + bus.

## Consequences
Owning orchestration code (tested)

## Alternatives considered and rejected
Spring Batch remote partitioning.

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
