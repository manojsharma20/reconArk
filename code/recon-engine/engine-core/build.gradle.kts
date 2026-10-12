// The heart of the recon engine, shared by manager and worker:
// - the messages they exchange over Kafka (messages/)
// - the matching algorithm (matching/)
// - the database tables of the engine (src/main/resources/db/migration/engine), applied by the manager with Flyway
// Plain Java: easy to unit test.
description = "reconArk engine core: job messages, matching algorithm, engine database schema"

dependencies {
    api(project(":backend:common"))
}
