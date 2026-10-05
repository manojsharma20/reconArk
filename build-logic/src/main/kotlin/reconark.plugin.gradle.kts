// A reconArk plugin: implements SPI extension points, registered through
// META-INF/services/io.reconark.kernel.api.ReconArkPlugin, and must pass the TCK of every point it provides.
plugins {
    id("reconark.library")
}

dependencies {
    api(project(":kernel:plugin-api"))
    api(project(":domain:spi"))
    testImplementation(project(":testing:plugin-tck"))
    testImplementation(project(":kernel:plugin-runtime"))
}

tasks.named<Jar>("jar") {
    manifest { attributes("ReconArk-Plugin" to "true") }
}
