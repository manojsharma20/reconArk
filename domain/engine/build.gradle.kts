plugins { id("reconark.library") }

description = "reconArk engines: the generic ETL stages and the generic recon engine. Behaviour comes from extensions."

dependencies {
    api(project(":domain:spi"))
    api(project(":kernel:plugin-runtime"))
    testImplementation(project(":plugins:recon-standard"))
    testImplementation(project(":plugins:format-delimited"))
}
