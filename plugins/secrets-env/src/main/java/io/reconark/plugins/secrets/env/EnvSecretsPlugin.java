package io.reconark.plugins.secrets.env;

import io.reconark.kernel.api.ConfigSpec;
import io.reconark.kernel.api.ExtensionRegistrar;
import io.reconark.kernel.api.PluginContext;
import io.reconark.kernel.api.PluginDescriptor;
import io.reconark.kernel.api.PropertySpec;
import io.reconark.kernel.api.PropertyType;
import io.reconark.kernel.api.ReconArkPlugin;
import io.reconark.kernel.api.TrustTier;
import io.reconark.spi.ExtensionPoints;
import io.reconark.spi.SecretProvider;
import io.reconark.spi.SecretValue;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * Contributes {@code secret-provider:env}. Reference {@code dev/reconark/acquirer-a/pgp} resolves to the environment
 * variable {@code RECONARK_SECRET_DEV_RECONARK_ACQUIRER_A_PGP}.
 */
public final class EnvSecretsPlugin implements ReconArkPlugin {

    private static final PluginDescriptor DESCRIPTOR = PluginDescriptor.builder("secrets-env", "0.2.0")
            .name("Environment secrets")
            .description("Development secret provider reading environment variables")
            .trustTier(TrustTier.DEV_ONLY)
            .provides(ExtensionPoints.SECRET_PROVIDER)
            .config(ConfigSpec.of(PropertySpec.optional("prefix", PropertyType.STRING, "RECONARK_SECRET_", "variable prefix")))
            .build();

    private final UnaryOperator<String> env;

    public EnvSecretsPlugin() {
        this(System::getenv);
    }

    EnvSecretsPlugin(UnaryOperator<String> env) {
        this.env = env;
    }

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void register(ExtensionRegistrar registrar, PluginContext context) {
        String prefix = context.config().string("prefix");
        SecretProvider provider = reference -> {
            String name = prefix + reference.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "_");
            String v = env.apply(name);
            if (v == null) {
                throw new IllegalArgumentException("Unknown secret reference '" + reference + "'");
            }
            return new SecretValue(v.toCharArray());
        };
        registrar.contribute(ExtensionPoints.SECRET_PROVIDER, "env", provider);
    }
}
