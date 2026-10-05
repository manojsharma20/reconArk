package io.reconark.spi;

import io.reconark.kernel.api.PluginConfig;
import java.io.IOException;
import java.io.InputStream;

/** Decrypts or decompresses a stream (PGP, gzip, zip). Streaming only: constant memory regardless of size. */
public interface PayloadDecoder {

    /**
     * @param in encoded stream
     * @param options decoder options (e.g. secret reference of the private key)
     * @param secrets resolves secret references
     */
    InputStream decode(InputStream in, PluginConfig options, SecretProvider secrets) throws IOException;
}
