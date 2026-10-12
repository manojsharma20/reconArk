package io.reconark.config;

import io.reconark.common.plugin.FieldComparator;
import io.reconark.common.plugin.FileFormat;
import io.reconark.common.plugin.PluginRegistry;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** config-service: recon definitions and the plugin catalogue. */
@SpringBootApplication
public class ConfigServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
    }

    @Bean
    PluginRegistry<FileFormat> fileFormats(List<FileFormat> formats) {
        return new PluginRegistry<>("format", formats);
    }

    @Bean
    PluginRegistry<FieldComparator> fieldComparators(List<FieldComparator> comparators) {
        return new PluginRegistry<>("comparator", comparators);
    }
}
