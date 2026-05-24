package com.localrag.common.dto;

import java.util.List;

public record SearchResponse(
        String projectId,
        String query,
        String mode,
        List<String> sourcesSearched,
        List<SearchResultItem> results
) {
}
