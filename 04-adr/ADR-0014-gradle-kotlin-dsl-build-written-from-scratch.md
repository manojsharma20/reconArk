# ADR-0014: Gradle Kotlin DSL build written from scratch

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../02-architecture/reconArk-architecture.md` §4, §5.3 |
| Build prompt | `../01-prompt/reconArk-build-prompt.md` |

## Context
Driven by R13 — Java 25 LTS, latest stable stack, Gradle written from scratch, virtual threads where they help; no duplicated build logic.

## Decision
Gradle 9 Kotlin DSL written from scratch; convention plugins in `build-logic`; dependency verification.

## Consequences
Initial build effort.

## Alternatives considered and rejected
Copying the legacy build.

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
