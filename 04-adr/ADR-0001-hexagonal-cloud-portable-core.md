# ADR-0001: Hexagonal, cloud-portable core

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../02-architecture/reconArk-architecture.md` §3, §5.3, §13.1 |
| Build prompt | `../01-prompt/reconArk-build-prompt.md` |

## Context
Driven by R14 — Deployable on AWS, Azure or GCP; testability.

## Decision
Hexagonal, cloud-portable core; cloud specifics only in adapters and Terraform.

## Consequences
More interfaces; ArchUnit enforcement.

## Alternatives considered and rejected
Cloud-native SDKs in business code.

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
