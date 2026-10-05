package io.reconark.kernel.api;

/** Handed to {@link ReconArkPlugin#register}; the only way a plugin contributes implementations. */
public interface ExtensionRegistrar {

    /**
     * Contributes an implementation of {@code point} under {@code key}.
     *
     * @param point the extension point; must be listed in the descriptor's {@code provides}
     * @param key the key business configuration uses to select it, e.g. {@code delimited}
     * @param implementation the implementation
     * @param <T> contract type
     */
    <T> void contribute(ExtensionPoint<T> point, String key, T implementation);
}
