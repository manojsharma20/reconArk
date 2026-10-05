package io.reconark.services.outboxrelay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls committed {@code platform.outbox_event} rows in commit order, publishes them through the bound
 * {@code message-bus} brick and marks them published (ADR-0007). The persistence adapter is added in the walking
 * skeleton increment (LLD §8.9); this placeholder only proves the service boots with its composition.
 */
@Component
class OutboxPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxPublisher.class);

    @Scheduled(fixedDelayString = "${reconark.outbox.poll-interval:PT1S}")
    void poll() {
        LOG.trace("outbox poll");
    }
}
