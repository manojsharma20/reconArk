package io.reconark.kernel.pipeline;

import java.io.IOException;

/** A supplier that may throw {@link IOException}; used to open streams lazily and more than once. */
@FunctionalInterface
public interface IoSupplier<T> {
    T get() throws IOException;
}
