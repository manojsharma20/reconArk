package io.reconark.worker;

import io.reconark.common.plugin.FieldComparator;
import io.reconark.common.plugin.FileFormat;
import io.reconark.common.plugin.PluginRegistry;
import io.reconark.engine.matching.Matcher;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Collects the active plugins (every enabled plugin module registers its beans) into registries. */
@Configuration(proxyBeanMethods = false)
public class WorkerConfig {

    @Bean
    PluginRegistry<FileFormat> fileFormats(List<FileFormat> formats) {
        return new PluginRegistry<>("format", formats);
    }

    @Bean
    PluginRegistry<FieldComparator> fieldComparators(List<FieldComparator> comparators) {
        return new PluginRegistry<>("comparator", comparators);
    }

    @Bean
    Matcher matcher(PluginRegistry<FieldComparator> fieldComparators) {
        return new Matcher(fieldComparators);
    }
}
