package io.reconark.manager;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Manager settings ({@code reconark.manager.*} in application.yaml).
 *
 * @param bucketCount          how many MATCH jobs a run is split into; more buckets = more parallel workers
 * @param maxAttempts          how often a job is tried before the run fails
 * @param jobTimeout           a job with no answer after this long is sent again (its worker probably died)
 * @param keepStagingRecords   keep loaded records after a run completes (for debugging); normally false
 * @param configServiceUrl     where config-service runs (to read recon definitions)
 * @param ingestionServiceUrl  where ingestion-service runs (to read uploaded file details)
 */
@ConfigurationProperties("reconark.manager")
public record ManagerProperties(
        @DefaultValue("16") int bucketCount,
        @DefaultValue("3") int maxAttempts,
        @DefaultValue("10m") Duration jobTimeout,
        @DefaultValue("false") boolean keepStagingRecords,
        @DefaultValue("http://localhost:8081") String configServiceUrl,
        @DefaultValue("http://localhost:8082") String ingestionServiceUrl) {}
