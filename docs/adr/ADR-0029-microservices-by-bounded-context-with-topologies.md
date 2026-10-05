# ADR-0029: Microservices by bounded context, with switchable deployment topologies

| | |
|---|---|
| Status | Proposed — v0.2 composable architecture, pending architecture review |
| Date | 2026-10-05 |
| Deciders | Architecture review board (to confirm) |
| Details | [HLD](../hld/reconArk-HLD.md) · [LLD](../lld/reconArk-LLD.md) · [Solution document](../solution/reconArk-solution-document.md) |
| Relates to | ADR-0001 (extended, not superseded) |

## Context
C5: APIs and features must be separately deployable and optional. v0.1 grouped services by technical role.

## Decision
Services follow bounded contexts: web-bff, admin-api, ingestion-api, recon-api, reports-api, operations-api, scheduler, etl-worker, recon-worker, report-worker, outbox-relay (+ optional remote-plugin-host). One Helm chart, one release per service, each enable-able per environment. Services compile against SPI only and carry plugins as `runtimeOnly`. Topologies: Full, Standard, Lite (roadmap: modular monolith using the same modules).

## Consequences
Independent scaling and release per context; more deployables to operate (mitigated by one chart, GitOps, shared starter).

## Alternatives considered and rejected
Modular monolith only (simpler ops but no independent scaling of workers); nano-services per extension point (operational explosion).

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
