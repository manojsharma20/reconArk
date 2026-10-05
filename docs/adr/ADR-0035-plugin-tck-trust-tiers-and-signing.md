# ADR-0035: Plugin TCK, trust tiers and signing

| | |
|---|---|
| Status | Proposed — v0.2 composable architecture, pending architecture review |
| Date | 2026-10-05 |
| Deciders | Architecture review board (to confirm) |
| Details | [HLD](../hld/reconArk-HLD.md) · [LLD](../lld/reconArk-LLD.md) · [Solution document](../solution/reconArk-solution-document.md) |
| Relates to | ADR-0001 (extended, not superseded) |

## Context
C7, C8: bricks are only interchangeable if they behave the same, and only safe if their origin is known.

## Decision
Every extension point has a TCK in `testing/plugin-tck`; every plugin test extends `PluginContractTck` plus the TCK of each point it provides, and CI blocks releases that fail. Trust tiers: CORE, VERIFIED, DEV_ONLY (refused in prod), REMOTE (out-of-process only). Plugin artifacts and images are signed (Sigstore cosign) and allow-listed per environment.

## Consequences
Quality is enforced mechanically; third-party bricks have a defined path to production.

## Alternatives considered and rejected
Code review only (does not scale with brick count).

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
