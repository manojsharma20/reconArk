# ADR-0021: Terraform, Helm and GitOps

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../02-architecture/reconArk-architecture.md` §13.1 |
| Build prompt | `../01-prompt/reconArk-build-prompt.md` |

## Context
Repeatable, auditable deployments.

## Decision
Terraform + Helm + Argo CD (GitOps); policy-as-code; drift detection.

## Consequences
Platform skills.

## Alternatives considered and rejected
Console changes.

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
