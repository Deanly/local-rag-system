package com.localrag.common.registry;

import java.util.List;

public record ProjectRegistration(
        String projectId,
        String displayName,
        String repoPath,
        String primarySourceId,
        List<String> defaultContext,
        boolean active
) {
}
