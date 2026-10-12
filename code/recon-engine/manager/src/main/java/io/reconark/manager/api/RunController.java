package io.reconark.manager.api;

import io.reconark.manager.run.Job;
import io.reconark.manager.run.Run;
import io.reconark.manager.run.RunService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API to start and follow runs.
 *
 * <pre>
 * POST /api/runs            {"definitionId": "...", "leftFileId": "...", "rightFileId": "..."}  -> 202 + run
 * GET  /api/runs            latest 100 runs
 * GET  /api/runs/{id}       one run with its progress and counts
 * GET  /api/runs/{id}/jobs  the jobs of a run (for troubleshooting)
 * </pre>
 */
@RestController
@RequestMapping("/api/runs")
public class RunController {

    private final RunService runs;

    public RunController(RunService runs) {
        this.runs = runs;
    }

    public record StartRunRequest(@NotBlank String definitionId, @NotBlank String leftFileId, @NotBlank String rightFileId) {}

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Run start(@Valid @RequestBody StartRunRequest request) {
        return runs.startRun(request.definitionId(), request.leftFileId(), request.rightFileId());
    }

    @GetMapping
    public List<Run> latest() {
        return runs.latestRuns();
    }

    @GetMapping("/{id}")
    public Run get(@PathVariable UUID id) {
        return runs.getRun(id);
    }

    @GetMapping("/{id}/jobs")
    public List<Job> jobs(@PathVariable UUID id) {
        return runs.jobs(id);
    }
}
