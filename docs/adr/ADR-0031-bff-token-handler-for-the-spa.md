# ADR-0031: Backend-for-frontend token handler for the React SPA

| | |
|---|---|
| Status | Proposed — v0.2 composable architecture, pending architecture review |
| Date | 2026-10-05 |
| Deciders | Architecture review board (to confirm) |
| Details | [HLD](../hld/reconArk-HLD.md) · [LLD](../lld/reconArk-LLD.md) · [Solution document](../solution/reconArk-solution-document.md) |
| Relates to | ADR-0001 (extended, not superseded) |

## Context
R9/VAPT: tokens in browser storage are exposed to XSS; the SPA calls five APIs.

## Decision
`web-bff` performs OIDC authorization code + PKCE, keeps tokens server-side, issues an HttpOnly SameSite=Strict session cookie plus a CSRF cookie, proxies `/api/{context}/**` via a configured route table with header allow-lists, and serves the UI manifest. Business APIs remain stateless JWT resource servers.

## Consequences
No tokens in JavaScript; one origin for the browser; route table and UI manifest are configuration (Lego). The BFF is on the request path and must scale (stateless with a shared session store when > 1 replica: Spring Session on Valkey).

## Alternatives considered and rejected
SPA with tokens in memory + silent refresh (still exposed to XSS, third-party cookie issues); API gateway token relay only (no CSRF/session story).

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
