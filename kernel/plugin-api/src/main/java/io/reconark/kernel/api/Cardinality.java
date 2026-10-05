package io.reconark.kernel.api;

/** How many implementations of an extension point may be active at once. */
public enum Cardinality {
    /** Exactly one active implementation, chosen by a binding (e.g. the message bus). */
    SINGLE,
    /** Many active implementations, each selected by key from business configuration (e.g. format readers). */
    KEYED,
    /** An ordered list of implementations applied in sequence (e.g. audit sinks). */
    CHAIN
}
