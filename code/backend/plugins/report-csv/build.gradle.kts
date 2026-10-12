// Plugin: writes reconciliation results as CSV. Switch off with reconark.plugins.report-csv.enabled=false
description = "ReportWriter plugin 'csv'"

dependencies {
    api(project(":backend:common"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}
