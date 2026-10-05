package io.reconark.kernel.api;

/** Version of the kernel API that plugins compile against. Bump MAJOR on any incompatible SPI change. */
public final class KernelApi {

    /** The kernel API version implemented by this build. */
    public static final SemVer VERSION = SemVer.parse("1.0.0");

    private KernelApi() {}
}
