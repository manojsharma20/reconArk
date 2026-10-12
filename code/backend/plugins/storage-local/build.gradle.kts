// Plugin: keeps uploaded files in a folder (a shared volume in docker-compose / Kubernetes).
// Replace with an object-storage plugin (S3, Azure Blob, GCS) in the cloud.
// Settings: reconark.plugins.storage-local.enabled (default true), reconark.plugins.storage-local.root
description = "FileStore plugin 'local'"

dependencies {
    api(project(":backend:common"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}
