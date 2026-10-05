package io.reconark.domain.engine;

import static org.assertj.core.api.Assertions.assertThat;

import io.reconark.domain.engine.etl.EtlPipelines;
import io.reconark.domain.engine.recon.CompareRule;
import io.reconark.domain.engine.recon.MatchKeySpec;
import io.reconark.domain.engine.recon.Normalization;
import io.reconark.domain.engine.recon.ReconEngine;
import io.reconark.domain.engine.recon.ReconOutcome;
import io.reconark.domain.engine.recon.ReconRuleSet;
import io.reconark.domain.model.CanonicalRecord;
import io.reconark.kernel.pipeline.PipelineDefinition;
import io.reconark.kernel.pipeline.PipelineResult;
import io.reconark.kernel.pipeline.StageDefinition;
import io.reconark.kernel.pipeline.StagePayload;
import io.reconark.kernel.runtime.CompositionConfig;
import io.reconark.kernel.runtime.Kernel;
import io.reconark.kernel.runtime.PluginCatalog;
import io.reconark.kernel.runtime.PluginRuntime;
import io.reconark.spi.FieldDiff;
import io.reconark.spi.OutcomeStatus;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** A provider onboarded purely by configuration: CSV in, canonical records out, reconciled against the core. */
class EndToEndCompositionTest {

    private static Kernel kernel() {
        return new PluginRuntime().boot(PluginCatalog.discover(EndToEndCompositionTest.class.getClassLoader()),
                CompositionConfig.fromMap(Map.of("plugins", Map.of(
                        "core-stages", Map.of(), "format-delimited", Map.of(), "recon-standard", Map.of()))));
    }

    private static final String CSV = "﻿ref,amount,date,status\n"
            + "TX1,100.00,2026-10-01,00\n"
            + "TX2,abc,2026-10-01,00\n"
            + "\"TX,3\",50.50,2026-10-02,05\n"
            + ",1.00,2026-10-01,00\n";

    @Test
    void configurationOnlyProviderIngestsAndReconciles() {
        Kernel k = kernel();
        PipelineDefinition etl = new PipelineDefinition("acquirer-a/v1", List.of(
                StageDefinition.of("parse", Map.of("format", "delimited")),
                StageDefinition.of("map", Map.of("fields", Map.of(
                        "partnerRef", Map.of("source", "ref", "required", true),
                        "amount", Map.of("source", "amount", "type", "decimal", "required", true),
                        "txDate", Map.of("source", "date", "type", "date"),
                        "status", Map.of("source", "status")))),
                StageDefinition.of("validate", Map.of("rules", List.of(Map.of("validator", "required", "options", Map.of("fields", List.of("status")))))),
                StageDefinition.of("canonicalize", Map.of("record-key-field", "partnerRef", "business-date-field", "txDate"))));

        PipelineResult r = EtlPipelines.engine(k.extensions()).compile(etl).run("run-1", Map.of(),
                new StagePayload.Bytes(() -> new ByteArrayInputStream(CSV.getBytes(StandardCharsets.UTF_8))));

        assertThat(r.read()).isEqualTo(4);
        assertThat(r.accepted()).hasSize(2);
        assertThat(r.rejected()).extracting(e -> e.violations().getFirst().code()).containsExactlyInAnyOrder("RK-MAP-0002", "RK-MAP-0001");

        List<CanonicalRecord> partner = r.accepted().stream()
                .map(e -> new CanonicalRecord("ACQUIRER_A", e.fields().get("_record_key").toString(),
                        (LocalDate) e.fields().get("_business_date"), e.fields()))
                .toList();
        List<CanonicalRecord> core = List.of(
                new CanonicalRecord("PAYMENTS_CORE", "1", LocalDate.parse("2026-10-01"), Map.of("txnRef", " tx1", "amt", "100.004", "status", "SUCCESS")),
                new CanonicalRecord("PAYMENTS_CORE", "2", LocalDate.parse("2026-10-02"), Map.of("txnRef", "TX,3", "amt", "50.00", "status", "SUCCESS")),
                new CanonicalRecord("PAYMENTS_CORE", "3", LocalDate.parse("2026-10-02"), Map.of("txnRef", "TX9", "amt", "1", "status", "SUCCESS")));

        ReconRuleSet rs = new ReconRuleSet("RS-ACQ-A", "PAYMENTS_CORE", "ACQUIRER_A",
                new MatchKeySpec(List.of("txnRef"), List.of("partnerRef"), List.of(Normalization.TRIM, Normalization.UPPER_CASE)),
                "one-to-one", Map.of(),
                List.of(new CompareRule("amt", "amount", "numeric-tolerance", Map.of("scale", 2), FieldDiff.Severity.BLOCKING, false),
                        new CompareRule("status", "status", "value-map", Map.of("mapping", Map.of("00", "SUCCESS")), FieldDiff.Severity.BLOCKING, false)));

        List<ReconOutcome> outcomes = new ReconEngine(k.extensions()).compile(rs).reconcile(core, partner);

        assertThat(outcomes).extracting(ReconOutcome::matchKey, ReconOutcome::status).containsExactly(
                org.assertj.core.groups.Tuple.tuple("TX1", OutcomeStatus.MATCHED),
                org.assertj.core.groups.Tuple.tuple("TX,3", OutcomeStatus.MISMATCHED),
                org.assertj.core.groups.Tuple.tuple("TX9", OutcomeStatus.UNMATCHED_LEFT));
        assertThat(outcomes.get(1).diffs()).hasSize(2);
    }

    @Test
    void sensitiveDiffsAreMasked() {
        Kernel k = kernel();
        ReconRuleSet rs = new ReconRuleSet("RS", "L", "R",
                new MatchKeySpec(List.of("id"), List.of("id"), List.of()), "one-to-one", Map.of(),
                List.of(new CompareRule("iban", "iban", "exact", Map.of(), FieldDiff.Severity.BLOCKING, true)));
        var out = new ReconEngine(k.extensions()).compile(rs).reconcile(
                List.of(new CanonicalRecord("L", "1", LocalDate.now(), Map.of("id", "1", "iban", "AE070331234567891234"))),
                List.of(new CanonicalRecord("R", "1", LocalDate.now(), Map.of("id", "1", "iban", "AE070331234567899999"))));
        assertThat(out.getFirst().diffs().getFirst().explanation()).doesNotContain("0331234567891234").contains("1234").contains("9999");
    }
}
