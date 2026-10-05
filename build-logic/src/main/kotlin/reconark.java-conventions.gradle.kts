// Base conventions for every reconArk Java module.
plugins {
    java
    jacoco
}

group = "io.reconark"
version = rootProject.version

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
    withSourcesJar()
}

repositories { mavenCentral() }

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
    // Final features only (no --enable-preview, ADR-0013).
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing,-serial,-this-escape", "-parameters"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes("Implementation-Title" to project.name, "Implementation-Version" to project.version)
    }
}
