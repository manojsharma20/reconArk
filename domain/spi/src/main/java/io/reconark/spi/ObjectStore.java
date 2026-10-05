package io.reconark.spi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/** Raw artifacts and reports (S3, Blob Storage, GCS, filesystem for development). */
public interface ObjectStore {

    /** Stores a stream under {@code key} and returns its SHA-256 (hex). */
    String put(String key, InputStream content, Map<String, String> metadata) throws IOException;

    /** Opens the whole object. */
    InputStream open(String key) throws IOException;

    /** Opens {@code [offset, offset + length)}; chunks read byte ranges in parallel. */
    InputStream openRange(String key, long offset, long length) throws IOException;

    boolean exists(String key) throws IOException;
}
