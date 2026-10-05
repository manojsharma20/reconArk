# ADR-0032: React shell with manifest-driven feature modules

| | |
|---|---|
| Status | Proposed — v0.2 composable architecture, pending architecture review |
| Date | 2026-10-05 |
| Deciders | Architecture review board (to confirm) |
| Details | [HLD](../hld/reconArk-HLD.md) · [LLD](../lld/reconArk-LLD.md) · [Solution document](../solution/reconArk-solution-document.md) |
| Relates to | ADR-0001 (extended, not superseded) |

## Context
C6: UI features must be addable, removable and role-filtered per environment, and plugin configuration UIs must not need hand-written forms.

## Decision
React 19 + TypeScript + Vite. A shell owns layout, navigation, auth state and errors; feature modules implement `ReconArkUiModule` from `@reconark/module-sdk` and are lazy-loaded. The BFF's UI manifest (filtered by composition, flags and roles) decides which modules mount. Plugin config forms render from each plugin's JSON Schema. Modules are build-time packages now; Module Federation remotes later, same contract.

## Consequences
UI bricks mirror backend bricks; no rebuild to hide or show a module; independent deployment is a later switch, not a rewrite.

## Alternatives considered and rejected
Angular (strong for enterprise, steeper learning curve, smaller pool locally); Next.js (SSR not needed, adds a server tier); single-spa (heavier than needed today).

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
