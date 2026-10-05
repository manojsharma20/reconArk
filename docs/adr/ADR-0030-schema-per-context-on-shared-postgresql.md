# ADR-0030: Schema per bounded context on the shared PostgreSQL cluster

| | |
|---|---|
| Status | Proposed — v0.2 composable architecture, pending architecture review |
| Date | 2026-10-05 |
| Deciders | Architecture review board (to confirm) |
| Details | [HLD](../hld/reconArk-HLD.md) · [LLD](../lld/reconArk-LLD.md) · [Solution document](../solution/reconArk-solution-document.md) |
| Relates to | ADR-0001 (extended, not superseded) |

## Context
Microservice purity suggests a database per service, but recon needs set-based joins over canonical data at 100M-record scale (ADR-0002, ADR-0005).

## Decision
One PostgreSQL cluster; schemas `config`, `etl`, `recon`, `reporting`, `platform`. Only the owning service's role writes a schema; other contexts read published `*_v1` views on replicas. A context can later move to its own cluster (ADR-0024) without changing consumers.

## Consequences
Keeps ACID and set-based recon; ownership is enforced by database roles and ArchUnit; cross-context coupling is explicit through versioned views.

## Alternatives considered and rejected
Database per service (forces data copying and eventual consistency into the recon hot path).

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
