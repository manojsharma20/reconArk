# ADR-0018: Secrets only in the cloud secret manager

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../architecture/reconArk-architecture.md` §10.5 |
| Build prompt | `../reference/reconArk-build-prompt.md` |

## Context
Driven by R8 — Secrets only in a secret manager; only keys in properties.

## Decision
Secrets only in the cloud secret manager; workload identity; no static credentials.

## Consequences
Secret caching with rotation.

## Alternatives considered and rejected
Secrets in properties or the database.

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
