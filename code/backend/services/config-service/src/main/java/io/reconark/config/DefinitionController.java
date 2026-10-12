package io.reconark.config;

import io.reconark.common.model.ReconDefinition;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Recon definitions.
 *
 * <pre>
 * GET    /api/definitions          all definitions
 * GET    /api/definitions/{id}     one definition
 * POST   /api/definitions          create (the id is generated)          -> 201, or 400 with the list of problems
 * PUT    /api/definitions/{id}     replace                                -> 200, 400 or 404
 * DELETE /api/definitions/{id}                                            -> 204 or 404
 * POST   /api/definitions/validate check without saving                  -> {"valid": true|false, "problems": [...]}
 * </pre>
 * Changing a definition never changes a run that already started: each run keeps a copy.
 */
@RestController
@RequestMapping("/api/definitions")
public class DefinitionController {

    private final DefinitionRepository repository;
    private final DefinitionValidator validator;

    public DefinitionController(DefinitionRepository repository, DefinitionValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    @GetMapping
    public List<ReconDefinition> all() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ReconDefinition one(@PathVariable String id) {
        return repository.findById(id).orElseThrow(() -> notFound(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReconDefinition create(@RequestBody ReconDefinition request) {
        ReconDefinition definition = request.withId(UUID.randomUUID().toString());
        checkValid(definition);
        repository.insert(definition);
        return definition;
    }

    @PutMapping("/{id}")
    public ReconDefinition update(@PathVariable String id, @RequestBody ReconDefinition request) {
        ReconDefinition definition = request.withId(id);
        checkValid(definition);
        if (!repository.update(definition)) {
            throw notFound(id);
        }
        return definition;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!repository.delete(id)) {
            throw notFound(id);
        }
        return ResponseEntity.noContent().build();
    }

    public record ValidationResult(boolean valid, List<String> problems) {}

    @PostMapping("/validate")
    public ValidationResult validate(@RequestBody ReconDefinition request) {
        List<String> problems = validator.problems(request);
        return new ValidationResult(problems.isEmpty(), problems);
    }

    private void checkValid(ReconDefinition definition) {
        List<String> problems = new java.util.ArrayList<>(validator.problems(definition));
        if (definition.name() != null && repository.nameTakenByAnother(definition.name(), definition.id())) {
            problems.add("another definition is already called '" + definition.name() + "'");
        }
        if (!problems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join("; ", problems));
        }
    }

    private static ResponseStatusException notFound(String id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Recon definition not found: " + id);
    }
}
