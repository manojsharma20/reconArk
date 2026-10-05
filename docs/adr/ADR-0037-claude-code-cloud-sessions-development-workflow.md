# ADR-0037: AI-assisted development in Claude Code cloud sessions

| | |
|---|---|
| Status | Proposed — v0.2 composable architecture, pending architecture review |
| Date | 2026-10-05 |
| Deciders | Architecture review board (to confirm) |
| Details | [HLD](../hld/reconArk-HLD.md) · [LLD](../lld/reconArk-LLD.md) · [Solution document](../solution/reconArk-solution-document.md) |
| Relates to | ADR-0001 (extended, not superseded) |

## Context
Future development will be carried out with Claude Code cloud sessions against this GitHub repository; the guard rails must live in the repo.

## Decision
`CLAUDE.md` records the architecture rules, commands and conventions. `.claude/settings.json` sets allow/deny permissions and hooks: SessionStart environment bootstrap (cloud only), PreToolUse guards (protected files, accepted ADRs, secrets, destructive git), PostToolUse formatting and secret scanning, and a Stop hook running the fast verification. Reusable commands live in `.claude/commands/`; review subagents in `.claude/agents/`. Branch protection and CI remain the final gate.

## Consequences
Consistent, reviewable AI contributions; the same rules apply to humans through CI. Cloud environments need Trusted network access (Maven Central, Gradle, npm).

## Alternatives considered and rejected
Relying on prompts alone (rules forgotten between sessions); GitHub Actions-based agents (separate billing path, not requested).

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
