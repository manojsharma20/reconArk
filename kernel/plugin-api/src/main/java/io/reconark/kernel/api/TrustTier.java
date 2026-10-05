package io.reconark.kernel.api;

/** How much the platform trusts a plugin; decides where it may run (ADR-0035). */
public enum TrustTier {
    /** First-party, signed, runs in-process anywhere. */
    CORE,
    /** Third-party, signed and TCK-certified; runs in-process where the composition allow-lists it. */
    VERIFIED,
    /** Developer convenience (in-memory bus, env secrets); refused when the environment is {@code prod}. */
    DEV_ONLY,
    /** Untrusted or non-JVM code; only ever runs out-of-process behind the gRPC ExtensionService. */
    REMOTE
}
