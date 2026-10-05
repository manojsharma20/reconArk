package io.reconark.plugins.secrets.env;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.reconark.kernel.api.ReconArkPlugin;
import io.reconark.kernel.runtime.CompositionConfig;
import io.reconark.kernel.runtime.PluginCatalog;
import io.reconark.kernel.runtime.PluginRuntime;
import io.reconark.spi.ExtensionPoints;
import io.reconark.spi.SecretValue;
import io.reconark.testing.tck.PluginContractTck;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EnvSecretsPluginTest extends PluginContractTck {

    @Override
    protected ReconArkPlugin plugin() {
        return new EnvSecretsPlugin();
    }

    @Test
    void resolvesReferenceNamesAndNeverPrintsValues() {
        var plugin = new EnvSecretsPlugin(name -> "RECONARK_SECRET_DEV_RECONARK_ACQUIRER_A_PGP".equals(name) ? "pa55" : null);
        var k = new PluginRuntime().boot(PluginCatalog.of(plugin),
                CompositionConfig.fromMap(Map.of("plugins", Map.of("secrets-env", Map.of()))));
        try (SecretValue v = k.extensions().single(ExtensionPoints.SECRET_PROVIDER).resolve("dev/reconark/acquirer-a/pgp")) {
            assertThat(new String(v.chars())).isEqualTo("pa55");
            assertThat(v.toString()).doesNotContain("pa55");
        }
        assertThatThrownBy(() -> k.extensions().single(ExtensionPoints.SECRET_PROVIDER).resolve("missing"))
                .hasMessageNotContaining("RECONARK_SECRET_");
    }

    @Test
    void refusedInProduction() {
        assertThatThrownBy(() -> new PluginRuntime().boot(PluginCatalog.of(new EnvSecretsPlugin()),
                CompositionConfig.fromMap(Map.of("environment", "prod", "plugins", Map.of("secrets-env", Map.of())))))
                .hasMessageStartingWith("RK-KRN-0004");
    }
}
