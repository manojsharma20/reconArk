package io.reconark.services.operationsapi;

import io.reconark.kernel.api.HealthStatus;
import io.reconark.kernel.runtime.Kernel;
import io.reconark.kernel.runtime.PluginCatalog;
import io.reconark.kernel.runtime.PluginInventoryEntry;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal listener only (never routed by the public gateway). Run control (pause, resume, replay, DLQ requeue) is
 * added with the persistence adapters (LLD §8.5).
 */
@RestController
@RequestMapping("/api/ops/v1")
@PreAuthorize("hasRole('OPERATOR')")
class OperationsController {

    private final Kernel kernel;
    private final PluginCatalog catalog;

    OperationsController(Kernel kernel, PluginCatalog catalog) {
        this.kernel = kernel;
        this.catalog = catalog;
    }

    @GetMapping("/plugins")
    List<PluginInventoryEntry> plugins() {
        return kernel.inventory(catalog);
    }

    @GetMapping("/kernel/health")
    HealthStatus health() {
        return kernel.health();
    }
}
