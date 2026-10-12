# 0007: Prometheus and Grafana for monitoring

| Status | Accepted |
|---|---|
| Date | 2026-10-12 |

## Context

Operations need to see runs, jobs, worker throughput and health, and be alerted. Prometheus and Grafana (or similar) are a stated requirement.

## Decision

Every service exposes Micrometer metrics at `/actuator/prometheus`, including business metrics (`reconark_*`). Prometheus scrapes them and evaluates alert rules (`deploy/prometheus/alerts.yml`); Grafana shows the provisioned **reconArk** dashboard. Both are part of docker compose.

## Consequences

- Free, standard, works on every platform; the dashboard is code in the repository.
- Alert delivery (e-mail, Teams) needs Alertmanager, to be added per environment.

## Alternatives considered

- OpenTelemetry + a vendor (Datadog, Dynatrace, New Relic): richer tracing, paid.
- Cloud-native monitoring (CloudWatch, Azure Monitor): tied to one cloud.
