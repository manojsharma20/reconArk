# ADR-0013: Java 25 LTS, Spring Boot 4 and virtual threads

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../architecture/reconArk-architecture.md` §4 |
| Build prompt | `../reference/reconArk-build-prompt.md` |

## Context
Driven by R13 — Java 25 LTS, latest stable stack, Gradle written from scratch, virtual threads where they help.

## Decision
Java 25 LTS, Spring Boot 4; virtual threads for IO, platform pools for CPU; no preview features.

## Consequences
Pinning checks in performance tests.

## Alternatives considered and rejected
Reactive stack (complexity)

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
