package com.localrag.common.registry;

import java.util.List;

public record SourceRegistry(
        int version,
        String deviceId,
        String defaultProjectId,
        List<ProjectRegistration> projects,
        List<SourceRoot> sources
) {
    public ProjectRegistration requireProject(String projectId) {
        return projects.stream()
                .filter(project -> project.projectId().equals(projectId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown project_id: " + projectId));
    }

    public List<SourceRoot> activeSourcesFor(String projectId) {
        return sources.stream()
                .filter(SourceRoot::active)
                .filter(source -> source.projectId().equals(projectId))
                .toList();
    }
}
