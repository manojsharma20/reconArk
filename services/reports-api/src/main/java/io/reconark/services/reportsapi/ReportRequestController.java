package io.reconark.services.reportsapi;

import io.reconark.kernel.api.ExtensionLookup;
import io.reconark.spi.BusMessage;
import io.reconark.spi.ExtensionPoints;
import io.reconark.spi.MessageBus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Asynchronous reports (ADR-0019). A request is accepted, published as {@code reconark.reporting.report-requested.v1}
 * and rendered by report-worker with the chosen {@code report-renderer}. In production the event goes through the
 * outbox in the same transaction as the {@code report_execution} row.
 */
@RestController
@RequestMapping("/api/reports/v1")
class ReportRequestController {

    static final String TOPIC = "reconark.reporting.report-requested.v1";

    private final ExtensionLookup extensions;
    private final MessageBus bus;

    ReportRequestController(ExtensionLookup extensions, MessageBus bus) {
        this.extensions = extensions;
        this.bus = bus;
    }

    /** Output formats available in this environment = active report-renderer keys. */
    @GetMapping("/formats")
    Set<String> formats() {
        return extensions.keys(ExtensionPoints.REPORT_RENDERER);
    }

    record ReportRequest(@NotBlank @Pattern(regexp = "[a-z0-9-]{1,64}") String definition, @NotBlank String format) {}

    record Accepted(String requestId, String status) {}

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('REPORT_VIEWER')")
    Accepted request(@Valid @RequestBody ReportRequest request, Principal principal) {
        extensions.keyed(ExtensionPoints.REPORT_RENDERER, request.format()); // unknown format -> RK-KRN-0012 -> 422
        String id = UUID.randomUUID().toString();
        String body = "{\"requestId\":\"" + id + "\",\"definition\":\"" + request.definition() + "\",\"format\":\""
                + request.format() + "\"}";
        bus.publish(new BusMessage(id, TOPIC, principal.getName(), Map.of(), body.getBytes(StandardCharsets.UTF_8)));
        return new Accepted(id, "QUEUED");
    }
}
