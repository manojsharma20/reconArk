plugins { id("reconark.library") }

description = "ArchUnit rules that keep the Lego boundaries honest. Scans every reconArk module."

dependencies {
    // everything framework-free that the rules inspect
    testImplementation(project(":kernel:plugin-api"))
    testImplementation(project(":kernel:plugin-runtime"))
    testImplementation(project(":kernel:pipeline"))
    testImplementation(project(":domain:model"))
    testImplementation(project(":domain:spi"))
    testImplementation(project(":domain:engine"))
    rootProject.subprojects.filter { it.path.startsWith(":plugins:") }.forEach { testImplementation(project(it.path)) }
    testImplementation(libs.archunit.junit5)
}
