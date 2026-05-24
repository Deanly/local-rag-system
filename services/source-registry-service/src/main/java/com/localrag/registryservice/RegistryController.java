package com.localrag.registryservice;

import com.localrag.common.dto.HealthResponse;
import com.localrag.common.dto.RegistryDtos.ProjectDto;
import com.localrag.common.dto.RegistryDtos.RegistryValidationResponse;
import com.localrag.common.dto.RegistryDtos.ScopeResponse;
import com.localrag.common.dto.RegistryDtos.SourceDto;
import com.localrag.common.registry.ProjectRegistration;
import com.localrag.common.registry.RegistryMapper;
import com.localrag.common.registry.RegistrySynchronizer;
import com.localrag.common.registry.SourceRegistry;
import com.localrag.common.registry.SourceRegistryValidator;
import com.localrag.common.registry.SourceRoot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RegistryController {
    private final RegistryRepository repository;
    private final SourceRegistryValidator validator;
    private final RegistryConfig config;
    private final RegistrySynchronizer synchronizer;

    public RegistryController(
            RegistryRepository repository,
            SourceRegistryValidator validator,
            RegistryConfig config,
            RegistrySynchronizer synchronizer
    ) {
        this.repository = repository;
        this.validator = validator;
        this.config = config;
        this.synchronizer = synchronizer;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        SourceRegistry registry = repository.current();
        return HealthResponse.up("source-registry-service", Map.of(
                "projects", registry.projects().size(),
                "sources", registry.sources().size()
        ));
    }

    @GetMapping("/registry/projects")
    public List<ProjectDto> projects() {
        return repository.current().projects().stream().map(RegistryMapper::toDto).toList();
    }

    @GetMapping("/registry/sources")
    public List<SourceDto> sources() {
        return repository.current().sources().stream().map(RegistryMapper::toDto).toList();
    }

    @GetMapping("/registry/projects/{projectId}/scope")
    public ScopeResponse scope(@PathVariable("projectId") String projectId) {
        SourceRegistry registry = repository.current();
        ProjectRegistration project = registry.requireProject(projectId);
        List<String> activeSourceIds = registry.sources().stream()
                .filter(SourceRoot::active)
                .map(SourceRoot::sourceId)
                .toList();
        LinkedHashSet<String> sourceIds = new LinkedHashSet<>();
        sourceIds.add(project.primarySourceId());
        sourceIds.addAll(project.defaultContext());
        for (SourceRoot source : registry.activeSourcesFor(projectId).stream()
                .sorted((a, b) -> Integer.compare(b.priority(), a.priority()))
                .toList()) {
            sourceIds.add(source.sourceId());
        }
        sourceIds.retainAll(activeSourceIds);
        return new ScopeResponse(projectId, new ArrayList<>(sourceIds));
    }

    @PostMapping("/registry/validate")
    public RegistryValidationResponse validateRegistry() {
        SourceRegistry registry = repository.current();
        List<String> errors = validator.validate(registry, config.requirePaths());
        return new RegistryValidationResponse(errors.isEmpty(), errors, registry.projects().size(), registry.sources().size());
    }

    @PostMapping("/registry/reload")
    public RegistryValidationResponse reload() {
        SourceRegistry registry = repository.reload();
        List<String> errors = validator.validate(registry, config.requirePaths());
        if (errors.isEmpty()) {
            synchronizer.synchronize(registry);
        }
        return new RegistryValidationResponse(errors.isEmpty(), errors, registry.projects().size(), registry.sources().size());
    }
}
