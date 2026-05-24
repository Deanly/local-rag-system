package com.localrag.common.dto;

import java.util.List;

public final class RegistryDtos {
    private RegistryDtos() {
    }

    public record ProjectDto(
            String projectId,
            String displayName,
            String repoPath,
            String primarySourceId,
            List<String> defaultContext,
            boolean active
    ) {
    }

    public record SourceDto(
            String sourceId,
            String projectId,
            String type,
            String ssotRole,
            String path,
            int priority,
            boolean active,
            String sensitivityDefault,
            List<String> include,
            List<String> exclude,
            String readPolicy,
            String writePolicy
    ) {
    }

    public record RegistryValidationResponse(
            boolean valid,
            List<String> errors,
            int projects,
            int sources
    ) {
    }

    public record ScopeResponse(
            String projectId,
            List<String> sourceIds
    ) {
    }
}
