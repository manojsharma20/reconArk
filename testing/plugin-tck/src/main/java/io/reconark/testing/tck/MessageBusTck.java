package io.reconark.testing.tck;

import static org.assertj.core.api.Assertions.assertThat;

import io.reconark.spi.BusMessage;
import io.reconark.spi.MessageBus;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Contract of {@code message-bus}: delivery, per-key ordering, redelivery, unsubscribe. */
public abstract class MessageBusTck {

    protected abstract MessageBus bus();

    /** How long to wait for asynchronous delivery. */
    protected Duration patience() {
        return Duration.ofSeconds(10);
    }

    private static BusMessage msg(String topic, String key, String body) {
        return new BusMessage(UUID.randomUUID().toString(), topic, key, Map.of("traceparent", "00-x-y-01"),
                body.getBytes(StandardCharsets.UTF_8));
    }

    private void await(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + patience().toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
    }

    @Test
    void deliversWithHeadersInKeyOrder() throws Exception {
        String topic = "tck." + UUID.randomUUID();
        List<String> got = new CopyOnWriteArrayList<>();
        try (AutoCloseable sub = bus().subscribe(topic, "g1", m -> {
            assertThat(m.headers()).containsKey("traceparent");
            got.add(new String(m.payload(), StandardCharsets.UTF_8));
        })) {
            for (int i = 0; i < 10; i++) {
                bus().publish(msg(topic, "k", "m" + i));
            }
            await(() -> got.size() == 10);
        }
        assertThat(got).containsExactly("m0", "m1", "m2", "m3", "m4", "m5", "m6", "m7", "m8", "m9");
    }

    @Test
    void redeliversAfterFailure() throws Exception {
        String topic = "tck." + UUID.randomUUID();
        AtomicInteger attempts = new AtomicInteger();
        try (AutoCloseable sub = bus().subscribe(topic, "g1", m -> {
            if (attempts.incrementAndGet() == 1) {
                throw new IllegalStateException("transient");
            }
        })) {
            bus().publish(msg(topic, "k", "x"));
            await(() -> attempts.get() >= 2);
        }
        assertThat(attempts.get()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void unsubscribeStopsDelivery() throws Exception {
        String topic = "tck." + UUID.randomUUID();
        AtomicInteger count = new AtomicInteger();
        AutoCloseable sub = bus().subscribe(topic, "g1", m -> count.incrementAndGet());
        sub.close();
        bus().publish(msg(topic, "k", "x"));
        Thread.sleep(200);
        assertThat(count.get()).isZero();
    }
}
