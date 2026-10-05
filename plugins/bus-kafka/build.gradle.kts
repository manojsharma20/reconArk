plugins { id("reconark.plugin") }

description = "Kafka message bus (default broker, ADR-0008). Idempotent producer, manual commits, per-key ordering."

dependencies {
    implementation(libs.kafka.clients)
}
