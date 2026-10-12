// Plugin: reads CSV / delimited files. Switch off with reconark.plugins.format-csv.enabled=false
description = "FileFormat plugin 'csv'"

dependencies {
    api(project(":backend:common"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}
