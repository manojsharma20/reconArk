plugins { id("reconark.spring-service") }

description = "Report definitions, requests, schedules and signed downloads."

dependencies {
    implementation(project(":platform:api-support"))
    // Bricks this distribution carries. Which ones are ACTIVE is decided by reconark.composition (ADR-0026).
    runtimeOnly(project(":plugins:report-csv"))
    runtimeOnly(project(":plugins:bus-inmemory"))
    runtimeOnly(project(":plugins:bus-kafka"))
}
