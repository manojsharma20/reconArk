package io.reconark.services.adminapi;

import io.reconark.kernel.api.ExtensionLookup;
import io.reconark.kernel.runtime.Kernel;
import io.reconark.kernel.runtime.PluginCatalog;
import io.reconark.kernel.runtime.PluginInventoryEntry;
import io.reconark.spi.ExtensionPoints;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The Lego catalogue: installed bricks, their sockets and their configuration schemas (feeds schema-driven UI forms). */
@RestController
@RequestMapping("/api/admin/v1")
class PluginCatalogController {

    private final Kernel kernel;
    private final PluginCatalog catalog;
    private final ExtensionLookup extensions;

    PluginCatalogController(Kernel kernel, PluginCatalog catalog, ExtensionLookup extensions) {
        this.kernel = kernel;
        this.catalog = catalog;
        this.extensions = extensions;
    }

    @GetMapping("/plugins")
    List<PluginInventoryEntry> plugins() {
        return kernel.inventory(catalog);
    }

    /** Every socket and the keys currently plugged into it. */
    @GetMapping("/extension-points")
    List<ExtensionPointView> extensionPoints() {
        return ExtensionPoints.ALL.stream()
                .map(p -> new ExtensionPointView(p.id(), p.cardinality().name(), p.version().toString(), extensions.keys(p)))
                .toList();
    }

    record ExtensionPointView(String id, String cardinality, String version, Set<String> activeKeys) {}
}
