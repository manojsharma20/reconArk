# ADR-0017: Identity, authorization and partner authentication

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../architecture/reconArk-architecture.md` §10.1–10.2 |
| Build prompt | `../reference/reconArk-build-prompt.md` |

## Context
Driven by R9 — Excellent security for the onboarding and reports APIs; nothing a VAPT would flag.

## Decision
OIDC with the bank's IdP + MFA and step-up; ABAC + row-level security; mTLS + signatures for partners.

## Consequences
IdP integration work.

## Alternatives considered and rejected
API keys alone.

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
