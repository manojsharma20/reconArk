package io.reconark.plugins.bus.inmemory;

import io.reconark.spi.BusMessage;
import io.reconark.spi.MessageBus;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Synchronous in-process bus: one delivery per consumer group, with bounded redelivery before the message lands on
 * {@code <topic>.dlq}. Mirrors the at-least-once contract closely enough for tests.
 */
final class InMemoryMessageBus implements MessageBus {

    private final int maxDeliveries;
    private final Map<String, Map<String, MessageBus.Handler>> groupsByTopic = new ConcurrentHashMap<>();
    private final List<BusMessage> deadLetters = new CopyOnWriteArrayList<>();

    InMemoryMessageBus(int maxDeliveries) {
        this.maxDeliveries = maxDeliveries;
    }

    @Override
    public void publish(BusMessage message) {
        groupsByTopic.getOrDefault(message.topic(), Map.of()).values().forEach(h -> deliver(h, message));
    }

    private void deliver(MessageBus.Handler handler, BusMessage message) {
        for (int attempt = 1; attempt <= maxDeliveries; attempt++) {
            try {
                handler.handle(message);
                return;
            } catch (Exception e) {
                if (attempt == maxDeliveries) {
                    deadLetters.add(new BusMessage(message.id(), message.topic() + ".dlq", message.key(), message.headers(),
                            message.payload()));
                }
            }
        }
    }

    @Override
    public AutoCloseable subscribe(String topic, String group, MessageBus.Handler handler) {
        groupsByTopic.computeIfAbsent(topic, t -> new ConcurrentHashMap<>()).put(group, handler);
        return () -> groupsByTopic.getOrDefault(topic, Map.of()).remove(group);
    }

    List<BusMessage> deadLetters() {
        return List.copyOf(deadLetters);
    }
}
