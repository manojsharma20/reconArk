package io.reconark.services.adminapi;

import io.reconark.kernel.pipeline.ErrorPolicy;
import io.reconark.kernel.pipeline.StageDefinition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Provider configuration lifecycle: draft -> dry-run -> submit -> approve (maker-checker). */
@RestController
@RequestMapping("/api/admin/v1/providers/{providerCode}/versions")
class ProviderConfigController {

    private final ProviderConfigService service;

    ProviderConfigController(ProviderConfigService service) {
        this.service = service;
    }

    record StageRequest(@NotBlank String key, Map<String, Object> options, ErrorPolicy onError) {
        StageDefinition toDefinition() {
            return new StageDefinition(key, options, onError);
        }
    }

    record DraftRequest(@Size(max = 500) String description, @NotEmpty @Size(max = 50) List<@Valid StageRequest> pipeline) {}

    record DryRunRequest(@NotBlank @Size(max = 1_000_000) String sample) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ONBOARDING_MAKER')")
    ProviderVersion create(
            @PathVariable @Pattern(regexp = "[A-Z][A-Z0-9_]{1,31}") String providerCode,
            @Valid @RequestBody DraftRequest request,
            Principal principal) {
        return service.createDraft(providerCode, request.description(),
                request.pipeline().stream().map(StageRequest::toDefinition).toList(), principal.getName());
    }

    @GetMapping
    List<ProviderVersion> list(@PathVariable String providerCode) {
        return service.list(providerCode);
    }

    @PostMapping("/{version}/dry-run")
    @PreAuthorize("hasRole('ONBOARDING_MAKER')")
    ProviderConfigService.DryRunResult dryRun(
            @PathVariable String providerCode, @PathVariable int version, @Valid @RequestBody DryRunRequest request) {
        return service.dryRun(providerCode, version, request.sample());
    }

    @PostMapping("/{version}/submit")
    @PreAuthorize("hasRole('ONBOARDING_MAKER')")
    ProviderVersion submit(@PathVariable String providerCode, @PathVariable int version, Principal principal) {
        return service.submit(providerCode, version, principal.getName());
    }

    @PostMapping("/{version}/approve")
    @PreAuthorize("hasRole('ONBOARDING_CHECKER')")
    ProviderVersion approve(@PathVariable String providerCode, @PathVariable int version, Principal principal) {
        return service.approve(providerCode, version, principal.getName());
    }
}
