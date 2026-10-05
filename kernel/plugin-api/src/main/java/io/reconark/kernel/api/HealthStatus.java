package io.reconark.kernel.api;

/**
 * Plugin health reported to readiness checks.
 *
 * @param state the state
 * @param detail short, non-sensitive explanation
 */
public record HealthStatus(State state, String detail) {

    public static final HealthStatus UP = new HealthStatus(State.UP, "");

    /** Health states. */
    public enum State { UP, DEGRADED, DOWN }

    public static HealthStatus down(String detail) {
        return new HealthStatus(State.DOWN, detail);
    }
}
