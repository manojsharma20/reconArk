package io.reconark.report;

import io.reconark.common.plugin.PluginRegistry;
import io.reconark.common.plugin.ReportWriter;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** report-service: results and downloads. Reads the engine tables; never writes them. */
@SpringBootApplication
public class ReportServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportServiceApplication.class, args);
    }

    @Bean
    PluginRegistry<ReportWriter> reportWriters(List<ReportWriter> writers) {
        return new PluginRegistry<>("report", writers);
    }
}
