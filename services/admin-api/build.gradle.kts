plugins { id("reconark.spring-service") }

description = "Onboarding and configuration: providers, pipelines, rule sets, maker-checker, plugin catalog."

dependencies {
    implementation(project(":platform:api-support"))
    implementation(project(":domain:engine"))
    // Bricks this distribution carries. Which ones are ACTIVE is decided by reconark.composition (ADR-0026).
    runtimeOnly(project(":plugins:format-delimited"))
    runtimeOnly(project(":plugins:recon-standard"))
    runtimeOnly(project(":plugins:report-csv"))
    runtimeOnly(project(":plugins:bus-inmemory"))
    runtimeOnly(project(":plugins:secrets-env"))
    runtimeOnly(project(":plugins:storage-filesystem"))
    runtimeOnly(project(":plugins:bus-kafka"))
}
