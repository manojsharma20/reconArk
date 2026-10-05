# ADR-0027: Extension-point cardinality and explicit bindings

| | |
|---|---|
| Status | Proposed — v0.2 composable architecture, pending architecture review |
| Date | 2026-10-05 |
| Deciders | Architecture review board (to confirm) |
| Details | [HLD](../hld/reconArk-HLD.md) · [LLD](../lld/reconArk-LLD.md) · [Solution document](../solution/reconArk-solution-document.md) |
| Relates to | ADR-0001 (extended, not superseded) |

## Context
Some sockets must have exactly one active implementation (the message bus), others many selected by key (format readers), others an ordered list (audit sinks). Ambiguity must be impossible.

## Decision
Every `ExtensionPoint` declares `SINGLE`, `KEYED` or `CHAIN`. A SINGLE point with several active contributions requires `bindings.<point>: <key>`; CHAIN order comes from `chains.<point>`; KEYED implementations are chosen by business configuration. Unknown keys fail at boot (RK-KRN-0009) or at save time in admin-api (RK-KRN-0012).

## Consequences
"Exactly one broker active" (ADR-0007) is enforced by the kernel, not by convention.

## Alternatives considered and rejected
Implicit priority ordering (surprising), Spring `@Primary` (framework-bound).

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
