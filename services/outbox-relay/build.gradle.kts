plugins { id("reconark.spring-service") }

description = "Publishes committed outbox events, in order, through the active message bus."

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    // Bricks this distribution carries. Which ones are ACTIVE is decided by reconark.composition (ADR-0026).
    runtimeOnly(project(":plugins:bus-inmemory"))
    runtimeOnly(project(":plugins:bus-kafka"))
}
