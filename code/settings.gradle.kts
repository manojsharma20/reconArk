// reconArk — one Gradle build for all Java code.
// Each include() below is one module. To add a module: create the folder, add its build.gradle.kts, add a line here.

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories { mavenCentral() }
}

rootProject.name = "reconark"

// Backend: shared library, plugins (the "Lego bricks") and business microservices
include(":backend:common")
include(":backend:plugins:format-csv")
include(":backend:plugins:format-fixed-width")
include(":backend:plugins:compare-standard")
include(":backend:plugins:report-csv")
include(":backend:plugins:storage-local")
include(":backend:services:config-service")
include(":backend:services:ingestion-service")
include(":backend:services:report-service")

// Recon engine: distributed manager + workers that talk over Kafka
include(":recon-engine:engine-core")
include(":recon-engine:manager")
include(":recon-engine:worker")
