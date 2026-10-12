package io.reconark.config;

import io.reconark.common.plugin.FieldComparator;
import io.reconark.common.plugin.FileFormat;
import io.reconark.common.plugin.PluginInfo;
import io.reconark.common.plugin.PluginRegistry;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/plugins}: the formats and comparators that are switched on, for the definition editor in the UI. */
@RestController
public class PluginCatalogController {

    private final PluginRegistry<FileFormat> formats;
    private final PluginRegistry<FieldComparator> comparators;

    public PluginCatalogController(PluginRegistry<FileFormat> formats, PluginRegistry<FieldComparator> comparators) {
        this.formats = formats;
        this.comparators = comparators;
    }

    @GetMapping("/api/plugins")
    public List<PluginInfo> plugins() {
        List<PluginInfo> all = new ArrayList<>();
        formats.all().forEach(p -> all.add(PluginInfo.of("format", p)));
        comparators.all().forEach(p -> all.add(PluginInfo.of("comparator", p)));
        return all;
    }
}
