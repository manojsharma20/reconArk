// Plugin: the standard field comparators (exact, ignore-case, number, date).
// Switch off with reconark.plugins.compare-standard.enabled=false
description = "FieldComparator plugins 'exact', 'ignore-case', 'number', 'date'"

dependencies {
    api(project(":backend:common"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}
