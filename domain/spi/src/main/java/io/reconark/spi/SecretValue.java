package io.reconark.spi;

import java.util.Arrays;

/** A secret held as characters and zeroed on close. {@link #toString()} never reveals it. */
public final class SecretValue implements AutoCloseable {

    private final char[] value;

    public SecretValue(char[] value) {
        this.value = value.clone();
    }

    /** A copy of the secret; callers should zero it after use. */
    public char[] chars() {
        return value.clone();
    }

    @Override
    public void close() {
        Arrays.fill(value, '\0');
    }

    @Override
    public String toString() {
        return "SecretValue[****]";
    }
}
