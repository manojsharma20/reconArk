package io.reconark.services.adminapi;

import static org.assertj.core.api.Assertions.assertThat;

import io.reconark.kernel.api.ExtensionLookup;
import io.reconark.kernel.pipeline.StageDefinition;
import io.reconark.spi.ExtensionPoints;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Boots the service with its local composition and exercises configuration-only onboarding. */
@SpringBootTest(properties = {"reconark.security.dev-mode=true", "reconark.composition.environment=test"})
class AdminApiApplicationTest {

    @Autowired
    ExtensionLookup extensions;

    @Autowired
    ProviderConfigService providers;

    @Test
    void composedBricksAreActive() {
        assertThat(extensions.keys(ExtensionPoints.FORMAT_READER)).contains("delimited");
        assertThat(extensions.keys(ExtensionPoints.FIELD_COMPARATOR)).contains("numeric-tolerance");
        assertThat(extensions.keys(ExtensionPoints.MESSAGE_BUS)).isEmpty(); // carried but not enabled: Lego
    }

    @Test
    void draftDryRunAndMakerChecker() {
        var v = providers.createDraft("ACQUIRER_A", "first version", List.of(
                StageDefinition.of("parse", Map.of("format", "delimited")),
                StageDefinition.of("map", Map.of("fields", Map.of("ref", Map.of("source", "ref", "required", true))))), "alice");
        var dry = providers.dryRun("ACQUIRER_A", v.version(), "ref\nA1\n\n");
        assertThat(dry.accepted()).isEqualTo(1);
        providers.submit("ACQUIRER_A", v.version(), "alice");
        assertThat(providers.approve("ACQUIRER_A", v.version(), "bob").state()).isEqualTo(ProviderVersion.State.APPROVED);
    }
}
