package io.reconark.spi;

import io.reconark.kernel.api.PluginConfig;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * How data arrives: SFTP, object storage, REST, SOAP, gRPC, queues. Retry, timeouts, SSRF protection and metrics live
 * once in the kernel interceptors and the connector base, not in each connector.
 */
public interface SourceConnector {

    /**
     * Lists new items since the checkpoint.
     *
     * @param options provider connector options (host, path pattern, secret references ...)
     * @param checkpoint opaque checkpoint from the previous poll, or {@code null}
     */
    Poll poll(PluginConfig options, String checkpoint) throws IOException;

    /** Opens one item for streaming into the object store. */
    InputStream open(PluginConfig options, Item item) throws IOException;

    /**
     * One item to acquire.
     *
     * @param name remote name (normalised; never used directly as a local path)
     * @param size size in bytes, or -1
     */
    record Item(String name, long size) {}

    /**
     * A poll result.
     *
     * @param items items to acquire
     * @param nextCheckpoint checkpoint to persist after the items are stored
     */
    record Poll(List<Item> items, String nextCheckpoint) {
        public Poll {
            items = List.copyOf(items);
        }
    }
}
