plugins { id("reconark.spring-service") }

description = "Executes batch recon buckets and streaming matches with the configured rule sets."

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation(project(":domain:engine"))
    // Bricks this distribution carries. Which ones are ACTIVE is decided by reconark.composition (ADR-0026).
    runtimeOnly(project(":plugins:recon-standard"))
    runtimeOnly(project(":plugins:bus-inmemory"))
    runtimeOnly(project(":plugins:bus-kafka"))
}
