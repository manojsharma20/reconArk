# ADR-0016: jOOQ and COPY for data access

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../02-architecture/reconArk-architecture.md` §4 |
| Build prompt | `../01-prompt/reconArk-build-prompt.md` |

## Context
Set-based SQL, predictable statements.

## Decision
jOOQ for typed SQL; PgJDBC `CopyManager` for bulk load; no JPA on hot paths.

## Consequences
SQL skills needed.

## Alternatives considered and rejected
JPA/Hibernate.

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
