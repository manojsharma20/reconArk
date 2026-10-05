package io.reconark.services.reconapi;

import io.reconark.platform.api.EnableReconArkApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Recon results, field diffs, exception workflow, re-recon, rule-set preview. */
@SpringBootApplication
@EnableReconArkApi
@ConfigurationPropertiesScan
@EnableScheduling
public class ReconApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReconApiApplication.class, args);
    }
}
