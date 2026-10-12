# 0001: Simple two-folder layout replaces the v0.2 design

| Status | Accepted |
|---|---|
| Date | 2026-10-12 |

## Context

The v0.2 design (plugin kernel with descriptors, trust tiers and versioning; 11 microservices; 37 decision records; three documents of 550–700 lines) was hard for new developers, consultants and testers to follow.

## Decision

Restructure the repository into `docs/` (all documentation) and `code/` (all source). Keep the ideas that matter: plugins, separate services, a distributed engine. Drop the machinery that a team of 2–3-year developers cannot maintain. Archive v0.2 on the branch `archive/v0.2`.

## Consequences

- A newcomer finds anything in two clicks; one Gradle build, one npm project.
- Fewer, smaller services (5 instead of 11).
- Some v0.2 capabilities (remote plugins, plugin signing, maker-checker) are not present; they are on the roadmap where still needed.

## Alternatives considered

- Keep v0.2 and add more documentation: does not remove the complexity.
- Separate repositories per service: more overhead for a small team.
