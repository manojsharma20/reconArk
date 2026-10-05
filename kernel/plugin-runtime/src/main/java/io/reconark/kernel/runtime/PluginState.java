package io.reconark.kernel.runtime;

/** Lifecycle state of a plugin in a running kernel. */
public enum PluginState {
    DISCOVERED,
    VALIDATED,
    REGISTERED,
    ACTIVE,
    DISABLED,
    FAILED,
    STOPPED
}
