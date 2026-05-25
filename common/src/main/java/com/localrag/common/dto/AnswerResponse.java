package com.localrag.common.dto;

import java.util.List;

public record AnswerResponse(
        String projectId,
        String query,
        String mode,
        String model,
        String answer,
        List<String> sourcesSearched,
        List<String> citations,
        List<SearchResultItem> results
) {
}
