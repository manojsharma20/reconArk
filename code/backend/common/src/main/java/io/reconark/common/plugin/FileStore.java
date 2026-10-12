package io.reconark.common.plugin;

import java.io.IOException;
import java.io.InputStream;

/** Where uploaded files are kept: a shared folder locally, object storage (S3, Azure Blob, GCS) in the cloud. */
public interface FileStore extends Plugin {

    /**
     * Stores the content and returns a storage key that {@link #open(String)} understands.
     *
     * @param fileName the original file name (used only to make the key readable)
     */
    String save(String fileName, InputStream content) throws IOException;

    /** Opens a stored file for reading. The caller closes the stream. */
    InputStream open(String storageKey) throws IOException;
}
