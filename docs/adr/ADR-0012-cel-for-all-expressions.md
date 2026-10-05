# ADR-0012: CEL for all expressions

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../architecture/reconArk-architecture.md` §10.3 |
| Build prompt | `../reference/reconArk-build-prompt.md` |

## Context
Sandboxed by design; prevents expression injection.

## Decision
CEL for all expressions.

## Consequences
Migration of legacy expression rules.

## Alternatives considered and rejected
JEXL (needs a hardened sandbox)

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
