# Configuration — the three layers (HLD §8)

| Layer | Where | Decides | Change control |
|---|---|---|---|
| 1. Platform composition | `config/composition/*.yaml` → Helm values → `reconark.composition.*` | Which bricks each service runs, their config, the bindings | Git PR, CI composition lint, GitOps |
| 2. Business configuration | admin-api (database), examples in `config/providers/` | Providers, stage graphs, rule sets, report definitions | Maker-checker, versioned, effective-dated |
| 3. Runtime flags | OpenFeature / flagd | Switching installed behaviour per environment, tenant or rule set | Flag change with audit |

Rules:
- Layer 2 can only name extension keys that layer 1 made active. admin-api rejects anything else with `RK-KRN-0012`.
- Configuration holds secret **reference names** only (`*-ref`), never secret values.
- Plugins marked *planned* below are designed (HLD §7) but not yet implemented as modules.
