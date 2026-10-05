# ADR-0019: Asynchronous reports from replicas

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../architecture/reconArk-architecture.md` §6.3, §10.2 |
| Build prompt | `../reference/reconArk-build-prompt.md` |

## Context
Driven by R4 — The database is the shared, finite IO resource, R9 — Excellent security for the onboarding and reports APIs; nothing a VAPT would flag.

## Decision
Reports generated asynchronously from replicas to encrypted object storage; signed single-use URLs.

## Consequences
Report service and queue.

## Alternatives considered and rejected
Synchronous large exports.

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
