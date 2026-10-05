package io.reconark.services.ingestionapi;

import io.reconark.spi.BusMessage;
import io.reconark.spi.MessageBus;
import io.reconark.spi.ObjectStore;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Partner push (build prompt §13). Stores the payload as an immutable raw artifact, then requests a run. Signature,
 * nonce and timestamp checks run in a filter before this controller in production (architecture §10.1).
 */
@RestController
@RequestMapping("/api/ingestion/v1/sources/{sourceId}/batches")
class BatchIngestionController {

    static final String TOPIC = "reconark.ingestion.artifact-received.v1";
    private static final int MAX_BYTES = 50 * 1024 * 1024;

    private final ObjectStore store;
    private final MessageBus bus;
    private final Map<String, Receipt> idempotency = new ConcurrentHashMap<>();

    BatchIngestionController(ObjectStore store, MessageBus bus) {
        this.store = store;
        this.bus = bus;
    }

    record Receipt(String artifactKey, String sha256, String status) {}

    @PostMapping(consumes = {"text/csv", "application/json", "application/octet-stream"})
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('PARTNER_' + #sourceId)")
    Receipt push(
            @PathVariable String sourceId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody byte[] body) throws IOException {
        if (!sourceId.matches("[A-Z][A-Z0-9_]{1,31}") || !idempotencyKey.matches("[A-Za-z0-9-]{8,64}")) {
            throw new IllegalArgumentException("RK-ING-0001 invalid source id or Idempotency-Key");
        }
        if (body.length > MAX_BYTES) {
            throw new IllegalArgumentException("RK-ING-0002 payload exceeds " + MAX_BYTES + " bytes");
        }
        Receipt previous = idempotency.get(sourceId + "/" + idempotencyKey);
        if (previous != null) {
            return previous;
        }
        String key = "raw/" + sourceId + "/" + LocalDate.now() + "/" + UUID.randomUUID();
        String sha = store.put(key, new ByteArrayInputStream(body), Map.of("source", sourceId));
        String event = "{\"artifactKey\":\"" + key + "\",\"sha256\":\"" + sha + "\",\"sourceId\":\"" + sourceId + "\"}";
        bus.publish(new BusMessage(UUID.randomUUID().toString(), TOPIC, sourceId, Map.of(), event.getBytes(StandardCharsets.UTF_8)));
        Receipt receipt = new Receipt(key, sha, "ACCEPTED");
        idempotency.putIfAbsent(sourceId + "/" + idempotencyKey, receipt);
        return receipt;
    }
}
