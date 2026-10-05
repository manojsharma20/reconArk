package io.reconark.services.webbff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Backend-for-frontend: OIDC token handler, config-driven route table, UI manifest. */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class WebBffApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebBffApplication.class, args);
    }
}
