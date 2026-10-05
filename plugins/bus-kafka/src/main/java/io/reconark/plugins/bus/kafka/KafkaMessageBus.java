package io.reconark.plugins.bus.kafka;

import io.reconark.kernel.api.PluginConfig;
import io.reconark.spi.BusMessage;
import io.reconark.spi.MessageBus;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * Kafka implementation of {@link MessageBus}. One virtual thread per subscription polls and commits after the handler
 * succeeded (at-least-once). Failed messages are retried {@code max-deliveries} times, then sent to {@code <topic>.dlq}.
 */
final class KafkaMessageBus implements MessageBus, AutoCloseable {

    private static final String MESSAGE_ID = "reconark-message-id";

    private final PluginConfig config;
    private final KafkaProducer<String, byte[]> producer;
    private final List<AutoCloseable> subscriptions = new ArrayList<>();

    KafkaMessageBus(PluginConfig config) {
        this.config = config;
        Properties p = base();
        p.put(ProducerConfig.ACKS_CONFIG, "all");
        p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        this.producer = new KafkaProducer<>(p);
    }

    private Properties base() {
        Properties p = new Properties();
        p.put("bootstrap.servers", config.string("bootstrap-servers"));
        p.put("security.protocol", config.string("security-protocol"));
        p.put("client.id", config.string("client-id"));
        return p;
    }

    @Override
    public void publish(BusMessage message) {
        ProducerRecord<String, byte[]> rec = new ProducerRecord<>(message.topic(), message.key(), message.payload());
        rec.headers().add(MESSAGE_ID, message.id().getBytes(StandardCharsets.UTF_8));
        message.headers().forEach((k, v) -> rec.headers().add(k, v.getBytes(StandardCharsets.UTF_8)));
        try {
            producer.send(rec).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing to " + message.topic(), e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Publish to " + message.topic() + " failed", e.getCause());
        }
    }

    @Override
    public AutoCloseable subscribe(String topic, String group, MessageBus.Handler handler) {
        Properties p = base();
        p.put(ConsumerConfig.GROUP_ID_CONFIG, group);
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(config.integer("max-poll-records")));
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        AtomicBoolean running = new AtomicBoolean(true);
        Thread worker = Thread.ofVirtual().name("reconark-kafka-" + topic + "-" + group).start(() -> {
            try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(p)) {
                consumer.subscribe(List.of(topic));
                while (running.get()) {
                    for (ConsumerRecord<String, byte[]> r : consumer.poll(Duration.ofMillis(500))) {
                        deliver(handler, toMessage(r));
                    }
                    consumer.commitSync();
                }
            }
        });
        AutoCloseable handle = () -> {
            running.set(false);
            worker.join(Duration.ofSeconds(10));
        };
        subscriptions.add(handle);
        return handle;
    }

    private void deliver(MessageBus.Handler handler, BusMessage m) {
        int max = config.integer("max-deliveries");
        for (int attempt = 1; attempt <= max; attempt++) {
            try {
                handler.handle(m);
                return;
            } catch (Exception e) {
                if (attempt == max) {
                    publish(new BusMessage(m.id(), m.topic() + ".dlq", m.key(), m.headers(), m.payload()));
                }
            }
        }
    }

    private static BusMessage toMessage(ConsumerRecord<String, byte[]> r) {
        Map<String, String> headers = new HashMap<>();
        String id = r.topic() + "-" + r.partition() + "-" + r.offset();
        for (Header h : r.headers()) {
            String v = new String(h.value(), StandardCharsets.UTF_8);
            if (MESSAGE_ID.equals(h.key())) {
                id = v;
            } else {
                headers.put(h.key(), v);
            }
        }
        return new BusMessage(id, r.topic(), r.key(), headers, r.value());
    }

    @Override
    public void close() throws Exception {
        for (AutoCloseable s : subscriptions) {
            s.close();
        }
        producer.close(Duration.ofSeconds(10));
    }
}
