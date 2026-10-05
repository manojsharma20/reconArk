package io.reconark.services.reconworker;

import io.reconark.spi.BusMessage;
import io.reconark.spi.MessageBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Evaluates one recon bucket (claim, LSN fence, candidate read, engine, outcome write) — LLD §8.7.
 *
 * <p>Subscribes through whichever {@code message-bus} brick the composition binds (Kafka, RabbitMQ, ActiveMQ,
 * in-memory). Messages are triggers; the database claim is the source of truth for work (ADR-0009).
 */
@Component
class ReconBucketListener implements SmartLifecycle {

    static final String TOPIC = "reconark.recon.bucket-ready.v1";
    private static final Logger LOG = LoggerFactory.getLogger(ReconBucketListener.class);

    private final MessageBus bus;
    private AutoCloseable subscription;

    ReconBucketListener(MessageBus bus) {
        this.bus = bus;
    }

    void handle(BusMessage message) {
        LOG.info("received {} id={} key={}", message.topic(), message.id(), message.key());
    }

    @Override
    public void start() {
        subscription = bus.subscribe(TOPIC, "recon-worker", this::handle);
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
