# Contracts

Contract-first (build prompt §13): these files are the source of truth for every interface. Server stubs, DTOs and
the frontend API client are generated from them, never hand-written twice.

| File | Interface |
|---|---|
| `openapi/common.yaml` | Shared components: RFC 9457 problem details, cursor pagination, `Idempotency-Key` |
| `openapi/admin-api.yaml` | Onboarding: providers, pipelines, maker-checker, plugin catalogue |
| `openapi/recon-api.yaml` | Recon preview, comparators (results, diffs and exceptions follow the same style) |
| `openapi/reports-api.yaml` | Report formats and requests |
| `openapi/ingestion-api.yaml` | Partner push |
| `openapi/operations-api.yaml` | Internal operations |
| `asyncapi/reconark-events.yaml` | Events on the message bus (CloudEvents envelope) |
| `proto/extension_service.proto` | Out-of-process plugin contract (remote bricks) |

Compatibility rules: additive changes only within a major version (`/v1`); breaking changes add `/v2` and run both
until consumers move. CI lints these files and diffs them for breaking changes on every PR.
