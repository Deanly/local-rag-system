package com.localrag.common.registry;

import com.localrag.common.dto.RegistryDtos.ProjectDto;
import com.localrag.common.dto.RegistryDtos.SourceDto;

public final class RegistryMapper {
    private RegistryMapper() {
    }

    public static ProjectDto toDto(ProjectRegistration project) {
        return new ProjectDto(
                project.projectId(),
                project.displayName(),
                project.repoPath(),
                project.primarySourceId(),
                project.defaultContext(),
                project.active()
        );
    }

    public static SourceDto toDto(SourceRoot source) {
        return new SourceDto(
                source.sourceId(),
                source.projectId(),
                source.type(),
                source.ssotRole(),
                source.path().toString(),
                source.priority(),
                source.active(),
                source.sensitivityDefault(),
                source.include(),
                source.exclude(),
                source.readPolicy(),
                source.writePolicy()
        );
    }
}
