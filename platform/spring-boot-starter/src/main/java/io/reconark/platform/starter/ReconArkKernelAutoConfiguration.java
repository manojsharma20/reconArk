package io.reconark.platform.starter;

import io.reconark.kernel.api.ExtensionLookup;
import io.reconark.kernel.api.ReconArkPlugin;
import io.reconark.kernel.runtime.CompositionConfig;
import io.reconark.kernel.runtime.ExtensionInterceptor;
import io.reconark.kernel.runtime.Kernel;
import io.reconark.kernel.runtime.PluginCatalog;
import io.reconark.kernel.runtime.PluginRuntime;
import io.reconark.spi.ExtensionPoints;
import io.reconark.spi.MessageBus;
import io.reconark.spi.ObjectStore;
import io.reconark.spi.SecretProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Boots the kernel once per service. A bad composition fails application startup with an {@code RK-KRN-*} error, so
 * Kubernetes keeps the previous ReplicaSet serving (HLD §12.2).
 */
@AutoConfiguration
@EnableConfigurationProperties(ReconArkCompositionProperties.class)
public class ReconArkKernelAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(ReconArkKernelAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public PluginCatalog reconArkPluginCatalog(ObjectProvider<ReconArkPlugin> pluginBeans) {
        PluginCatalog discovered = PluginCatalog.discover(Thread.currentThread().getContextClassLoader());
        return discovered.with(pluginBeans.orderedStream().toArray(ReconArkPlugin[]::new));
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public Kernel reconArkKernel(
            PluginCatalog catalog, ReconArkCompositionProperties properties, ObjectProvider<ExtensionInterceptor> interceptors) {
        CompositionConfig composition = CompositionConfig.fromMap(properties.toMap());
        Kernel kernel = new PluginRuntime(interceptors.orderedStream().toList()).boot(catalog, composition);
        kernel.inventory(catalog).forEach(p -> LOG.info("reconArk plugin {} {} [{}] {} -> {}",
                p.id(), p.version(), p.trustTier(), p.state(), p.contributions()));
        return kernel;
    }

    @Bean
    @ConditionalOnMissingBean
    public ExtensionLookup reconArkExtensions(Kernel kernel) {
        return kernel.extensions();
    }

    /** Convenience beans for the SINGLE platform points; lazy so services that do not need them can omit the plugin. */
    @Bean
    @Lazy
    @ConditionalOnMissingBean
    public MessageBus reconArkMessageBus(ExtensionLookup extensions) {
        return extensions.single(ExtensionPoints.MESSAGE_BUS);
    }

    @Bean
    @Lazy
    @ConditionalOnMissingBean
    public ObjectStore reconArkObjectStore(ExtensionLookup extensions) {
        return extensions.single(ExtensionPoints.OBJECT_STORE);
    }

    @Bean
    @Lazy
    @ConditionalOnMissingBean
    public SecretProvider reconArkSecretProvider(ExtensionLookup extensions) {
        return extensions.single(ExtensionPoints.SECRET_PROVIDER);
    }

    /** Times every extension call (HLD §14). */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    static class MetricsConfiguration {
        @Bean
        ExtensionInterceptor reconArkMetricsInterceptor(ObjectProvider<io.micrometer.core.instrument.MeterRegistry> registry) {
            return new MicrometerExtensionInterceptor(registry);
        }
    }

    /** Exposes {@code /actuator/plugins}. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    static class EndpointConfiguration {
        @Bean
        PluginsEndpoint reconArkPluginsEndpoint(Kernel kernel, PluginCatalog catalog) {
            return new PluginsEndpoint(kernel, catalog);
        }
    }
}
