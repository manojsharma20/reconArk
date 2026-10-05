package io.reconark.spi;

import io.reconark.kernel.api.ConfigSpec;
import io.reconark.kernel.api.PluginConfig;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Parses one format into raw records (delimited, JSON, XML, fixed-width, Excel, Avro, ...). Mapping, validation and
 * rejection are never reimplemented per format — a reader only turns bytes into field maps.
 */
public interface FormatReader {

    /** Options this reader accepts (delimiter, header, encoding, record root ...). */
    default ConfigSpec options() {
        return ConfigSpec.NONE;
    }

    /**
     * Streams records to {@code sink}.
     *
     * @param in decoded input
     * @param options validated reader options
     * @param sink receives each record with its 1-based position in the source
     */
    void read(InputStream in, PluginConfig options, Sink sink) throws IOException;

    /** Receives parsed records. */
    @FunctionalInterface
    interface Sink {
        void accept(long position, Map<String, Object> rawFields);
    }
}
