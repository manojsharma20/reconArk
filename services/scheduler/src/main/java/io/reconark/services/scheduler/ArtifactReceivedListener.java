package io.reconark.services.scheduler;

import io.reconark.spi.BusMessage;
import io.reconark.spi.MessageBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Plans a run for a received artifact (dedupe, chunk plan, outbox) — LLD §8.4.
 *
 * <p>Subscribes through whichever {@code message-bus} brick the composition binds (Kafka, RabbitMQ, ActiveMQ,
 * in-memory). Messages are triggers; the database claim is the source of truth for work (ADR-0009).
 */
@Component
class ArtifactReceivedListener implements SmartLifecycle {

    static final String TOPIC = "reconark.ingestion.artifact-received.v1";
    private static final Logger LOG = LoggerFactory.getLogger(ArtifactReceivedListener.class);

    private final MessageBus bus;
    private AutoCloseable subscription;

    ArtifactReceivedListener(MessageBus bus) {
        this.bus = bus;
    }

    void handle(BusMessage message) {
        LOG.info("received {} id={} key={}", message.topic(), message.id(), message.key());
    }

    @Override
    public void start() {
        subscription = bus.subscribe(TOPIC, "scheduler", this::handle);
    }

    @Override
    public void stop() {
        try {
            if (subscription != null) {
                subscription.close();
            }
        } catch (Exception e) {
            LOG.warn("Error closing subscription to {}", TOPIC, e);
        } finally {
            subscription = null;
        }
    }

    @Override
    public boolean isRunning() {
        return subscription != null;
    }
}
