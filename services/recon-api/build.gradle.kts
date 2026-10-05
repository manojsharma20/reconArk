plugins { id("reconark.spring-service") }

description = "Recon results, field diffs, exception workflow, re-recon, rule-set preview."

dependencies {
    implementation(project(":platform:api-support"))
    implementation(project(":domain:engine"))
    // Bricks this distribution carries. Which ones are ACTIVE is decided by reconark.composition (ADR-0026).
    runtimeOnly(project(":plugins:recon-standard"))
}
