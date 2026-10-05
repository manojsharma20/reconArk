package io.reconark.kernel.api;

import java.io.Serial;

/** A composition or lookup failure. Carries a stable {@link KernelError} code. */
public class KernelException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final KernelError error;

    public KernelException(KernelError error, String message) {
        super(error.code() + " " + message);
        this.error = error;
    }

    public KernelException(KernelError error, String message, Throwable cause) {
        super(error.code() + " " + message, cause);
        this.error = error;
    }

    public KernelError error() {
        return error;
    }
}
