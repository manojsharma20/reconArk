# Decisions

One short page per important decision: the situation, what we chose, what it costs us, and what else we considered.
A decision is never edited after it is accepted; a new one replaces it and says so.

| # | Decision |
|---|---|
| [0001](0001-simple-two-folder-layout.md) | Simple two-folder layout replaces the v0.2 design |
| [0002](0002-plugins-as-spring-auto-configurations.md) | Plugins are Spring Boot auto-configurations behind small interfaces |
| [0003](0003-manager-and-workers-over-kafka.md) | Recon engine: one manager and many workers, connected by Kafka |
| [0004](0004-postgresql-schema-per-service.md) | One PostgreSQL database, one schema per service |
| [0005](0005-buckets-for-parallel-matching.md) | Split matching into buckets by match key |
| [0006](0006-database-first-then-kafka.md) | Write to the database first, send to Kafka after commit; jobs are idempotent |
| [0007](0007-prometheus-and-grafana.md) | Prometheus and Grafana for monitoring |
| [0008](0008-technology-stack.md) | Java 25, Spring Boot 4, React 19, Gradle |

New decision: copy any file, use the next number, add it to this table (`/new-adr` in Claude Code does this).
