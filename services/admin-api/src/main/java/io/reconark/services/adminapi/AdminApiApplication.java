package io.reconark.services.adminapi;

import io.reconark.platform.api.EnableReconArkApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Onboarding and configuration: providers, pipelines, rule sets, maker-checker, plugin catalog. */
@SpringBootApplication
@EnableReconArkApi
@ConfigurationPropertiesScan
@EnableScheduling
public class AdminApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApiApplication.class, args);
    }
}
