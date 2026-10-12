// Recon engine WORKER: takes jobs from Kafka, does the heavy lifting (load files, match buckets), reports back.
// Run as many copies as you need; they share the work automatically (one Kafka consumer group).
plugins {
    id("org.springframework.boot")
}

description = "reconArk recon-engine worker"

dependencies {
    implementation(project(":recon-engine:engine-core"))

    // Lego bricks this worker can use. Add a plugin = add a line here (and to config-service, so the UI offers it).
    implementation(project(":backend:plugins:format-csv"))
    implementation(project(":backend:plugins:format-fixed-width"))
    implementation(project(":backend:plugins:compare-standard"))
    implementation(project(":backend:plugins:storage-local"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc") // only for /actuator endpoints
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-kafka")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // Tests create the engine tables themselves (in production the manager does it).
    testImplementation("org.springframework.boot:spring-boot-starter-flyway")
    testImplementation("org.flywaydb:flyway-database-postgresql")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
}
