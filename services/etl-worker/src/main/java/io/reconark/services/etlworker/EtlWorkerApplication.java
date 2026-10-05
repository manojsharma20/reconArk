package io.reconark.services.etlworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Executes ETL chunks through the provider's configured stage pipeline. */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class EtlWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EtlWorkerApplication.class, args);
    }
}
