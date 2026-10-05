# ADR-0036: Platform composition in Git; business configuration in the database

| | |
|---|---|
| Status | Proposed — v0.2 composable architecture, pending architecture review |
| Date | 2026-10-05 |
| Deciders | Architecture review board (to confirm) |
| Details | [HLD](../hld/reconArk-HLD.md) · [LLD](../lld/reconArk-LLD.md) · [Solution document](../solution/reconArk-solution-document.md) |
| Relates to | ADR-0001 (extended, not superseded) |

## Context
Two very different kinds of configuration change: which bricks/services exist (platform) and how providers and rules use them (business).

## Decision
Platform composition (compositions, Helm values, UI manifest) lives in Git and is deployed by GitOps with two-person review. Business configuration lives in the database via admin-api with maker-checker, versioning and effective dates (v0.1 §6). admin-api validates business config against the active composition.

## Consequences
Clear ownership and audit trail for each kind; no business user can install code, no platform change bypasses review.

## Alternatives considered and rejected
Everything in the database (platform changes without code review), everything in Git (business users cannot self-serve).

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
