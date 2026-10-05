package io.reconark.services.reconworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Executes batch recon buckets and streaming matches with the configured rule sets. */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class ReconWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReconWorkerApplication.class, args);
    }
}
