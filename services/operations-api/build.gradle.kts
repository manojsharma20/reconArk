plugins { id("reconark.spring-service") }

description = "Internal-only operations: runs, DLQ, plugin inventory and health."

dependencies {
    implementation(project(":platform:api-support"))
}
