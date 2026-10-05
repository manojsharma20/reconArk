package io.reconark.kernel.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.reconark.kernel.api.Cardinality;
import io.reconark.kernel.api.ConfigSpec;
import io.reconark.kernel.api.ExtensionPoint;
import io.reconark.kernel.api.ExtensionRegistrar;
import io.reconark.kernel.api.KernelError;
import io.reconark.kernel.api.KernelException;
import io.reconark.kernel.api.PluginContext;
import io.reconark.kernel.api.PluginDescriptor;
import io.reconark.kernel.api.PropertySpec;
import io.reconark.kernel.api.PropertyType;
import io.reconark.kernel.api.ReconArkPlugin;
import io.reconark.kernel.api.TrustTier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class PluginRuntimeTest {

    interface Greeter {
        String greet(String name);
    }

    interface Secrets {
        String get(String ref);
    }

    static final ExtensionPoint<Greeter> GREETER = ExtensionPoint.of("greeter", Greeter.class, Cardinality.SINGLE);
    static final ExtensionPoint<Greeter> STYLE = ExtensionPoint.of("greeting-style", Greeter.class, Cardinality.KEYED);
    static final ExtensionPoint<Greeter> CHAIN = ExtensionPoint.of("greeting-chain", Greeter.class, Cardinality.CHAIN);
    static final ExtensionPoint<Secrets> SECRETS = ExtensionPoint.of("secrets", Secrets.class, Cardinality.SINGLE);

    /** A configurable test plugin. */
    record TestPlugin(
            PluginDescriptor descriptor, java.util.function.BiConsumer<ExtensionRegistrar, PluginContext> body, Runnable onClose)
            implements ReconArkPlugin {
        TestPlugin(PluginDescriptor descriptor, java.util.function.BiConsumer<ExtensionRegistrar, PluginContext> body) {
            this(descriptor, body, () -> {});
        }

        @Override
        public void register(ExtensionRegistrar registrar, PluginContext context) {
            body.accept(registrar, context);
        }

        @Override
        public void close() {
            onClose.run();
        }
    }

    static TestPlugin greeter(String id, String key, Supplier<String> prefix) {
        return new TestPlugin(
                PluginDescriptor.builder(id, "1.0.0").description("t").provides(GREETER).build(),
                (r, c) -> r.contribute(GREETER, key, n -> prefix.get() + n));
    }

    static CompositionConfig composition(Map<String, ?> map) {
        return CompositionConfig.fromMap(map);
    }

    @Test
    void swappingABrickIsAConfigurationChange() {
        PluginCatalog catalog = PluginCatalog.of(greeter("greeter-en", "en", () -> "Hello "), greeter("greeter-ar", "ar", () -> "Marhaba "));

        Kernel en = new PluginRuntime().boot(catalog, composition(Map.of("plugins", Map.of("greeter-en", Map.of()))));
        Kernel ar = new PluginRuntime().boot(catalog, composition(Map.of("plugins", Map.of("greeter-ar", Map.of()))));

        assertThat(en.extensions().single(GREETER).greet("Manoj")).isEqualTo("Hello Manoj");
        assertThat(ar.extensions().single(GREETER).greet("Manoj")).isEqualTo("Marhaba Manoj");
    }

    @Test
    void enabledListAndPluginsMapCombine() {
        CompositionConfig c = composition(Map.of(
                "enabled", List.of("a-p", "b-p"),
                "plugins", Map.of("b-p", Map.of("config", Map.of("x", 1)))));
        assertThat(c.plugins()).containsOnlyKeys("a-p", "b-p");
        assertThat(c.plugins().get("b-p").config()).containsEntry("x", 1);
        assertThat(c.plugins().get("a-p").config()).isEmpty();
    }

    @Test
    void severalSingleCandidatesNeedABinding() {
        PluginCatalog catalog = PluginCatalog.of(greeter("greeter-en", "en", () -> "Hello "), greeter("greeter-ar", "ar", () -> "Marhaba "));
        Map<String, Object> both = Map.of("greeter-en", Map.of(), "greeter-ar", Map.of());

        assertThatThrownBy(() -> new PluginRuntime().boot(catalog, composition(Map.of("plugins", both))))
                .isInstanceOf(KernelException.class)
                .extracting(e -> ((KernelException) e).error())
                .isEqualTo(KernelError.MISSING_BINDING);

        Kernel k = new PluginRuntime().boot(catalog, composition(Map.of("plugins", both, "bindings", Map.of("greeter", "ar"))));
        assertThat(k.extensions().single(GREETER).greet("x")).isEqualTo("Marhaba x");
    }

    @Test
    void unknownPluginFailsFast() {
        assertThatThrownBy(() -> new PluginRuntime().boot(PluginCatalog.of(), composition(Map.of("plugins", Map.of("nope", Map.of())))))
                .hasMessageStartingWith("RK-KRN-0001");
    }

    @Test
    void disabledWinsOverEnabled() {
        PluginCatalog catalog = PluginCatalog.of(greeter("greeter-en", "en", () -> "Hello "));
        Kernel k = new PluginRuntime().boot(catalog, composition(Map.of(
                "plugins", Map.of("greeter-en", Map.of()), "disabled", List.of("greeter-en"))));
        assertThat(k.extensions().keys(GREETER)).isEmpty();
        assertThat(k.inventory(catalog)).hasSize(1);
        assertThat(k.inventory(catalog).getFirst().state()).isEqualTo(PluginState.DISABLED);
    }

    @Test
    void devOnlyPluginsAreRefusedInProduction() {
        TestPlugin dev = new TestPlugin(
                PluginDescriptor.builder("dev-greeter", "1.0.0").description("t").trustTier(TrustTier.DEV_ONLY).provides(GREETER).build(),
                (r, c) -> r.contribute(GREETER, "dev", n -> n));
        assertThatThrownBy(() -> new PluginRuntime().boot(PluginCatalog.of(dev), composition(Map.of(
                "environment", "prod", "plugins", Map.of("dev-greeter", Map.of())))))
                .hasMessageStartingWith("RK-KRN-0004");
    }

    @Test
    void incompatibleKernelApiIsRefused() {
        TestPlugin future = new TestPlugin(
                PluginDescriptor.builder("future", "1.0.0").description("t").kernelApi("[2.0,3.0)").provides(GREETER).build(),
                (r, c) -> r.contribute(GREETER, "f", n -> n));
        assertThatThrownBy(() -> new PluginRuntime().boot(PluginCatalog.of(future), composition(Map.of("plugins", Map.of("future", Map.of())))))
                .hasMessageStartingWith("RK-KRN-0003");
    }

    @Test
    void configIsValidatedDefaultedAndTyped() {
        TestPlugin p = new TestPlugin(
                PluginDescriptor.builder("conf", "1.0.0").description("t").provides(GREETER)
                        .config(ConfigSpec.of(
                                PropertySpec.required("prefix", PropertyType.STRING, "p"),
                                PropertySpec.optional("repeat", PropertyType.INTEGER, 1, "r")))
                        .build(),
                (r, c) -> r.contribute(GREETER, "c", n -> c.config().string("prefix").repeat(c.config().integer("repeat")) + n));

        assertThatThrownBy(() -> new PluginRuntime().boot(PluginCatalog.of(p), composition(Map.of(
                "plugins", Map.of("conf", Map.of("config", Map.of("repeat", "two", "extra", 1)))))))
                .hasMessageContaining("missing required property 'prefix'")
                .hasMessageContaining("unknown property 'extra'")
                .hasMessageContaining("'repeat' is not a valid INTEGER");

        Kernel k = new PluginRuntime().boot(PluginCatalog.of(p), composition(Map.of(
                "plugins", Map.of("conf", Map.of("config", Map.of("prefix", "> "))))));
        assertThat(k.extensions().single(GREETER).greet("a")).isEqualTo("> a");
    }

    @Test
    void requirementsAreRegisteredFirst() {
        TestPlugin secrets = new TestPlugin(
                PluginDescriptor.builder("secrets-x", "1.0.0").description("t").provides(SECRETS).build(),
                (r, c) -> r.contribute(SECRETS, "x", ref -> "s3cr3t"));
        TestPlugin user = new TestPlugin(
                PluginDescriptor.builder("needs-secrets", "1.0.0").description("t").provides(GREETER).requires(SECRETS).build(),
                (r, c) -> {
                    String s = c.lookup().single(SECRETS).get("ref");
                    r.contribute(GREETER, "s", n -> s.length() + n);
                });
        Map<String, Object> plugins = new java.util.LinkedHashMap<>();
        plugins.put("needs-secrets", Map.of()); // declared first on purpose
        plugins.put("secrets-x", Map.of());
        Kernel k = new PluginRuntime().boot(PluginCatalog.of(user, secrets), composition(Map.of("plugins", plugins)));
        assertThat(k.extensions().single(GREETER).greet("!")).isEqualTo("6!");

        assertThatThrownBy(() -> new PluginRuntime().boot(PluginCatalog.of(user), composition(Map.of("plugins", Map.of("needs-secrets", Map.of())))))
                .hasMessageStartingWith("RK-KRN-0006");
    }

    @Test
    void undeclaredContributionIsRefused() {
        TestPlugin sneaky = new TestPlugin(
                PluginDescriptor.builder("sneaky", "1.0.0").description("t").provides(STYLE).build(),
                (r, c) -> r.contribute(GREETER, "x", n -> n));
        assertThatThrownBy(() -> new PluginRuntime().boot(PluginCatalog.of(sneaky), composition(Map.of("plugins", Map.of("sneaky", Map.of())))))
                .hasMessageStartingWith("RK-KRN-0014");
    }

    @Test
    void chainsFollowConfiguredOrder() {
        TestPlugin p = new TestPlugin(
                PluginDescriptor.builder("chain", "1.0.0").description("t").provides(CHAIN).build(),
                (r, c) -> {
                    r.contribute(CHAIN, "a", n -> n + "a");
                    r.contribute(CHAIN, "b", n -> n + "b");
                    r.contribute(CHAIN, "c", n -> n + "c");
                });
        Kernel k = new PluginRuntime().boot(PluginCatalog.of(p), composition(Map.of(
                "plugins", Map.of("chain", Map.of()), "chains", Map.of("greeting-chain", List.of("c", "a")))));
        String out = "";
        for (Greeter g : k.extensions().chain(CHAIN)) {
            out = g.greet(out);
        }
        assertThat(out).isEqualTo("ca");
    }

    @Test
    void interceptorsWrapEveryCall() {
        List<String> seen = new ArrayList<>();
        ExtensionInterceptor timing = (call, next) -> {
            seen.add(call.pointId() + ":" + call.key() + "#" + call.method());
            return next.proceed();
        };
        Kernel k = new PluginRuntime(List.of(timing)).boot(
                PluginCatalog.of(greeter("greeter-en", "en", () -> "Hi ")), composition(Map.of("plugins", Map.of("greeter-en", Map.of()))));
        assertThat(k.extensions().single(GREETER).greet("x")).isEqualTo("Hi x");
        assertThat(seen).containsExactly("greeter:en#greet");
    }

    @Test
    void closeStopsPluginsInReverseOrder() {
        List<String> stopped = new ArrayList<>();
        ReconArkPlugin a = new TestPlugin(PluginDescriptor.builder("a-p", "1.0.0").description("t").provides(SECRETS).build(),
                (r, c) -> r.contribute(SECRETS, "a", ref -> ref), () -> stopped.add("a"));
        ReconArkPlugin b = new TestPlugin(PluginDescriptor.builder("b-p", "1.0.0").description("t").provides(GREETER).requires(SECRETS).build(),
                (r, c) -> r.contribute(GREETER, "b", n -> n), () -> stopped.add("b"));
        Kernel k = new PluginRuntime().boot(PluginCatalog.of(a, b), composition(Map.of("plugins", Map.of("a-p", Map.of(), "b-p", Map.of()))));
        k.close();
        assertThat(stopped).containsExactly("b", "a");
    }
}
