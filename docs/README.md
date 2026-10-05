# reconArk documentation

Start with the **[solution document](solution/reconArk-solution-document.md)**. It explains the whole solution, with
diagrams, and links to everything else.

| Document | What it is | Read it when |
|---|---|---|
| [solution/reconArk-solution-document.md](solution/reconArk-solution-document.md) | Our understanding, the Lego model, C4 summary, architecture and flow diagrams, concepts, step-by-step scenarios, tech stack with learning curve and alternatives, enhancements, roadmap | First |
| [c4/workspace.dsl](c4/workspace.dsl) | C4 model of record (Structurizr DSL): L1, L2, L3, dynamic views, AWS deployment | You want navigable C4 views |
| [hld/reconArk-HLD.md](hld/reconArk-HLD.md) | High-level design: drivers, Lego rules, containers, extension-point catalogue, configuration layers, pipelines, services, frontend, key flows | You review the architecture |
| [lld/reconArk-LLD.md](lld/reconArk-LLD.md) | Low-level design: modules and dependency rules, kernel classes and boot algorithm, SPI, engines, remote bricks, services, APIs, events, data deltas, frontend, tests, CI, recipes | You build or change something |
| [adr/](adr/README.md) | 37 architecture decision records (0001–0025 baseline, 0026–0037 composable) | You challenge or change a decision |
| [architecture/reconArk-architecture.md](architecture/reconArk-architecture.md) | v0.1 baseline architecture: data, transactions, performance, messaging, security, clouds | Data, performance, security or cloud questions |
| [reference/reconArk-build-prompt.md](reference/reconArk-build-prompt.md) | v0.1 build prompt: the full requirements specification | You need the original requirement |
| [diagrams/](diagrams/) | v0.1 Mermaid diagrams (sequence, state, ERD, deployment) | You need a specific v0.1 view |
| [README-v0.1.md](README-v0.1.md) | The original design-pack README | History |

Validate every Mermaid diagram with `cd tools/docs-lint && npm ci && npm run lint`.
