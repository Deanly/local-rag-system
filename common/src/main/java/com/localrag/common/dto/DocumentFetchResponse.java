package com.localrag.common.dto;

public record DocumentFetchResponse(
        String sourceId,
        String projectId,
        String relativePath,
        String citation,
        int bytesRead,
        boolean truncated,
        String content
) {
}
