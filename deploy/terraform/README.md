# Terraform — one module interface, one implementation per cloud (architecture §13, ADR-0021)

```
deploy/terraform/
  modules/
    platform-interface/   variables.tf + outputs.tf only: the contract every cloud implements
    aws/                  EKS, Aurora PostgreSQL, MSK, ElastiCache, S3, Secrets Manager, KMS, Transfer Family
    azure/                AKS, PostgreSQL Flexible Server, Event Hubs/Confluent, Managed Redis, Blob, Key Vault
    gcp/                  GKE, AlloyDB, Managed Kafka, Memorystore, GCS, Secret Manager, Cloud KMS
  environments/
    dev/ test/ prod/      choose a cloud module; same outputs feed the Helm values
```

Interface outputs (consumed by Helm values and compositions): `kubernetes_cluster`, `postgres_writer_endpoint`,
`postgres_reader_endpoint`, `kafka_bootstrap`, `object_store_buckets`, `secret_prefix`, `kms_key_ids`,
`workload_identity_bindings`. Policy-as-code (Checkov / OPA Conftest) runs in CI. Not yet implemented: this is the
next infrastructure increment (see the solution document roadmap).
