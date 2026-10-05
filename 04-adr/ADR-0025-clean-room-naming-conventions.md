# ADR-0025: Clean-room naming conventions

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../02-architecture/reconArk-architecture.md` §5.3 |
| Build prompt | `../01-prompt/reconArk-build-prompt.md` |

## Context
Avoids coupling to the legacy model; consistent vocabulary.

## Decision
Clean-room naming: own package root (placeholder `io.reconark`), module, property, topic, metric and code conventions; no legacy identifiers.

## Consequences
Legacy values only translated in outbound adapters; a deny-list check in CI.

## Alternatives considered and rejected
Reusing legacy names "for familiarity".

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
