package io.reconark.plugins.bus.kafka;

import io.reconark.kernel.api.ConfigSpec;
import io.reconark.kernel.api.ExtensionRegistrar;
import io.reconark.kernel.api.HealthStatus;
import io.reconark.kernel.api.PluginContext;
import io.reconark.kernel.api.PluginDescriptor;
import io.reconark.kernel.api.PropertySpec;
import io.reconark.kernel.api.PropertyType;
import io.reconark.kernel.api.ReconArkPlugin;
import io.reconark.spi.ExtensionPoints;

/** Contributes {@code message-bus:kafka}. */
public final class KafkaBusPlugin implements ReconArkPlugin {

    private static final PluginDescriptor DESCRIPTOR = PluginDescriptor.builder("bus-kafka", "0.2.0")
            .name("Kafka bus")
            .description("Apache Kafka 4.x (MSK, Confluent, Strimzi, Event Hubs Kafka endpoint)")
            .provides(ExtensionPoints.MESSAGE_BUS)
            .config(ConfigSpec.of(
                    PropertySpec.required("bootstrap-servers", PropertyType.STRING, "host:port list"),
                    PropertySpec.optional("security-protocol", PropertyType.STRING, "SSL", "SSL or SASL_SSL in every shared environment"),
                    PropertySpec.optional("client-id", PropertyType.STRING, "reconark", "client id prefix"),
                    PropertySpec.optional("max-poll-records", PropertyType.INTEGER, 100, "records per poll"),
                    PropertySpec.optional("max-deliveries", PropertyType.INTEGER, 5, "deliveries before DLQ")))
            .build();

    private KafkaMessageBus bus;

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void register(ExtensionRegistrar registrar, PluginContext context) {
        bus = new KafkaMessageBus(context.config());
        registrar.contribute(ExtensionPoints.MESSAGE_BUS, "kafka", bus);
    }

    @Override
    public HealthStatus health() {
        return bus == null ? HealthStatus.down("not started") : HealthStatus.UP;
    }

    @Override
    public void close() {
        if (bus != null) {
            try {
                bus.close();
            } catch (Exception ignored) {
                // shutting down
            }
        }
    }
}
