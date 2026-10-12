// Plugin: reads fixed-width text files (common for bank and card-scheme files).
// Switch off with reconark.plugins.format-fixed-width.enabled=false
description = "FileFormat plugin 'fixed-width'"

dependencies {
    api(project(":backend:common"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}
