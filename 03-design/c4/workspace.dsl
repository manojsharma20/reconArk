workspace "reconArk" "Distributed ETL and reconciliation platform — C4 model" {

    !identifiers hierarchical

    model {
        analyst = person "Recon analyst / supervisor" "Investigates mismatches, resolves exceptions, runs reports."
        onboarder = person "Onboarding maker / checker" "Configures providers, field mappings and recon rules; approves changes."
        auditor = person "Auditor" "Reads audit trails and reports."

        acquirer = softwareSystem "Card acquirers" "Deliver settlement files over SFTP or object storage, often PGP-encrypted." "External"
        merchant = softwareSystem "Merchants and exchange partners" "Expose files or APIs for pull." "External"
        apiPartner = softwareSystem "API partners" "Push transaction batches over REST, SOAP or gRPC." "External"
        payments = softwareSystem "Internal payment systems" "Emit payment events and expose APIs." "Internal"
        idp = softwareSystem "Bank identity provider" "OIDC with MFA and step-up." "Internal"
        downstream = softwareSystem "Downstream consumers" "Settlement, finance and BI consume recon outcomes." "Internal"

        reconArk = softwareSystem "reconArk" "Ingests transactions from any channel into a canonical model and reconciles them by configured rules." {
            onboardingApi = container "onboarding-api" "Provider configuration lifecycle, connector tests, mapping preview." "Java 25, Spring Boot 4" "Service"
            ingestionGateway = container "ingestion-gateway" "Partner push endpoints; mTLS, request signatures, contract validation." "Java 25, Spring Boot 4" "Service"
            queryApi = container "query-api" "Recon results, field differences, exceptions, run status." "Java 25, Spring Boot 4" "Service"
            reportService = container "report-service" "Asynchronous and scheduled reports from replicas." "Java 25, Spring Boot 4" "Service"
            scheduler = container "scheduler-planner" "Pull scheduling, run planning, fair scheduling, admission control, heartbeat reclaimer, partition maintenance." "Java 25, Spring Boot 4" "Service"
            ingestionWorker = container "ingestion-worker" "Decrypt, sniff, parse, map, validate, COPY and merge chunks." "Java 25, Spring Boot 4" "Worker"
            reconWorker = container "recon-worker" "Batch buckets and streaming matches; rule evaluation; outcome writes." "Java 25, Spring Boot 4" "Worker"
            outboxRelay = container "outbox-relay" "Publishes committed outbox events in order." "Java 25, Spring Boot 4" "Worker"

            primary = container "PostgreSQL primary" "System of record; writers only." "PostgreSQL 18" "Database"
            replicas = container "PostgreSQL read replicas" "Every read that is not part of a write; LSN-fenced read-your-writes." "PostgreSQL 18" "Database"
            objectStore = container "Object storage" "Raw artifacts and generated reports, encrypted with customer-managed keys." "S3 / Blob Storage / Cloud Storage" "Storage"
            bus = container "Message bus" "Exactly one of Kafka, RabbitMQ or ActiveMQ, behind the MessageBus port." "Kafka 4.x (default)" "Queue"
            cache = container "Valkey / Redis" "Rate limits, replay nonces, short-lived caches. Never a system of record." "Valkey" "Database"
            secrets = container "Secret manager + KMS" "Secret values and keys; properties hold reference names only." "Secrets Manager / Key Vault / Secret Manager" "Storage"
        }

        acquirer -> reconArk.scheduler "Files pulled from" "SFTP / object storage"
        merchant -> reconArk.scheduler "Files or APIs pulled from" "SFTP / HTTPS"
        apiPartner -> reconArk.ingestionGateway "Pushes batches to" "HTTPS / gRPC, mTLS + signature"
        payments -> reconArk.bus "Publishes payment events to" "Events"
        onboarder -> reconArk.onboardingApi "Configures and approves providers with" "HTTPS, OIDC"
        analyst -> reconArk.queryApi "Investigates outcomes with" "HTTPS, OIDC"
        analyst -> reconArk.reportService "Requests reports from" "HTTPS, OIDC"
        auditor -> reconArk.queryApi "Reads audit and results with" "HTTPS, OIDC"
        reconArk.onboardingApi -> idp "Authenticates users with" "OIDC"
        reconArk.queryApi -> idp "Authenticates users with" "OIDC"
        reconArk.reportService -> idp "Authenticates users with" "OIDC"

        reconArk.onboardingApi -> reconArk.primary "Writes configuration versions" "JDBC"
        reconArk.onboardingApi -> reconArk.replicas "Reads configuration" "JDBC"
        reconArk.ingestionGateway -> reconArk.objectStore "Stores raw payloads"
        reconArk.ingestionGateway -> reconArk.primary "Creates runs" "JDBC"
        reconArk.ingestionGateway -> reconArk.cache "Checks nonces and rate limits"
        reconArk.scheduler -> reconArk.primary "Claims, plans, finalizes" "JDBC"
        reconArk.scheduler -> reconArk.replicas "Reads run state and dependencies" "JDBC"
        reconArk.scheduler -> reconArk.objectStore "Stores pulled artifacts"
        reconArk.outboxRelay -> reconArk.primary "Polls committed events" "JDBC"
        reconArk.outboxRelay -> reconArk.bus "Publishes events"
        reconArk.bus -> reconArk.ingestionWorker "Delivers chunk work"
        reconArk.bus -> reconArk.reconWorker "Delivers buckets and canonical-record events"
        reconArk.bus -> reconArk.reportService "Delivers report requests"
        reconArk.ingestionWorker -> reconArk.objectStore "Streams byte ranges"
        reconArk.ingestionWorker -> reconArk.primary "COPY and merge" "JDBC"
        reconArk.ingestionWorker -> reconArk.replicas "Reference lookups" "JDBC"
        reconArk.ingestionWorker -> reconArk.secrets "Reads PGP keys"
        reconArk.reconWorker -> reconArk.replicas "Candidate reads" "JDBC"
        reconArk.reconWorker -> reconArk.primary "Writes outcomes" "JDBC"
        reconArk.queryApi -> reconArk.replicas "Reads results" "JDBC"
        reconArk.reportService -> reconArk.replicas "Reads report data" "JDBC"
        reconArk.reportService -> reconArk.objectStore "Writes encrypted report files"
        reconArk.primary -> reconArk.replicas "Streams WAL to" "Physical replication"
        reconArk.bus -> downstream "Delivers outcome events to"

        aws = deploymentEnvironment "AWS (primary target)" {
            deploymentNode "AWS Region" "Three availability zones" "AWS" {
                deploymentNode "EKS" "Karpenter, KEDA" "Kubernetes" {
                    containerInstance reconArk.onboardingApi
                    containerInstance reconArk.ingestionGateway
                    containerInstance reconArk.queryApi
                    containerInstance reconArk.reportService
                    containerInstance reconArk.scheduler
                    containerInstance reconArk.ingestionWorker
                    containerInstance reconArk.reconWorker
                    containerInstance reconArk.outboxRelay
                }
                deploymentNode "Aurora PostgreSQL" "I/O-Optimized cluster" "Amazon Aurora" {
                    containerInstance reconArk.primary
                    containerInstance reconArk.replicas
                }
                deploymentNode "Amazon MSK" "Three brokers, three AZs" "Kafka" {
                    containerInstance reconArk.bus
                }
                deploymentNode "ElastiCache" "" "Valkey" {
                    containerInstance reconArk.cache
                }
                deploymentNode "Amazon S3" "SSE-KMS, replication" "S3" {
                    containerInstance reconArk.objectStore
                }
                deploymentNode "Secrets Manager and KMS" "" "AWS" {
                    containerInstance reconArk.secrets
                }
            }
        }
    }

    views {
        systemContext reconArk "SystemContext" {
            include *
            autolayout lr
        }

        container reconArk "Containers" {
            include *
            autolayout tb
        }

        deployment reconArk aws "AwsDeployment" {
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
        }
    }
}
