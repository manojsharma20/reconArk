// Root build: no logic here. Conventions live in build-logic/ (ADR-0014).
version = providers.gradleProperty("reconarkVersion").getOrElse("0.2.0-SNAPSHOT")

tasks.register("verifyQuick") {
    group = "verification"
    description = "Fast inner-loop check used by Claude Code hooks: compile + unit tests of kernel, domain and plugins."
    dependsOn(
        subprojects
            .filter { it.path.startsWith(":kernel") || it.path.startsWith(":domain") || it.path.startsWith(":plugins") }
            .map { "${it.path}:test" },
    )
}
