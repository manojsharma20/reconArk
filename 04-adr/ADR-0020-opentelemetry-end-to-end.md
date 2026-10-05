# ADR-0020: OpenTelemetry end to end

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../02-architecture/reconArk-architecture.md` §11 |
| Build prompt | `../01-prompt/reconArk-build-prompt.md` |

## Context
Driven by R12 — Real-time monitoring and excellent logging, portability.

## Decision
OpenTelemetry end to end, exported to each cloud's backend.

## Consequences
Collector operations.

## Alternatives considered and rejected
Vendor agents in code.

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
