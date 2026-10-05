# ADR-0028: Pipelines declared in configuration as stage graphs

| | |
|---|---|
| Status | Proposed — v0.2 composable architecture, pending architecture review |
| Date | 2026-10-05 |
| Deciders | Architecture review board (to confirm) |
| Details | [HLD](../hld/reconArk-HLD.md) · [LLD](../lld/reconArk-LLD.md) · [Solution document](../solution/reconArk-solution-document.md) |
| Relates to | ADR-0001 (extended, not superseded) |

## Context
C4: providers need different pre-processing (decryption chains, dedupe, tokenisation) without releases. v0.1 fixed the ETL step order in code.

## Decision
Each provider configuration version declares an ordered list of `pipeline-stage` keys with options and an error policy (`REJECT_RECORD`, `QUARANTINE_SOURCE`, `FAIL_RUN`). The `PipelineEngine` compiles the definition once (resolving keys, validating options) and enforces `read = accepted + rejected + quarantined`. Core stages are themselves a plugin (`core-stages`). Recon uses the same idea (strategy, comparators, classifier as extensions).

## Consequences
New steps ship as bricks and are placed by configuration with maker-checker; accounting and claim invariants stay in the engine where plugins cannot bypass them. Linear graphs now; branching DAGs are a later, additive change.

## Alternatives considered and rejected
Workflow engines (Temporal, Camunda) for the record path — too heavy for per-chunk execution at 42k records/s; Apache Camel routes — strong connector library but couples the domain to Camel.

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
