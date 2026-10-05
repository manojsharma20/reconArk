package io.reconark.platform.starter;

import io.reconark.kernel.runtime.Kernel;
import io.reconark.kernel.runtime.PluginCatalog;
import io.reconark.kernel.runtime.PluginInventoryEntry;
import java.util.List;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/** {@code GET /actuator/plugins}: the installed bricks, their state, contributions and health. */
@Endpoint(id = "plugins")
public class PluginsEndpoint {

    private final Kernel kernel;
    private final PluginCatalog catalog;

    public PluginsEndpoint(Kernel kernel, PluginCatalog catalog) {
        this.kernel = kernel;
        this.catalog = catalog;
    }

    @ReadOperation
    public List<PluginInventoryEntry> plugins() {
        return kernel.inventory(catalog);
    }
}
