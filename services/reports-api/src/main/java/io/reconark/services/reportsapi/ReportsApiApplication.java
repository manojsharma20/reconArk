package io.reconark.services.reportsapi;

import io.reconark.platform.api.EnableReconArkApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Report definitions, requests, schedules and signed downloads. */
@SpringBootApplication
@EnableReconArkApi
@ConfigurationPropertiesScan
@EnableScheduling
public class ReportsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportsApiApplication.class, args);
    }
}
