package io.reconark.services.ingestionapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Partner push: authenticated batches stored as raw artifacts, run requested via the outbox. */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class IngestionApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngestionApiApplication.class, args);
    }
}
