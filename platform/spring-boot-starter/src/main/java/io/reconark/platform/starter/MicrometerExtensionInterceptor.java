package io.reconark.platform.starter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.reconark.kernel.runtime.ExtensionInterceptor;
import org.springframework.beans.factory.ObjectProvider;

/** Records {@code reconark.extension.calls} (timer) tagged by point, key, plugin, method and outcome. */
final class MicrometerExtensionInterceptor implements ExtensionInterceptor {

    private final ObjectProvider<MeterRegistry> registry;

    MicrometerExtensionInterceptor(ObjectProvider<MeterRegistry> registry) {
        this.registry = registry;
    }

    @Override
    public Object around(Call call, Invocation next) throws Throwable {
        MeterRegistry r = registry.getIfAvailable();
        if (r == null) {
            return next.proceed();
        }
        Timer.Sample sample = Timer.start(r);
        String outcome = "success";
        try {
            return next.proceed();
        } catch (Throwable t) {
            outcome = "error";
            throw t;
        } finally {
            sample.stop(Timer.builder("reconark.extension.calls")
                    .tag("point", call.pointId())
                    .tag("key", call.key())
                    .tag("plugin", call.pluginId())
                    .tag("method", call.method())
                    .tag("outcome", outcome)
                    .register(r));
        }
    }
}
