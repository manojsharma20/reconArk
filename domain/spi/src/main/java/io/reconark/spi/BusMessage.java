package io.reconark.spi;

import java.util.Map;
import java.util.Objects;

/**
 * A broker-neutral message. Payloads are CloudEvents-compatible JSON produced by the outbox.
 *
 * @param id unique message id (consumers dedupe on it)
 * @param topic logical topic, e.g. {@code reconark.recon.bucket-ready.v1}
 * @param key ordering key
 * @param headers headers, including W3C trace context
 * @param payload body bytes
 */
public record BusMessage(String id, String topic, String key, Map<String, String> headers, byte[] payload) {
    public BusMessage {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(topic, "topic");
        headers = Map.copyOf(headers == null ? Map.of() : headers);
        payload = payload == null ? new byte[0] : payload.clone();
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }
}
