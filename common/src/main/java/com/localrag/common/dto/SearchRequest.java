package com.localrag.common.dto;

import java.util.List;
import java.util.Map;

public record SearchRequest(
        String projectId,
        String query,
        Integer limit,
        String mode,
        List<String> includeSourceIds,
        List<String> excludeSourceIds,
        Map<String, List<String>> filters
) {
    public int effectiveLimit() {
        if (limit == null || limit <= 0) {
            return 10;
        }
        return Math.min(limit, 50);
    }

    public String effectiveMode() {
        if (mode == null || mode.isBlank()) {
            return "hybrid";
        }
        return mode;
    }
}
