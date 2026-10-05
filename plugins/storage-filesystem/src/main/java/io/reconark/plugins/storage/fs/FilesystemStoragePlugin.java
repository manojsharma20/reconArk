package io.reconark.plugins.storage.fs;

import io.reconark.kernel.api.ConfigSpec;
import io.reconark.kernel.api.ExtensionRegistrar;
import io.reconark.kernel.api.PluginContext;
import io.reconark.kernel.api.PluginDescriptor;
import io.reconark.kernel.api.PropertySpec;
import io.reconark.kernel.api.PropertyType;
import io.reconark.kernel.api.ReconArkPlugin;
import io.reconark.kernel.api.TrustTier;
import io.reconark.spi.ExtensionPoints;
import java.nio.file.Path;

/** Contributes {@code object-store:filesystem}. */
public final class FilesystemStoragePlugin implements ReconArkPlugin {

    private static final PluginDescriptor DESCRIPTOR = PluginDescriptor.builder("storage-filesystem", "0.2.0")
            .name("Filesystem object store")
            .description("Local directory as object storage (development)")
            .trustTier(TrustTier.DEV_ONLY)
            .provides(ExtensionPoints.OBJECT_STORE)
            .config(ConfigSpec.of(PropertySpec.optional("root", PropertyType.STRING, "./build/object-store", "root directory")))
            .build();

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void register(ExtensionRegistrar registrar, PluginContext context) {
        registrar.contribute(ExtensionPoints.OBJECT_STORE, "filesystem", new FilesystemObjectStore(Path.of(context.config().string("root"))));
    }
}
