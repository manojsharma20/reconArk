package io.reconark.testing.tck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.reconark.kernel.api.ExtensionLookup;
import io.reconark.kernel.api.ExtensionPoint;
import io.reconark.kernel.api.ExtensionRegistrar;
import io.reconark.kernel.api.KernelApi;
import io.reconark.kernel.api.PluginConfig;
import io.reconark.kernel.api.PluginContext;
import io.reconark.kernel.api.PluginDescriptor;
import io.reconark.kernel.api.ReconArkPlugin;
import io.reconark.kernel.runtime.ConfigValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Contract every plugin honours, whatever it provides. */
public abstract class PluginContractTck {

    /** A fresh plugin instance. */
    protected abstract ReconArkPlugin plugin();

    /** Configuration used for the registration test; defaults to empty (all defaults). */
    protected Map<String, Object> configuration() {
        return Map.of();
    }

    /** Lookup offered to plugins that {@code require} other points; override when needed. */
    protected ExtensionLookup requirements() {
        return EmptyLookup.INSTANCE;
    }

    @Test
    void descriptorIsComplete() {
        PluginDescriptor d = plugin().descriptor();
        assertThat(d.id()).matches("[a-z][a-z0-9]*(-[a-z0-9]+)*");
        assertThat(d.provides()).as("a plugin must provide at least one extension point").isNotEmpty();
        assertThat(d.kernelApi().contains(KernelApi.VERSION)).as("supports the current kernel API").isTrue();
        assertThat(d.description()).isNotBlank();
    }

    @Test
    void descriptorIsStable() {
        assertThat(plugin().descriptor()).isEqualTo(plugin().descriptor());
    }

    @Test
    void configSchemaIsWellFormed() {
        String schema = plugin().descriptor().config().toJsonSchema("t");
        assertThat(schema).startsWith("{").endsWith("}").contains("\"additionalProperties\":false");
    }

    @Test
    void registersOnlyDeclaredPointsAndUniqueKeys() {
        ReconArkPlugin plugin = plugin();
        PluginDescriptor d = plugin.descriptor();
        PluginConfig config = ConfigValidator.validate(d.id(), d.config(), configuration());
        List<String> contributions = new ArrayList<>();
        ExtensionRegistrar recorder = new ExtensionRegistrar() {
            @Override
            public <T> void contribute(ExtensionPoint<T> point, String key, T implementation) {
                assertThat(d.provides()).as("declared in provides").contains(point.id());
                assertThat(point.type().isInstance(implementation)).isTrue();
                assertThat(key).matches("[a-z][a-z0-9]*(-[a-z0-9]+)*");
                contributions.add(point.id() + ":" + key);
            }
        };
        PluginContext ctx = new PluginContext() {
            @Override
            public PluginConfig config() {
                return config;
            }

            @Override
            public String environment() {
                return "test";
            }

            @Override
            public ExtensionLookup lookup() {
                return requirements();
            }
        };
        assertThatCode(() -> plugin.register(recorder, ctx)).doesNotThrowAnyException();
        assertThat(contributions).doesNotHaveDuplicates().isNotEmpty();
        assertThatCode(plugin::close).doesNotThrowAnyException();
    }

    @Test
    void isDiscoverableThroughServiceLoader() {
        String id = plugin().descriptor().id();
        boolean found = ServiceLoader.load(ReconArkPlugin.class).stream().anyMatch(p -> p.get().descriptor().id().equals(id));
        assertThat(found).as("META-INF/services registers " + id).isTrue();
    }

    /** No requirements available. */
    private enum EmptyLookup implements ExtensionLookup {
        INSTANCE;

        @Override
        public <T> T single(ExtensionPoint<T> point) {
            throw new IllegalStateException("no " + point.id());
        }

        @Override
        public <T> T keyed(ExtensionPoint<T> point, String key) {
            throw new IllegalStateException("no " + point.id());
        }

        @Override
        public <T> Optional<T> findKeyed(ExtensionPoint<T> point, String key) {
            return Optional.empty();
        }

        @Override
        public Set<String> keys(ExtensionPoint<?> point) {
            return Set.of();
        }

        @Override
        public <T> List<T> chain(ExtensionPoint<T> point) {
            return List.of();
        }
    }
}
