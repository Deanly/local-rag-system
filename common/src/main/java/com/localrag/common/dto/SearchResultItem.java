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
        Map<String, Object> metadata,
        Map<String, Object> score
) {
    public SearchResultItem {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
