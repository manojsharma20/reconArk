# ADR-0023: Valkey/Redis never a system of record

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../02-architecture/reconArk-architecture.md` §5.2 |
| Build prompt | `../01-prompt/reconArk-build-prompt.md` |

## Context
Data integrity.

## Decision
Valkey/Redis only for rate limits, nonce windows and short caches; never a system of record.

## Consequences
Cache can be lost safely.

## Alternatives considered and rejected
Redis-held work state (as in the legacy platform)

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
