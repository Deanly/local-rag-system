package com.localrag.common.dto;

import java.util.Map;

public record SearchResultItem(
        String chunkId,
        String documentId,
        String projectId,
        String sourceId,
        String ssotRole,
        String relativePath,
        String headingPath,
        String citation,
        String snippet,
        Map<String, Object> score
) {
}
