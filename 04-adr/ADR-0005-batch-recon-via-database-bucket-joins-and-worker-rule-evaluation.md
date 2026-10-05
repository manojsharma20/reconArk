# ADR-0005: Batch recon via database bucket joins and worker rule evaluation

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../02-architecture/reconArk-architecture.md` §7.2, §8 |
| Build prompt | `../01-prompt/reconArk-build-prompt.md` |

## Context
Scales to 100M; minimal network transfer.

## Decision
Batch recon = database hash-bucket candidate join (replica) + rule evaluation in workers.

## Consequences
Bucket sizing must avoid spills.

## Alternatives considered and rejected
In-memory JVM matching of whole days.

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
