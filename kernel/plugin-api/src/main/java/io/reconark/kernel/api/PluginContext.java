package io.reconark.kernel.api;

/** What the kernel gives a plugin while it registers. */
public interface PluginContext {

    /** The plugin's validated configuration from the composition. */
    PluginConfig config();

    /** Environment name from the composition, e.g. {@code local}, {@code test}, {@code prod}. */
    String environment();

    /**
     * Extensions already registered by plugins this one {@link PluginDescriptor#requires() requires}, e.g. the bound
     * {@code secret-provider}.
     */
    ExtensionLookup lookup();
}
