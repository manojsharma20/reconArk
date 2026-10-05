plugins { id("reconark.library") }

description = "reconArk kernel runtime: discovers, validates, composes and activates plugins."

dependencies {
    api(project(":kernel:plugin-api"))
}
