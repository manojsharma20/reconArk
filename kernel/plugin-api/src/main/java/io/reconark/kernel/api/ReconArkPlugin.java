package io.reconark.kernel.api;

/**
 * Entry point of every plugin — a Lego brick. Implementations are discovered through {@link java.util.ServiceLoader}
 * ({@code META-INF/services/io.reconark.kernel.api.ReconArkPlugin}) or provided as remote proxies.
 *
 * <p>Lifecycle: {@code DISCOVERED -> VALIDATED -> REGISTERED -> ACTIVE -> STOPPED} (or {@code FAILED}).
 */
public interface ReconArkPlugin extends AutoCloseable {

    /** Static description. Must be cheap and side-effect free. */
    PluginDescriptor descriptor();

    /** Contributes extensions. Called once, in dependency order, only if the composition enables the plugin. */
    void register(ExtensionRegistrar registrar, PluginContext context);

    /** Current health; included in readiness. */
    default HealthStatus health() {
        return HealthStatus.UP;
    }

    /** Releases resources (connections, threads). Called in reverse registration order. */
    @Override
    default void close() {}
}
