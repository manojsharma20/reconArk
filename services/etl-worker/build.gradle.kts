plugins { id("reconark.spring-service") }

description = "Executes ETL chunks through the provider's configured stage pipeline."

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation(project(":domain:engine"))
    // Bricks this distribution carries. Which ones are ACTIVE is decided by reconark.composition (ADR-0026).
    runtimeOnly(project(":plugins:format-delimited"))
    runtimeOnly(project(":plugins:bus-inmemory"))
    runtimeOnly(project(":plugins:storage-filesystem"))
    runtimeOnly(project(":plugins:secrets-env"))
    runtimeOnly(project(":plugins:bus-kafka"))
}
