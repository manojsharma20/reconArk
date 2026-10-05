package io.reconark.kernel.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.reconark.kernel.api.ExtensionRegistrar;
import io.reconark.kernel.api.PluginConfig;
import io.reconark.kernel.api.PluginContext;
import io.reconark.kernel.api.PluginDescriptor;
import io.reconark.kernel.api.ReconArkPlugin;
import io.reconark.kernel.runtime.CompositionConfig;
import io.reconark.kernel.runtime.ConfigValidator;
import io.reconark.kernel.runtime.Kernel;
import io.reconark.kernel.runtime.PluginCatalog;
import io.reconark.kernel.runtime.PluginRuntime;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PipelineEngineTest {

    /** lines -> records; "bad" rejected by 'check'; 'boom' throws. */
    static final class Stages implements ReconArkPlugin {
        @Override
        public PluginDescriptor descriptor() {
            return PluginDescriptor.builder("test-stages", "1.0.0").description("t").provides(PipelineStage.POINT).build();
        }

        @Override
        public void register(ExtensionRegistrar r, PluginContext c) {
            r.contribute(PipelineStage.POINT, "lines", (ctx, in) -> {
                String text;
                try (var s = ((StagePayload.Bytes) in).source().get()) {
                    text = new String(s.readAllBytes(), StandardCharsets.UTF_8);
                }
                List<RecordEnvelope> out = new ArrayList<>();
                long i = 0;
                for (String line : text.split("\n")) {
                    out.add(new RecordEnvelope(++i, Map.of("v", line)));
                }
                return new StagePayload.Records(out);
            });
            r.contribute(PipelineStage.POINT, "check", (ctx, in) -> {
                for (RecordEnvelope e : ((StagePayload.Records) in).records()) {
                    if ("bad".equals(e.fields().get("v"))) {
                        e.reject(new Violation("RK-T-1", "v", "bad value"));
                    }
                }
                return in;
            });
            r.contribute(PipelineStage.POINT, "boom", (ctx, in) -> {
                throw new IllegalStateException("checksum mismatch");
            });
        }
    }

    private static PipelineEngine engine() {
        Kernel k = new PluginRuntime().boot(PluginCatalog.of(new Stages()),
                CompositionConfig.fromMap(Map.of("plugins", Map.of("test-stages", Map.of()))));
        return new PipelineEngine(k.extensions(), (key, stage) -> raw -> ConfigValidator.validate(key, stage.options(), raw));
    }

    private static StagePayload bytes(String s) {
        return new StagePayload.Bytes(() -> new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void stagesRunInConfiguredOrderAndAccountingBalances() {
        var p = engine().compile(new PipelineDefinition("t", List.of(StageDefinition.of("lines", Map.of()), StageDefinition.of("check", Map.of()))));
        PipelineResult r = p.run("run-1", Map.of(), bytes("a\nbad\nc"));
        assertThat(r.read()).isEqualTo(3);
        assertThat(r.accepted()).extracting(e -> e.fields().get("v")).containsExactly("a", "c");
        assertThat(r.rejected()).hasSize(1);
        assertThat(r.rejected().getFirst().violations().getFirst().code()).isEqualTo("RK-T-1");
    }

    @Test
    void unknownStageFailsAtCompileTimeNotRunTime() {
        assertThatThrownBy(() -> engine().compile(new PipelineDefinition("t", List.of(StageDefinition.of("nope", Map.of())))))
                .hasMessageStartingWith("RK-KRN-0012");
    }

    @Test
    void quarantinePolicyHoldsTheWholeUnit() {
        var p = engine().compile(new PipelineDefinition("t", List.of(
                StageDefinition.of("lines", Map.of()),
                new StageDefinition("boom", Map.of(), ErrorPolicy.QUARANTINE_SOURCE))));
        PipelineResult r = p.run("run-1", Map.of(), bytes("a\nb"));
        assertThat(r.quarantined()).hasSize(2);
        assertThat(r.accepted()).isEmpty();
        assertThat(r.quarantineReason()).hasValueSatisfying(s -> assertThat(s).contains("boom"));
    }

    @Test
    void failRunPolicyThrows() {
        var p = engine().compile(new PipelineDefinition("t", List.of(
                StageDefinition.of("lines", Map.of()), new StageDefinition("boom", Map.of(), ErrorPolicy.FAIL_RUN))));
        assertThatThrownBy(() -> p.run("run-1", Map.of(), bytes("a"))).isInstanceOf(PipelineFailure.class);
    }

    @Test
    void stageOptionsAreValidated() {
        PluginConfig none = PluginConfig.EMPTY;
        assertThat(none.values()).isEmpty();
        assertThatThrownBy(() -> engine().compile(new PipelineDefinition("t", List.of(StageDefinition.of("lines", Map.of("x", 1))))))
                .hasMessageStartingWith("RK-KRN-0005");
    }
}
