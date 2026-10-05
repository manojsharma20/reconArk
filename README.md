# reconArk — design pack

reconArk is a distributed ETL and reconciliation platform. It ingests transactions from any channel into
one canonical model, then reconciles them by configured rules, at a few hundred to 100M+ records per run,
with many runs at once, each finishing within 40 minutes.

## What's here

| Path | What it is | Read it when |
|---|---|---|
| `01-prompt/reconArk-build-prompt.md` | The build prompt: requirements, constraints, data model, transactions, security, cloud, deliverables | You hand the build to an engineer or an AI agent |
| `02-architecture/reconArk-architecture.md` | The solution architecture and the AWS, Azure and GCP decisions | You review or approve the design |
| `03-design/diagrams/*.mmd` | Diagram sources in Mermaid (listed below) | You need a view the documents don't embed, or want to edit one |
| `03-design/c4/workspace.dsl` | The C4 model (context, containers, AWS deployment) in Structurizr DSL | You want a navigable, model-based C4 view |
| `04-adr/` | 25 architecture decision records (MADR style) plus an index | You challenge or change a decision |

**Suggested reading order:**
1. Architecture §1–3: purpose, requirements, principles.
2. Architecture §5–9: structure, data, transactions, processing, performance, messaging.
3. Architecture §10–13: security, operations, resilience, clouds.
4. ADR index.
5. The prompt — the full specification.

## Diagrams

| File | View |
|---|---|
| `01-system-context.mmd` | C4 level 1 — system context |
| `02-containers.mmd` | C4 level 2 — containers |
| `03-etl-file-ingestion.sequence.mmd` | File ingestion, end to end |
| `04-recon-batch.sequence.mmd` | Batch recon for a rule set and business date |
| `05-recon-streaming.sequence.mmd` | Real-time recon |
| `06-partner-push-ingestion.sequence.mmd` | Partner push with mTLS, signature, replay protection |
| `07-crash-recovery.sequence.mmd` | Self-healing after a worker dies mid-chunk |
| `08-run-lifecycle.state.mmd` | Run lifecycle (saga) |
| `09-work-claim-lifecycle.state.mmd` | Chunk / bucket / group claim |
| `10-config-version-lifecycle.state.mmd` | Provider configuration — maker-checker |
| `11-exception-lifecycle.state.mmd` | Recon exception workflow |
| `12-data-model-configuration.erd.mmd` | Configuration data model |
| `13-data-model-runtime.erd.mmd` | Runtime data model (partitioned) |
| `14-read-write-routing.mmd` | Primary for writers, replicas for reads, LSN fencing |
| `15-admission-control.mmd` | Database admission control and fair scheduling |
| `16-deployment-aws.architecture.mmd` | AWS deployment |
| `17-deployment-azure.architecture.mmd` | Azure deployment |
| `18-deployment-gcp.architecture.mmd` | GCP deployment |

Formats used:
- flowcharts and `sequenceDiagram`;
- `stateDiagram-v2` for lifecycles;
- `erDiagram` for the data model;
- Mermaid's newest diagram type, `architecture-beta`, for cloud deployments. The architecture document
  embeds flowchart versions of the cloud deployments, which render in more tools.

## How to view

- **Mermaid** (`.mmd`, and the blocks inside the documents):
  - renders natively in GitHub, GitLab, Azure DevOps, Confluence (with the Mermaid app), IntelliJ IDEA
    and VS Code (Markdown preview with a Mermaid extension);
  - or paste into <https://mermaid.live>;
  - the `architecture-beta` deployment diagrams need Mermaid 11 or later.
- **Structurizr DSL:**
  - `docker run -it --rm -p 8080:8080 -v "$PWD/03-design/c4":/usr/local/structurizr structurizr/lite`,
    then open <http://localhost:8080>;
  - or paste the file into the Structurizr online DSL editor.

## Validation status

- All 18 Mermaid diagram files, and all 7 Mermaid blocks embedded in the architecture document, parse
  without errors with Mermaid 12.1 (`mermaid.parse`).
- `03-design/c4/workspace.dsl` has **not** been machine-validated. The Structurizr CLI container would
  not run on the authoring machine. Validate it once with:

  ```
  docker run --rm -v "$PWD/03-design/c4":/usr/local/structurizr structurizr/cli validate -workspace /usr/local/structurizr/workspace.dsl
  ```

## Conventions

reconArk is a clean-room design. It reuses no names, packages, tables, properties, topics, codes or
statuses from the legacy platform; see prompt §2.4 and §16.4, and ADR-0025. Versions, cloud service
availability and regional support marked "verify" must be confirmed at project start (architecture §17).
