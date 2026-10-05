/*
 * reconArk — C4 model, v0.2 (composable "Lego" architecture)
 *
 * Levels in this workspace:
 *   L1  System context            -> view "L1-SystemContext"
 *   L2  Containers                -> view "L2-Containers"
 *   L3  Components                -> views "L3-ServiceAnatomy", "L3-EtlWorker", "L3-ReconWorker", "L3-AdminApi", "L3-WebApp"
 *   L4  Code                      -> docs/lld/reconArk-LLD.md §2 (class diagrams for the plugin kernel)
 *   Dynamic                       -> views "D1-PluginBoot", "D2-FileIngestion", "D3-BatchRecon"
 *   Deployment                    -> view "Deploy-AWS"
 *
 * Validate:  docker run --rm -v "$PWD/docs/c4":/usr/local/structurizr structurizr/cli validate -workspace /usr/local/structurizr/workspace.dsl
 * View:      docker run -it --rm -p 8080:8080 -v "$PWD/docs/c4":/usr/local/structurizr structurizr/lite
 */
workspace "reconArk" "Composable ETL and reconciliation platform — C4 model v0.2" {

    !identifiers hierarchical

    model {
        analyst = person "Recon analyst / supervisor" "Investigates mismatches, resolves exceptions, runs reports."
        onboarder = person "Onboarding maker / checker" "Configures providers, mappings, rule sets; approves changes."
        operator = person "Platform operator" "Composes plugins per environment, watches runs, replays work, manages DLQs."
        auditor = person "Auditor" "Reads audit trails and reports."
        pluginDev = person "Plugin developer" "Builds new extensions (connectors, formats, comparators, renderers) against the reconArk SPI and TCK."

        acquirer = softwareSystem "Card acquirers" "Deliver settlement files over SFTP or object storage, often PGP-encrypted." "External"
        merchant = softwareSystem "Merchants and exchange partners" "Expose files or APIs for pull." "External"
        apiPartner = softwareSystem "API partners" "Push transaction batches over REST, SOAP or gRPC." "External"
        payments = softwareSystem "Internal payment systems" "Emit payment events and expose APIs." "Internal"
        idp = softwareSystem "Bank identity provider" "OIDC with MFA and step-up." "Internal"
        downstream = softwareSystem "Downstream consumers" "Settlement, finance and BI consume recon outcomes." "Internal"
        siem = softwareSystem "SIEM" "Receives the audit stream and security events." "Internal"

        reconArk = softwareSystem "reconArk" "Ingests transactions from any channel into a canonical model and reconciles them by configured rules. Every capability is a plugin selected by configuration." {

            webApp = container "web-app" "React 19 SPA shell; loads feature modules (admin, recon, reports, operations) listed in the UI manifest." "React, TypeScript, Vite" "Browser"
            gateway = container "api-gateway" "TLS termination, WAF, rate limits, partner mTLS, routing to services." "Envoy Gateway / cloud API gateway" "Gateway"
            bff = container "web-bff" "Backend-for-frontend: OIDC token handler (cookies, no tokens in the browser), config-driven route table, UI manifest." "Java 25, Spring Boot 4" "Service"

            adminApi = container "admin-api" "Providers, mappings, rule sets, maker-checker, plugin catalog and environment composition." "Java 25, Spring Boot 4" "Service" {
                providerCtl = component "Provider configuration API" "Draft, validate, dry-run, submit, approve, activate, roll back." "Spring MVC"
                makerChecker = component "Maker-checker workflow" "Enforces maker != checker, content-hash bound approvals, step-up MFA." "Application service"
                configValidator = component "Configuration validator" "Validates a provider document against the JSON Schemas contributed by the active plugins." "Application service"
                dryRun = component "Dry-run service" "Runs the provider pipeline against a sample through the same pipeline engine as production." "Application service"
                catalogCtl = component "Plugin catalog API" "Lists installed plugins, versions, config schemas, health." "Spring MVC"
                compositionMgr = component "Composition manager" "Versioned per-environment plugin bindings and feature flags; publishes config-changed events." "Application service"
                kernel = component "Plugin kernel" "Discovers, validates and activates plugins; exposes the extension registry." "reconark-kernel"
            }
            ingestionApi = container "ingestion-api" "Partner push endpoints (REST, gRPC, SOAP); signature + replay checks; stores raw payloads; requests runs." "Java 25, Spring Boot 4" "Service"
            reconApi = container "recon-api" "Recon results, field diffs, exception workflow, re-recon, preview." "Java 25, Spring Boot 4" "Service"
            reportsApi = container "reports-api" "Report definitions, requests, schedules, signed downloads." "Java 25, Spring Boot 4" "Service"
            opsApi = container "operations-api" "Internal only: runs, pause/resume/replay, DLQ requeue, plugin inventory and health." "Java 25, Spring Boot 4" "Service"

            scheduler = container "scheduler" "Pull schedules, run planning, fair scheduling, admission control, heartbeat reclaimer, partition maintenance (leader-elected)." "Java 25, Spring Boot 4" "Worker"
            etlWorker = container "etl-worker" "Executes ETL chunks through the configured stage pipeline." "Java 25, Spring Boot 4" "Worker" {
                chunkConsumer = component "Chunk consumer" "Receives chunk-ready triggers from the MessageBus extension." "Inbound adapter"
                claimMgr = component "Claim manager" "Claims, heartbeats and completes work (owner-scoped)." "Application service"
                pipelineEngine = component "Pipeline engine" "Compiles the provider's stage graph and executes it per chunk." "reconark-pipeline"
                stages = component "Stage extensions" "decoder -> sniffer -> format reader -> field extractor -> validators -> enrichers -> canonicalizer." "Plugins"
                stagingWriter = component "Staging writer" "Binary COPY into UNLOGGED run staging." "Persistence adapter"
                merger = component "Merge service" "Set-based merge into canonical tables, rejects, counts, outbox (one transaction)." "Persistence adapter"
                kernel = component "Plugin kernel" "Extension registry for this worker's composition." "reconark-kernel"
            }
            reconWorker = container "recon-worker" "Executes batch recon buckets and streaming matches with configured rules." "Java 25, Spring Boot 4" "Worker" {
                bucketConsumer = component "Bucket consumer" "Receives bucket-ready and record-canonicalized triggers." "Inbound adapter"
                claimMgr = component "Claim manager" "Owner-scoped claims with heartbeat." "Application service"
                lsnFence = component "LSN fence" "Waits until the replica has replayed the required commit LSN." "Persistence adapter"
                candidateReader = component "Candidate reader" "REPEATABLE READ READ ONLY bucket join on match keys (replica)." "Persistence adapter"
                reconEngine = component "Recon engine" "Match strategy -> comparators -> classifier; identical for batch and streaming." "reconark-engine"
                comparators = component "Comparator extensions" "exact, case-insensitive, numeric tolerance, date window, value map, regex, expression." "Plugins"
                outcomeWriter = component "Outcome writer" "Group versions, members, diffs, counts, outbox (one transaction)." "Persistence adapter"
                kernel = component "Plugin kernel" "Extension registry for this worker's composition." "reconark-kernel"
            }
            reportWorker = container "report-worker" "Generates reports from replicas through renderer and delivery extensions." "Java 25, Spring Boot 4" "Worker"
            outboxRelay = container "outbox-relay" "Publishes committed outbox events in order." "Java 25, Spring Boot 4" "Worker"
            pluginHost = container "remote-plugin-host" "Optional. Runs out-of-process extensions (third-party, polyglot, untrusted) behind the gRPC ExtensionService contract." "Any language, gRPC" "Worker"

            flags = container "Feature flags" "Runtime toggles per environment/tenant via the OpenFeature API." "flagd (OpenFeature)" "Infra"
            schemaRegistry = container "Schema registry" "Event and plugin contract schemas (Avro/JSON Schema/Protobuf)." "Apicurio Registry" "Infra"
            primary = container "PostgreSQL primary" "System of record; writers only. Schema per bounded context." "PostgreSQL 18" "Database"
            replicas = container "PostgreSQL read replicas" "Every read that is not part of a write; LSN-fenced read-your-writes." "PostgreSQL 18" "Database"
            objectStore = container "Object storage" "Raw artifacts and generated reports; customer-managed keys." "S3 / Blob / GCS (ObjectStore extension)" "Storage"
            bus = container "Message bus" "Exactly one active broker, chosen by the MessageBus binding." "Kafka 4 (default) / RabbitMQ / ActiveMQ" "Queue"
            cache = container "Valkey" "Rate limits, replay nonces, short caches. Never a system of record." "Valkey 8" "Database"
            secrets = container "Secret manager + KMS" "Secret values and keys; config holds reference names only." "SecretProvider extension" "Storage"
            otel = container "OpenTelemetry collector" "Receives traces, metrics, logs; exports to the cloud backend." "OTel Collector" "Infra"
        }

        # --- L1 relationships
        acquirer -> reconArk.scheduler "Files pulled from" "SFTP / object storage"
        merchant -> reconArk.scheduler "Files or APIs pulled from" "SFTP / HTTPS"
        apiPartner -> reconArk.gateway "Pushes batches to" "HTTPS / gRPC, mTLS + signature"
        payments -> reconArk.bus "Publishes payment events to" "Events"
        analyst -> reconArk.webApp "Investigates outcomes, runs reports with" "HTTPS"
        onboarder -> reconArk.webApp "Configures and approves providers with" "HTTPS"
        operator -> reconArk.webApp "Composes plugins, operates runs with" "HTTPS"
        auditor -> reconArk.webApp "Reads audit trails with" "HTTPS"
        pluginDev -> reconArk.pluginHost "Ships out-of-process extensions to" "Container image"
        pluginDev -> reconArk.adminApi "Registers plugin versions in the catalog of" "CI pipeline"
        reconArk.bff -> idp "Authenticates users with" "OIDC code + PKCE"
        reconArk.bus -> downstream "Delivers outcome events to"
        reconArk.adminApi -> siem "Streams audit events to"

        # --- L2 relationships
        reconArk.webApp -> reconArk.gateway "Calls /bff and /api through" "HTTPS, same-site cookie"
        reconArk.gateway -> reconArk.bff "Routes browser traffic to" "HTTPS"
        reconArk.gateway -> reconArk.ingestionApi "Routes partner pushes to" "HTTPS / gRPC"
        reconArk.bff -> reconArk.adminApi "Forwards admin calls with access token" "HTTPS, JWT"
        reconArk.bff -> reconArk.reconApi "Forwards recon calls with access token" "HTTPS, JWT"
        reconArk.bff -> reconArk.reportsApi "Forwards report calls with access token" "HTTPS, JWT"
        reconArk.bff -> reconArk.opsApi "Forwards operator calls (internal listener)" "HTTPS, JWT"
        reconArk.bff -> reconArk.flags "Evaluates UI feature flags"

        reconArk.adminApi -> reconArk.primary "Writes configuration versions" "JDBC"
        reconArk.adminApi -> reconArk.replicas "Reads configuration" "JDBC"
        reconArk.adminApi -> reconArk.schemaRegistry "Reads plugin config schemas"
        reconArk.ingestionApi -> reconArk.objectStore "Stores raw payloads"
        reconArk.ingestionApi -> reconArk.primary "Creates runs + outbox" "JDBC"
        reconArk.ingestionApi -> reconArk.cache "Checks nonces and rate limits"
        reconArk.reconApi -> reconArk.replicas "Reads results" "JDBC"
        reconArk.reconApi -> reconArk.primary "Exception actions, re-recon requests" "JDBC"
        reconArk.reportsApi -> reconArk.primary "Report requests + outbox" "JDBC"
        reconArk.reportsApi -> reconArk.objectStore "Signs download URLs"
        reconArk.opsApi -> reconArk.primary "Pause / resume / replay commands" "JDBC"
        reconArk.opsApi -> reconArk.replicas "Run progress, claims, DLQ" "JDBC"

        reconArk.scheduler -> reconArk.primary "Claims, plans, finalizes" "JDBC"
        reconArk.scheduler -> reconArk.replicas "Reads run state and dependencies" "JDBC"
        reconArk.scheduler -> reconArk.objectStore "Stores pulled artifacts"
        reconArk.outboxRelay -> reconArk.primary "Polls committed events" "JDBC"
        reconArk.outboxRelay -> reconArk.bus "Publishes events"
        reconArk.bus -> reconArk.etlWorker "Delivers chunk work"
        reconArk.bus -> reconArk.reconWorker "Delivers buckets and canonical-record events"
        reconArk.bus -> reconArk.reportWorker "Delivers report requests"
        reconArk.etlWorker -> reconArk.objectStore "Streams byte ranges"
        reconArk.etlWorker -> reconArk.primary "COPY and merge" "JDBC"
        reconArk.etlWorker -> reconArk.replicas "Reference lookups" "JDBC"
        reconArk.etlWorker -> reconArk.secrets "Reads PGP keys"
        reconArk.etlWorker -> reconArk.pluginHost "Invokes remote stage extensions (optional)" "gRPC, mTLS"
        reconArk.reconWorker -> reconArk.replicas "Candidate reads" "JDBC"
        reconArk.reconWorker -> reconArk.primary "Writes outcomes" "JDBC"
        reconArk.reconWorker -> reconArk.pluginHost "Invokes remote comparators (optional)" "gRPC, mTLS"
        reconArk.reportWorker -> reconArk.replicas "Reads report data" "JDBC"
        reconArk.reportWorker -> reconArk.objectStore "Writes encrypted report files"
        reconArk.primary -> reconArk.replicas "Streams WAL to" "Physical replication"
        reconArk.etlWorker -> reconArk.otel "Telemetry" "OTLP"
        reconArk.reconWorker -> reconArk.otel "Telemetry" "OTLP"

        # --- L3 relationships (admin-api)
        reconArk.bff -> reconArk.adminApi.providerCtl "Provider configuration calls" "HTTPS, JWT"
        reconArk.bff -> reconArk.adminApi.catalogCtl "Plugin catalog calls" "HTTPS, JWT"
        reconArk.adminApi.providerCtl -> reconArk.adminApi.makerChecker "Submits / approves via"
        reconArk.adminApi.providerCtl -> reconArk.adminApi.configValidator "Validates with"
        reconArk.adminApi.providerCtl -> reconArk.adminApi.dryRun "Dry-runs with"
        reconArk.adminApi.configValidator -> reconArk.adminApi.kernel "Gets config schemas from"
        reconArk.adminApi.dryRun -> reconArk.adminApi.kernel "Builds pipeline from"
        reconArk.adminApi.catalogCtl -> reconArk.adminApi.kernel "Reads inventory from"
        reconArk.adminApi.catalogCtl -> reconArk.adminApi.compositionMgr "Changes bindings via"
        reconArk.adminApi.compositionMgr -> reconArk.primary "Stores composition versions" "JDBC"
        reconArk.adminApi.makerChecker -> reconArk.primary "Writes versions, approvals, audit" "JDBC"

        # --- L3 relationships (etl-worker)
        reconArk.bus -> reconArk.etlWorker.chunkConsumer "chunk-ready trigger"
        reconArk.etlWorker.chunkConsumer -> reconArk.etlWorker.claimMgr "Claims chunk"
        reconArk.etlWorker.claimMgr -> reconArk.primary "UPDATE ... RETURNING claim_token" "JDBC"
        reconArk.etlWorker.chunkConsumer -> reconArk.etlWorker.pipelineEngine "Runs chunk through"
        reconArk.etlWorker.pipelineEngine -> reconArk.etlWorker.kernel "Resolves stage extensions from"
        reconArk.etlWorker.pipelineEngine -> reconArk.etlWorker.stages "Executes"
        reconArk.etlWorker.stages -> reconArk.objectStore "Reads byte range"
        reconArk.etlWorker.pipelineEngine -> reconArk.etlWorker.stagingWriter "Emits accepted records to"
        reconArk.etlWorker.stagingWriter -> reconArk.primary "COPY (UNLOGGED staging)" "JDBC"
        reconArk.etlWorker.pipelineEngine -> reconArk.etlWorker.merger "Completes chunk via"
        reconArk.etlWorker.merger -> reconArk.primary "Merge + rejects + counts + outbox" "JDBC"

        # --- L3 relationships (recon-worker)
        reconArk.bus -> reconArk.reconWorker.bucketConsumer "bucket-ready trigger"
        reconArk.reconWorker.bucketConsumer -> reconArk.reconWorker.claimMgr "Claims bucket"
        reconArk.reconWorker.claimMgr -> reconArk.primary "Claim + heartbeat" "JDBC"
        reconArk.reconWorker.bucketConsumer -> reconArk.reconWorker.lsnFence "Waits on"
        reconArk.reconWorker.lsnFence -> reconArk.replicas "pg_last_wal_replay_lsn()" "JDBC"
        reconArk.reconWorker.bucketConsumer -> reconArk.reconWorker.candidateReader "Reads candidates"
        reconArk.reconWorker.candidateReader -> reconArk.replicas "Bucket join" "JDBC"
        reconArk.reconWorker.bucketConsumer -> reconArk.reconWorker.reconEngine "Evaluates"
        reconArk.reconWorker.reconEngine -> reconArk.reconWorker.kernel "Resolves comparators and match strategies"
        reconArk.reconWorker.reconEngine -> reconArk.reconWorker.comparators "Calls"
        reconArk.reconWorker.reconEngine -> reconArk.reconWorker.outcomeWriter "Writes outcomes via"
        reconArk.reconWorker.outcomeWriter -> reconArk.primary "Outcomes + outbox" "JDBC"

        prod = deploymentEnvironment "AWS (primary target)" {
            deploymentNode "AWS Region" "Three availability zones" "AWS" {
                deploymentNode "CloudFront + S3" "Static SPA hosting" "AWS" {
                    containerInstance reconArk.webApp
                }
                deploymentNode "WAF + ALB + Envoy Gateway" "" "AWS / Kubernetes" {
                    containerInstance reconArk.gateway
                }
                deploymentNode "EKS" "Karpenter, KEDA, Argo CD; one Helm chart, one release per service" "Kubernetes" {
                    containerInstance reconArk.bff
                    containerInstance reconArk.adminApi
                    containerInstance reconArk.ingestionApi
                    containerInstance reconArk.reconApi
                    containerInstance reconArk.reportsApi
                    containerInstance reconArk.opsApi
                    containerInstance reconArk.scheduler
                    containerInstance reconArk.etlWorker
                    containerInstance reconArk.reconWorker
                    containerInstance reconArk.reportWorker
                    containerInstance reconArk.outboxRelay
                    containerInstance reconArk.pluginHost
                    containerInstance reconArk.flags
                    containerInstance reconArk.schemaRegistry
                    containerInstance reconArk.otel
                }
                deploymentNode "Aurora PostgreSQL" "I/O-Optimized" "Amazon Aurora" {
                    containerInstance reconArk.primary
                    containerInstance reconArk.replicas
                }
                deploymentNode "Amazon MSK" "3 brokers, 3 AZs" "Kafka" {
                    containerInstance reconArk.bus
                }
                deploymentNode "ElastiCache" "" "Valkey" {
                    containerInstance reconArk.cache
                }
                deploymentNode "Amazon S3" "SSE-KMS" "S3" {
                    containerInstance reconArk.objectStore
                }
                deploymentNode "Secrets Manager + KMS" "" "AWS" {
                    containerInstance reconArk.secrets
                }
            }
        }
    }

    views {
        systemContext reconArk "L1-SystemContext" {
            include *
            autolayout lr
        }

        container reconArk "L2-Containers" {
            include *
            autolayout tb
        }

        component reconArk.adminApi "L3-AdminApi" {
            include *
            autolayout lr
        }

        component reconArk.etlWorker "L3-EtlWorker" {
            include *
            autolayout lr
        }

        component reconArk.reconWorker "L3-ReconWorker" {
            include *
            autolayout lr
        }

        dynamic reconArk.etlWorker "D2-FileIngestion" "One ETL chunk end to end." {
            reconArk.bus -> reconArk.etlWorker.chunkConsumer "chunk(run, range, config version)"
            reconArk.etlWorker.chunkConsumer -> reconArk.etlWorker.claimMgr "claim"
            reconArk.etlWorker.claimMgr -> reconArk.primary "UPDATE work_chunk ... RETURNING"
            reconArk.etlWorker.chunkConsumer -> reconArk.etlWorker.pipelineEngine "run"
            reconArk.etlWorker.pipelineEngine -> reconArk.etlWorker.stages "decode, parse, map, validate, enrich"
            reconArk.etlWorker.pipelineEngine -> reconArk.etlWorker.stagingWriter "accepted records"
            reconArk.etlWorker.stagingWriter -> reconArk.primary "COPY"
            reconArk.etlWorker.pipelineEngine -> reconArk.etlWorker.merger "complete"
            reconArk.etlWorker.merger -> reconArk.primary "merge + rejects + counts + outbox"
            autolayout lr
        }

        dynamic reconArk.reconWorker "D3-BatchRecon" "One recon bucket end to end." {
            reconArk.bus -> reconArk.reconWorker.bucketConsumer "bucket(run, rule set, hash range, LSN)"
            reconArk.reconWorker.bucketConsumer -> reconArk.reconWorker.claimMgr "claim"
            reconArk.reconWorker.bucketConsumer -> reconArk.reconWorker.lsnFence "wait for LSN"
            reconArk.reconWorker.bucketConsumer -> reconArk.reconWorker.candidateReader "read candidates"
            reconArk.reconWorker.bucketConsumer -> reconArk.reconWorker.reconEngine "evaluate"
            reconArk.reconWorker.reconEngine -> reconArk.reconWorker.comparators "compare fields"
            reconArk.reconWorker.reconEngine -> reconArk.reconWorker.outcomeWriter "outcomes"
            reconArk.reconWorker.outcomeWriter -> reconArk.primary "write + outbox"
            autolayout lr
        }

        deployment reconArk prod "Deploy-AWS" {
            include *
            autolayout lr
        }

        styles {
            element "Person" {
                shape person
            }
            element "External" {
                background #8a8a8a
                color #ffffff
            }
            element "Database" {
                shape cylinder
            }
            element "Storage" {
                shape folder
            }
            element "Queue" {
                shape pipe
            }
            element "Worker" {
                shape hexagon
            }
            element "Browser" {
                shape webbrowser
            }
            element "Gateway" {
                shape roundedbox
            }
            element "Plugins" {
                shape component
                background #2f6f4f
                color #ffffff
            }
        }
    }
}
