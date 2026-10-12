package io.reconark.ingestion;

import io.reconark.common.model.Side;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * File uploads.
 *
 * <pre>
 * POST /api/files   multipart form: file, definitionId, side (LEFT or RIGHT)    -> 201 + file details
 * GET  /api/files?definitionId=...&side=LEFT                                    latest uploads (filters optional)
 * GET  /api/files/{id}                                                           one upload
 * </pre>
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final UploadService uploads;
    private final FileRepository repository;

    public FileController(UploadService uploads, FileRepository repository) {
        this.uploads = uploads;
        this.repository = repository;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadedFile upload(@RequestParam("file") MultipartFile file, @RequestParam String definitionId,
            @RequestParam Side side) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file is empty");
        }
        if (definitionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "definitionId is required");
        }
        return uploads.upload(definitionId, side, file.getOriginalFilename(), file.getSize(), file.getInputStream());
    }

    @GetMapping
    public List<UploadedFile> list(@RequestParam(required = false) String definitionId,
            @RequestParam(required = false) Side side) {
        return repository.find(definitionId, side);
    }

    @GetMapping("/{id}")
    public UploadedFile one(@PathVariable String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + id));
    }
}
