package io.reconark.spi;

/**
 * Exactly one broker is active (ADR-0007): Kafka, RabbitMQ, ActiveMQ — or in-memory for development. Delivery is
 * at-least-once with ordering per key; consumers must be idempotent.
 */
public interface MessageBus {

    /** Publishes and returns once the broker acknowledged the message. */
    void publish(BusMessage message);

    /**
     * Subscribes a consumer group to a topic.
     *
     * @return a handle that stops the subscription when closed
     */
    AutoCloseable subscribe(String topic, String group, Handler handler);

    /** Consumer callback; throwing triggers bounded redelivery, then the DLQ. */
    @FunctionalInterface
    interface Handler {
        void handle(BusMessage message) throws Exception;
    }
}
