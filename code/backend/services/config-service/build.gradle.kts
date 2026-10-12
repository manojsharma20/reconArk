// config-service: create and edit recon definitions; list the plugins the UI can offer.
plugins {
    id("org.springframework.boot")
}

description = "reconArk config-service"

dependencies {
    implementation(project(":backend:common"))

    // Plugins this service knows about, so it can validate definitions and show the plugin catalogue.
    // Keep this list the same as the worker's.
    implementation(project(":backend:plugins:format-csv"))
    implementation(project(":backend:plugins:format-fixed-width"))
    implementation(project(":backend:plugins:compare-standard"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
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
