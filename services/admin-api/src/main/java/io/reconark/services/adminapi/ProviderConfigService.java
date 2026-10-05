package io.reconark.services.adminapi;

import io.reconark.domain.engine.etl.EtlPipelines;
import io.reconark.domain.model.MaskingPolicy;
import io.reconark.kernel.api.ExtensionLookup;
import io.reconark.kernel.pipeline.PipelineDefinition;
import io.reconark.kernel.pipeline.PipelineEngine;
import io.reconark.kernel.pipeline.PipelineResult;
import io.reconark.kernel.pipeline.StageDefinition;
import io.reconark.kernel.pipeline.StagePayload;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Configuration-only onboarding: a provider version is valid when its stage graph compiles against the bricks that are
 * actually installed; it can be dry-run on a sample; it needs a different person to approve it.
 */
@Service
class ProviderConfigService {

    private final PipelineEngine engine;
    private final Map<String, List<ProviderVersion>> versions = new ConcurrentHashMap<>();

    ProviderConfigService(ExtensionLookup extensions) {
        this.engine = EtlPipelines.engine(extensions);
    }

    ProviderVersion createDraft(String providerCode, String description, List<StageDefinition> pipeline, String maker) {
        engine.compile(new PipelineDefinition(providerCode + "/draft", pipeline)); // RK-KRN-* -> 422
        List<ProviderVersion> list = versions.computeIfAbsent(providerCode, k -> new ArrayList<>());
        synchronized (list) {
            ProviderVersion v = new ProviderVersion(providerCode, list.size() + 1, description, List.copyOf(pipeline),
                    ProviderVersion.State.DRAFT, maker, null, Instant.now());
            list.add(v);
            return v;
        }
    }

    List<ProviderVersion> list(String providerCode) {
        return List.copyOf(versions.getOrDefault(providerCode, List.of()));
    }

    DryRunResult dryRun(String providerCode, int version, String sample) {
        ProviderVersion v = get(providerCode, version);
        PipelineResult r = engine.compile(new PipelineDefinition(providerCode + "/v" + version, v.pipeline()))
                .run("dry-run", Map.of("providerCode", providerCode),
                        new StagePayload.Bytes(() -> new ByteArrayInputStream(sample.getBytes(StandardCharsets.UTF_8))));
        List<DryRunResult.Reject> rejects = r.rejected().stream()
                .map(e -> new DryRunResult.Reject(e.position(), e.violations().stream()
                        .map(x -> x.code() + " " + x.field() + ": " + x.message()).toList()))
                .toList();
        List<Map<String, String>> preview = r.accepted().stream().limit(20)
                .map(e -> {
                    Map<String, String> m = new java.util.LinkedHashMap<>();
                    e.fields().forEach((k, val) -> m.put(k, k.startsWith("_") ? String.valueOf(val) : MaskingPolicy.mask(val)));
                    return m;
                })
                .toList();
        return new DryRunResult(r.read(), r.accepted().size(), r.rejected().size(), rejects, preview);
    }

    ProviderVersion submit(String providerCode, int version, String user) {
        return transition(providerCode, version, ProviderVersion.State.DRAFT, ProviderVersion.State.SUBMITTED, user, false);
    }

    ProviderVersion approve(String providerCode, int version, String checker) {
        return transition(providerCode, version, ProviderVersion.State.SUBMITTED, ProviderVersion.State.APPROVED, checker, true);
    }

    private ProviderVersion transition(
            String providerCode, int version, ProviderVersion.State from, ProviderVersion.State to, String user, boolean checker) {
        List<ProviderVersion> list = versions.getOrDefault(providerCode, List.of());
        synchronized (list) {
            ProviderVersion v = get(providerCode, version);
            if (v.state() != from) {
                throw new IllegalStateException("RK-CFG-0201 version " + version + " is " + v.state() + ", expected " + from);
            }
            if (checker && v.maker().equals(user)) {
                throw new IllegalStateException("RK-CFG-0202 maker and checker must be different people");
            }
            ProviderVersion next = v.with(to, checker ? user : null);
            list.set(version - 1, next);
            return next;
        }
    }

    private ProviderVersion get(String providerCode, int version) {
        List<ProviderVersion> list = versions.getOrDefault(providerCode, List.of());
        if (version < 1 || version > list.size()) {
            throw new IllegalArgumentException("RK-CFG-0101 unknown provider version " + providerCode + "/" + version);
        }
        return list.get(version - 1);
    }

    /** Masked dry-run summary. */
    record DryRunResult(long read, int accepted, int rejected, List<Reject> rejects, List<Map<String, String>> preview) {
        record Reject(long position, List<String> violations) {}
    }
}
