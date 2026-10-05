package io.reconark.kernel.runtime;

/**
 * Cross-cutting behaviour applied once to every extension call (metrics, tracing, timeouts, masking, audit), so that
 * plugins never re-implement it. Interceptors are ordered; the first one is outermost.
 */
@FunctionalInterface
public interface ExtensionInterceptor {

    /**
     * Wraps one call.
     *
     * @param call what is being invoked
     * @param next proceeds to the next interceptor or the implementation
     * @return the call result
     * @throws Throwable whatever the implementation throws
     */
    Object around(Call call, Invocation next) throws Throwable;

    /**
     * Describes one extension call.
     *
     * @param pointId extension point id
     * @param key extension key
     * @param pluginId contributing plugin
     * @param method method name
     */
    record Call(String pointId, String key, String pluginId, String method) {}

    /** Proceeds with the call. */
    @FunctionalInterface
    interface Invocation {
        Object proceed() throws Throwable;
    }
}
