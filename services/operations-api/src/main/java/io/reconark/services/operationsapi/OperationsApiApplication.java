package io.reconark.services.operationsapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Internal-only operations: runs, DLQ, plugin inventory and health. */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class OperationsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OperationsApiApplication.class, args);
    }
}
