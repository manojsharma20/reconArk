# ADR-0022: AWS primary, Azure secondary, GCP supported

| | |
|---|---|
| Status | Accepted — baseline, pending architecture review |
| Date | 2026-10-02 |
| Deciders | Architecture review board (to confirm) |
| Details | `../architecture/reconArk-architecture.md` §13.6 |
| Build prompt | `../reference/reconArk-build-prompt.md` |

## Context
The cloud comparison (architecture §13.6): the current estate already runs on AWS (object storage, queues,
notifications, secret manager); Aurora suits replica-first reads; MSK, Transfer Family and Amazon MQ are
managed options. Azure is strong on enterprise identity and has in-country regions. GCP lacks managed SFTP,
RabbitMQ and ActiveMQ. Pending confirmation of the bank's cloud strategy and residency rules.

## Decision
AWS primary, Azure secondary, GCP supported.

## Consequences
Adapters for all three tested in CI.

## Alternatives considered and rejected
Single-cloud lock-in.

## Revisit when
The assumptions above change, or measurements from the performance, isolation or security suites contradict
them. A change is a new ADR that supersedes this one; accepted records are never edited in place.
