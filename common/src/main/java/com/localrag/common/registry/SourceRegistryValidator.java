package com.localrag.common.registry;

import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class SourceRegistryValidator {
    private static final Pattern PROJECT_ID = Pattern.compile("[a-z0-9][a-z0-9-]*");
    private static final Pattern SOURCE_ID = Pattern.compile("[a-z0-9][a-z0-9-]*(\\.[a-z0-9][a-z0-9-]*)*");

    public List<String> validate(SourceRegistry registry, boolean requirePaths) {
        List<String> errors = new ArrayList<>();
        if (registry.version() != 1) {
            errors.add("Unsupported registry version: " + registry.version());
        }
        if (registry.deviceId() == null || registry.deviceId().isBlank()) {
            errors.add("device_id is required");
        }

        Set<String> projectIds = new HashSet<>();
        for (ProjectRegistration project : registry.projects()) {
            if (!PROJECT_ID.matcher(project.projectId()).matches()) {
                errors.add("Invalid project_id: " + project.projectId());
            }
            if (!projectIds.add(project.projectId())) {
                errors.add("Duplicate project_id: " + project.projectId());
            }
        }

        Set<String> sourceIds = new HashSet<>();
        for (SourceRoot source : registry.sources()) {
            if (!SOURCE_ID.matcher(source.sourceId()).matches()) {
                errors.add("Invalid source_id: " + source.sourceId());
            }
            if (!sourceIds.add(source.sourceId())) {
                errors.add("Duplicate source_id: " + source.sourceId());
            }
            if (!projectIds.contains(source.projectId())) {
                errors.add("Source references missing project_id: " + source.sourceId());
            }
            if (!source.path().isAbsolute()) {
                errors.add("Source path must be absolute: " + source.sourceId());
            }
            if (requirePaths && source.active() && (!Files.exists(source.path()) || !Files.isReadable(source.path()))) {
                errors.add("Active source path is not readable: " + source.sourceId() + " -> " + source.path());
            }
            if (source.writePolicy() == null || source.writePolicy().isBlank()) {
                errors.add("write_policy is required: " + source.sourceId());
            }
            validateGlobPatterns(errors, source.sourceId(), "include", source.include());
            validateGlobPatterns(errors, source.sourceId(), "exclude", source.exclude());
        }

        for (ProjectRegistration project : registry.projects()) {
            if (!sourceIds.contains(project.primarySourceId())) {
                errors.add("Project primary_source_id does not exist: " + project.projectId());
            }
            for (String sourceId : project.defaultContext()) {
                if (!sourceIds.contains(sourceId)) {
                    errors.add("Project default_context source does not exist: " + project.projectId() + " -> " + sourceId);
                }
            }
        }
        return List.copyOf(errors);
    }

    private static void validateGlobPatterns(List<String> errors, String sourceId, String field, List<String> patterns) {
        for (String pattern : patterns) {
            if (pattern == null || pattern.isBlank()) {
                errors.add("Blank " + field + " glob: " + sourceId);
                continue;
            }
            try {
                FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            } catch (IllegalArgumentException exception) {
                errors.add("Invalid " + field + " glob: " + sourceId + " -> " + pattern);
            }
        }
    }
}
