package io.reconark.services.ingestionapi;

import io.reconark.platform.api.EnableReconArkApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Partner push: authenticated batches stored as raw artifacts, run requested via the outbox. */
@SpringBootApplication
@EnableReconArkApi
@ConfigurationPropertiesScan
@EnableScheduling
public class IngestionApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngestionApiApplication.class, args);
    }
}
