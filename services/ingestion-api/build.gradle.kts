plugins { id("reconark.spring-service") }

description = "Partner push: authenticated batches stored as raw artifacts, run requested via the outbox."

dependencies {
    implementation(project(":platform:api-support"))
    implementation(project(":domain:engine"))
    // Bricks this distribution carries. Which ones are ACTIVE is decided by reconark.composition (ADR-0026).
    runtimeOnly(project(":plugins:format-delimited"))
    runtimeOnly(project(":plugins:bus-inmemory"))
    runtimeOnly(project(":plugins:storage-filesystem"))
    runtimeOnly(project(":plugins:secrets-env"))
    runtimeOnly(project(":plugins:bus-kafka"))
}
