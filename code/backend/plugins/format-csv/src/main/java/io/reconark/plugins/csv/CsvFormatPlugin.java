package io.reconark.plugins.csv;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Plugs the CSV reader into any service that has this module on its classpath. On by default; switch it off with
 * {@code reconark.plugins.format-csv.enabled=false}.
 */
@AutoConfiguration
@ConditionalOnProperty(name = "reconark.plugins.format-csv.enabled", havingValue = "true", matchIfMissing = true)
public class CsvFormatPlugin {

    @Bean
    CsvFileFormat csvFileFormat() {
        return new CsvFileFormat();
    }
}
