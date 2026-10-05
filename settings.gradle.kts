pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.PREFER_SETTINGS
    repositories { mavenCentral() }
}

rootProject.name = "reconark"

// --- Kernel: the Lego baseplate. Framework-free.
include(":kernel:plugin-api")
include(":kernel:plugin-runtime")
include(":kernel:pipeline")

// --- Domain: canonical model, extension-point contracts (SPI), engines.
include(":domain:model")
include(":domain:spi")
include(":domain:engine")

// --- Plugins: the Lego bricks. Add a brick = add a module here + a composition entry.
include(":plugins:recon-standard")
include(":plugins:format-delimited")
include(":plugins:bus-inmemory")
include(":plugins:bus-kafka")
include(":plugins:secrets-env")
include(":plugins:storage-filesystem")
include(":plugins:report-csv")

// --- Platform: Spring integration shared by every service.
include(":platform:spring-boot-starter")
include(":platform:api-support")

// --- Services: one bounded context each; each is optional per environment.
include(":services:web-bff")
include(":services:admin-api")
include(":services:ingestion-api")
include(":services:recon-api")
include(":services:reports-api")
include(":services:operations-api")
include(":services:scheduler")
include(":services:etl-worker")
include(":services:recon-worker")
include(":services:report-worker")
include(":services:outbox-relay")

// --- Testing: plugin TCK and architecture rules.
include(":testing:plugin-tck")
include(":testing:architecture-tests")
