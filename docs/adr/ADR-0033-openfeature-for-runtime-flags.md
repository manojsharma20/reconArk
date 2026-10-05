# ADR-0033: OpenFeature for runtime feature flags

| | |
|---|---|
| Status | Proposed — v0.2 composable architecture, pending architecture review |
| Date | 2026-10-05 |
| Deciders | Architecture review board (to confirm) |
| Details | [HLD](../hld/reconArk-HLD.md) · [LLD](../lld/reconArk-LLD.md) · [Solution document](../solution/reconArk-solution-document.md) |
| Relates to | ADR-0001 (extended, not superseded) |

## Context
Installed behaviour sometimes needs switching per environment, tenant or rule set without a deployment (layer 3 of configuration).

## Decision
Use the vendor-neutral OpenFeature API with the flagd provider (self-hosted). Flags gate installed behaviour only; they never install code or replace maker-checker for business configuration.

## Consequences
Provider can change (Unleash, LaunchDarkly, cloud AppConfig) without code changes — the same Lego principle applied to flags.

## Alternatives considered and rejected
Home-grown flag tables (reinvention), vendor SDKs directly in code (lock-in).

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
