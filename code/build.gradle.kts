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
