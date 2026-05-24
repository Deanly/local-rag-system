package com.localrag.common.registry;

import java.nio.file.Path;
import java.util.List;

public record SourceRoot(
        String sourceId,
        String projectId,
        String type,
        String ssotRole,
        Path path,
        int priority,
        boolean active,
        String sensitivityDefault,
        List<String> include,
        List<String> exclude,
        String readPolicy,
        String writePolicy
) {
}
