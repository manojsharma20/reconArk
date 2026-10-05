// A framework-free library (kernel, domain, SPI, plugins). No Spring here — enforced by ArchUnit.
plugins {
    id("reconark.java-conventions")
    `java-library`
}

val catalog = the<VersionCatalogsExtension>().named("libs")

dependencies {
    testImplementation(platform(catalog.findLibrary("junit-bom").get()))
    testImplementation(catalog.findLibrary("junit-jupiter").get())
    testImplementation(catalog.findLibrary("assertj-core").get())
    testRuntimeOnly(catalog.findLibrary("junit-platform-launcher").get())
}

// Framework-free code compiles warning-free.
tasks.withType<JavaCompile>().configureEach { options.compilerArgs.add("-Werror") }
