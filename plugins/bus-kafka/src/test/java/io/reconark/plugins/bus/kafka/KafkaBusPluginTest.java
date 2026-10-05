package io.reconark.plugins.bus.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import io.reconark.kernel.api.KernelApi;
import io.reconark.spi.ExtensionPoints;
import org.junit.jupiter.api.Test;

/**
 * Descriptor checks only. The full {@code MessageBusTck} runs against a real broker in the integration suite
 * (Testcontainers), which needs Docker and is not part of the unit build.
 */
class KafkaBusPluginTest {

    @Test
    void descriptorDeclaresTheBusAndRequiredBootstrapServers() {
        var d = new KafkaBusPlugin().descriptor();
        assertThat(d.provides()).containsExactly(ExtensionPoints.MESSAGE_BUS.id());
        assertThat(d.kernelApi().contains(KernelApi.VERSION)).isTrue();
        assertThat(d.config().byName().get("bootstrap-servers").required()).isTrue();
    }
}
