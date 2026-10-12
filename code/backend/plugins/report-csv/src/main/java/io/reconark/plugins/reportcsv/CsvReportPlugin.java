package io.reconark.plugins.reportcsv;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Plugs in the CSV report writer. Switch off with {@code reconark.plugins.report-csv.enabled=false}. */
@AutoConfiguration
@ConditionalOnProperty(name = "reconark.plugins.report-csv.enabled", havingValue = "true", matchIfMissing = true)
public class CsvReportPlugin {

    @Bean
    CsvReportWriter csvReportWriter() {
        return new CsvReportWriter();
    }
}
