// report-service: read-only views of run results, and downloads through ReportWriter plugins.
plugins {
    id("org.springframework.boot")
}

description = "reconArk report-service"

dependencies {
    implementation(project(":backend:common"))
    implementation(project(":backend:plugins:report-csv"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // Tests create the engine tables (owned by the recon-engine manager) to have something to report on.
    testImplementation(project(":recon-engine:engine-core"))
    testImplementation("org.springframework.boot:spring-boot-starter-flyway")
    testImplementation("org.flywaydb:flyway-database-postgresql")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
}

// The runnable jar is always build/libs/app.jar, so Dockerfiles don't depend on the version.
tasks.bootJar { archiveFileName = "app.jar" }
tasks.jar { enabled = false }
