package com.localrag.common.dto;

public record DocumentFetchRequest(
        String sourceId,
        String relativePath,
        Integer maxBytes
) {
    private static final int DEFAULT_MAX_BYTES = 20_000;
    private static final int MIN_MAX_BYTES = 1_000;
    private static final int MAX_MAX_BYTES = 200_000;

    public int effectiveMaxBytes() {
        if (maxBytes == null || maxBytes <= 0) {
            return DEFAULT_MAX_BYTES;
        }
        return Math.max(MIN_MAX_BYTES, Math.min(maxBytes, MAX_MAX_BYTES));
    }
}
