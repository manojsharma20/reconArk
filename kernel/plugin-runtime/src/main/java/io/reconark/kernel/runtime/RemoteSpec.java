package io.reconark.kernel.runtime;

import java.time.Duration;
import java.util.Objects;

/**
 * Where an out-of-process plugin lives (gRPC {@code ExtensionService}, contracts/proto/extension_service.proto).
 *
 * @param endpoint gRPC target, e.g. {@code dns:///plugin-iso20022:9443}
 * @param timeout per-call deadline
 * @param maxBatch maximum records per call
 */
public record RemoteSpec(String endpoint, Duration timeout, int maxBatch) {

    public RemoteSpec {
        Objects.requireNonNull(endpoint, "endpoint");
        timeout = timeout == null ? Duration.ofSeconds(2) : timeout;
        maxBatch = maxBatch <= 0 ? 500 : maxBatch;
    }
}
