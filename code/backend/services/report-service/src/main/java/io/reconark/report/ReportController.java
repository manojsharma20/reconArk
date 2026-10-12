package io.reconark.report;

import io.reconark.common.model.MatchStatus;
import io.reconark.common.plugin.PluginInfo;
import io.reconark.common.plugin.PluginRegistry;
import io.reconark.common.plugin.ReportWriter;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Reports on runs.
 *
 * <pre>
 * GET /api/reports/runs/{runId}/summary                               counts and match rate
 * GET /api/reports/runs/{runId}/results?status=MISMATCHED&page=0&size=50   one page of results (status optional)
 * GET /api/reports/runs/{runId}/export?format=csv&status=...          download all results (streamed)
 * GET /api/reports/formats                                            available download formats (report plugins)
 * </pre>
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ResultRepository results;
    private final PluginRegistry<ReportWriter> writers;

    public ReportController(ResultRepository results, PluginRegistry<ReportWriter> writers) {
        this.results = results;
        this.writers = writers;
    }

    @GetMapping("/runs/{runId}/summary")
    public RunSummary summary(@PathVariable UUID runId) {
        return findRun(runId);
    }

    @GetMapping("/runs/{runId}/results")
    public ResultPage results(@PathVariable UUID runId,
            @RequestParam(required = false) MatchStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        findRun(runId);
        if (page < 0 || size < 1 || size > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0 and size between 1 and 500");
        }
        return results.page(runId, status, page, size);
    }

    @GetMapping("/runs/{runId}/export")
    public ResponseEntity<StreamingResponseBody> export(@PathVariable UUID runId,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) MatchStatus status) {
        RunSummary run = findRun(runId);
        if (!"COMPLETED".equals(run.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Run " + runId + " is " + run.status() + ", not COMPLETED");
        }
        if (!writers.contains(format)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown report format '" + format + "'");
        }
        ReportWriter writer = writers.get(format);
        String fileName = "recon-" + runId + (status == null ? "" : "-" + status.name().toLowerCase()) + "." + writer.fileExtension();

        StreamingResponseBody body = out -> results.stream(runId, status, rows -> writer.write(run.fields(), rows, out));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(writer.contentType()))
                .body(body);
    }

    @GetMapping("/formats")
    public List<PluginInfo> formats() {
        return writers.all().stream().map(w -> PluginInfo.of("report", w)).toList();
    }

    private RunSummary findRun(UUID runId) {
        return results.summary(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found: " + runId));
    }
}
