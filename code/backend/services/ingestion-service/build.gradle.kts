// ingestion-service: receives uploaded files, stores them with the FileStore plugin and records their details.
plugins {
    id("org.springframework.boot")
}

description = "reconArk ingestion-service"

dependencies {
    implementation(project(":backend:common"))
    implementation(project(":backend:plugins:storage-local"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
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
