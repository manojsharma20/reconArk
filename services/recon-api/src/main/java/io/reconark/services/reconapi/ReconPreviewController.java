package io.reconark.services.reconapi;

import io.reconark.domain.engine.recon.ReconEngine;
import io.reconark.domain.engine.recon.ReconOutcome;
import io.reconark.domain.engine.recon.ReconRuleSet;
import io.reconark.domain.model.CanonicalRecord;
import io.reconark.kernel.api.ExtensionLookup;
import io.reconark.spi.ExtensionPoints;
import io.reconark.spi.FieldComparator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rule-set preview: evaluates a draft rule set on sample records with exactly the engine production uses. Results,
 * diffs and the exception workflow are served from replicas by the query adapters (LLD §8.3).
 */
@RestController
@RequestMapping("/api/recon/v1")
class ReconPreviewController {

    private final ExtensionLookup extensions;
    private final ReconEngine engine;

    ReconPreviewController(ExtensionLookup extensions) {
        this.extensions = extensions;
        this.engine = new ReconEngine(extensions);
    }

    record PreviewRequest(
            @NotNull ReconRuleSet ruleSet,
            @NotNull @Size(max = 1000) List<CanonicalRecord> left,
            @NotNull @Size(max = 1000) List<CanonicalRecord> right) {}

    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('RECON_ANALYST','ONBOARDING_MAKER')")
    List<ReconOutcome> preview(@RequestBody PreviewRequest request) {
        return engine.compile(request.ruleSet()).reconcile(request.left(), request.right());
    }

    /** Active comparators with their parameter schemas (rule designer palette). */
    @GetMapping("/comparators")
    List<ComparatorView> comparators() {
        return extensions.keys(ExtensionPoints.FIELD_COMPARATOR).stream()
                .map(k -> {
                    FieldComparator c = extensions.keyed(ExtensionPoints.FIELD_COMPARATOR, k);
                    return new ComparatorView(k, c.parameters().toJsonSchema(k));
                })
                .toList();
    }

    record ComparatorView(String key, String parameterSchema) {}
}
