package io.reconark.manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The recon-engine manager. Read {@link io.reconark.manager.run.RunService} first: it holds the whole run lifecycle.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class ManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManagerApplication.class, args);
    }
}
