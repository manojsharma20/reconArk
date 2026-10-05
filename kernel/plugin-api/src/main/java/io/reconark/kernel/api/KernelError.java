package io.reconark.kernel.api;

/** Stable kernel error codes ({@code RK-KRN-NNNN}); documented in docs/lld/reconArk-LLD.md §2.6. */
public enum KernelError {
    UNKNOWN_PLUGIN("RK-KRN-0001"),
    DUPLICATE_PLUGIN("RK-KRN-0002"),
    INCOMPATIBLE_KERNEL_API("RK-KRN-0003"),
    TRUST_TIER_NOT_ALLOWED("RK-KRN-0004"),
    INVALID_CONFIG("RK-KRN-0005"),
    UNMET_REQUIREMENT("RK-KRN-0006"),
    DEPENDENCY_CYCLE("RK-KRN-0007"),
    MISSING_BINDING("RK-KRN-0008"),
    UNKNOWN_BINDING_KEY("RK-KRN-0009"),
    DUPLICATE_CONTRIBUTION("RK-KRN-0010"),
    TYPE_MISMATCH("RK-KRN-0011"),
    NO_SUCH_EXTENSION("RK-KRN-0012"),
    REGISTRATION_FAILED("RK-KRN-0013"),
    UNDECLARED_CONTRIBUTION("RK-KRN-0014");

    private final String code;

    KernelError(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
