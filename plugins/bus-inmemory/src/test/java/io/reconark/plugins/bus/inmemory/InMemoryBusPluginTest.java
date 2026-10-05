package io.reconark.plugins.bus.inmemory;

import io.reconark.kernel.api.ReconArkPlugin;
import io.reconark.spi.MessageBus;
import io.reconark.testing.tck.MessageBusTck;
import io.reconark.testing.tck.PluginContractTck;
import org.junit.jupiter.api.Nested;

class InMemoryBusPluginTest extends PluginContractTck {

    @Override
    protected ReconArkPlugin plugin() {
        return new InMemoryBusPlugin();
    }

    @Nested
    class Tck extends MessageBusTck {
        private final MessageBus bus = new InMemoryMessageBus(3);

        @Override
        protected MessageBus bus() {
            return bus;
        }
    }
}
