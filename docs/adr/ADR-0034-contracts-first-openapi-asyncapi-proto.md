# ADR-0034: Contract-first: OpenAPI 3.1, AsyncAPI 3, Protobuf, schema registry

| | |
|---|---|
| Status | Proposed — v0.2 composable architecture, pending architecture review |
| Date | 2026-10-05 |
| Deciders | Architecture review board (to confirm) |
| Details | [HLD](../hld/reconArk-HLD.md) · [LLD](../lld/reconArk-LLD.md) · [Solution document](../solution/reconArk-solution-document.md) |
| Relates to | ADR-0001 (extended, not superseded) |

## Context
Many independently deployed bricks and services need stable, checkable interfaces.

## Decision
`contracts/` holds OpenAPI 3.1 per API, AsyncAPI 3 for events (CloudEvents envelope), and the gRPC ExtensionService proto. Server stubs, DTOs and the frontend client are generated. Apicurio Registry stores event and plugin schemas with compatibility rules; CI lints and diffs contracts for breaking changes.

## Consequences
Breaking changes become visible in PRs; consumers and bricks evolve independently within a major version.

## Alternatives considered and rejected
Code-first annotations (drift between services), Confluent Schema Registry (licence constraints for some features).

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
