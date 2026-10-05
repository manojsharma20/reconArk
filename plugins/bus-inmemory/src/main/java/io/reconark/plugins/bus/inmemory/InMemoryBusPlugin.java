package io.reconark.plugins.bus.inmemory;

import io.reconark.kernel.api.ConfigSpec;
import io.reconark.kernel.api.ExtensionRegistrar;
import io.reconark.kernel.api.PluginContext;
import io.reconark.kernel.api.PluginDescriptor;
import io.reconark.kernel.api.PropertySpec;
import io.reconark.kernel.api.PropertyType;
import io.reconark.kernel.api.ReconArkPlugin;
import io.reconark.kernel.api.TrustTier;
import io.reconark.spi.ExtensionPoints;

/** Contributes {@code message-bus:inmemory}. */
public final class InMemoryBusPlugin implements ReconArkPlugin {

    private static final PluginDescriptor DESCRIPTOR = PluginDescriptor.builder("bus-inmemory", "0.2.0")
            .name("In-memory bus")
            .description("Development and test message bus")
            .trustTier(TrustTier.DEV_ONLY)
            .provides(ExtensionPoints.MESSAGE_BUS)
            .config(ConfigSpec.of(PropertySpec.optional("max-deliveries", PropertyType.INTEGER, 3, "deliveries before DLQ")))
            .build();

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void register(ExtensionRegistrar registrar, PluginContext context) {
        registrar.contribute(ExtensionPoints.MESSAGE_BUS, "inmemory", new InMemoryMessageBus(context.config().integer("max-deliveries")));
    }
}
