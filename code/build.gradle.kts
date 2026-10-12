// Settings shared by every Java module. Module-specific dependencies live in each module's build.gradle.kts.
plugins {
    alias(libs.plugins.spring.boot) apply false
}

val springBootVersion = libs.versions.spring.boot.get()

subprojects {
    // Folders that only group modules (backend, backend/plugins, ...) have no build file: skip them.
    if (!file("build.gradle.kts").exists()) return@subprojects

    apply(plugin = "java-library")
    group = "io.reconark"
    version = "0.3.0-SNAPSHOT"

    extensions.configure<JavaPluginExtension> {
        toolchain { languageVersion = JavaLanguageVersion.of(25) }
    }

    dependencies {
        // The Spring Boot BOM manages the versions of all Spring, Kafka, Flyway, Jackson, JUnit and Testcontainers libraries.
        "implementation"(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
        "testImplementation"(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}

// Fast check without Docker (used by the Claude Code Stop hook): compile everything,
// run the unit tests of the plain-Java modules and the plugins.
tasks.register("quickCheck") {
    group = "verification"
    description = "Compiles all modules and runs the tests that don't need Docker."
    dependsOn(subprojects.filter { it.buildFile.exists() }.map { "${it.path}:compileTestJava" })
    dependsOn(
        subprojects
            .filter { it.buildFile.exists() }
            .filter { it.path.startsWith(":backend:plugins:") || it.path in setOf(":backend:common", ":recon-engine:engine-core") }
            .map { "${it.path}:test" },
    )
}
