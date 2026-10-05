package io.reconark.services.reportworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Renders reports through report-renderer extensions into the object store. */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class ReportWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportWorkerApplication.class, args);
    }
}
