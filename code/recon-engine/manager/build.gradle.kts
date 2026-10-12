// Recon engine MANAGER: starts runs, splits them into jobs, sends jobs to workers over Kafka, tracks progress,
// retries failed or lost jobs and completes the run. Owns the engine database tables (Flyway).
plugins {
    id("org.springframework.boot")
}

description = "reconArk recon-engine manager"

dependencies {
    implementation(project(":recon-engine:engine-core"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-kafka")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
}

// The runnable jar is always build/libs/app.jar, so Dockerfiles don't depend on the version.
tasks.bootJar { archiveFileName = "app.jar" }
tasks.jar { enabled = false }
