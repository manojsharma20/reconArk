plugins { id("reconark.library") }

description = "reconArk pipeline engine: runs configuration-declared stage graphs built from pipeline-stage extensions."

dependencies {
    api(project(":kernel:plugin-api"))
    testImplementation(project(":kernel:plugin-runtime"))
}
