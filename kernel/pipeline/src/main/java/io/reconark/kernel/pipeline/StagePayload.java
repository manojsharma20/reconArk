package io.reconark.kernel.pipeline;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

/** What flows between stages: raw bytes before parsing, records after. */
public sealed interface StagePayload permits StagePayload.Bytes, StagePayload.Records {

    /**
     * A byte stream (e.g. an encrypted file before decoding).
     *
     * @param source opens the stream; stages wrap it rather than buffer it (constant memory)
     */
    record Bytes(IoSupplier<InputStream> source) implements StagePayload {
        public Bytes {
            Objects.requireNonNull(source, "source");
        }
    }

    /**
     * Parsed records.
     *
     * @param records the records still in flight
     */
    record Records(List<RecordEnvelope> records) implements StagePayload {
        public Records {
            records = List.copyOf(records);
        }
    }
}
