plugins { id("reconark.library") }

description = "Technology Compatibility Kit: abstract test suites every plugin of an extension point must pass (ADR-0035)."

dependencies {
    api(project(":kernel:plugin-runtime"))
    api(project(":domain:spi"))
    api(platform(libs.junit.bom))
    api(libs.junit.jupiter)
    api(libs.assertj.core)
}
