package com.localrag.common.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchRequestTests {
    @Test
    void normalizesLegacyBm25ModeToKeyword() {
        SearchRequest request = new SearchRequest(null, "query", 5, "bm25", null, null, null);

        assertEquals("keyword", request.effectiveMode());
    }

    @Test
    void defaultsBlankModeToHybrid() {
        SearchRequest request = new SearchRequest(null, "query", 5, " ", null, null, null);

        assertEquals("hybrid", request.effectiveMode());
    }
}
