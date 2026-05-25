package com.localrag.retrieval;

import com.localrag.common.dto.SearchRequest;
import com.localrag.common.embedding.EmbeddingClient;
import com.localrag.common.ollama.OllamaChatClient;
import com.localrag.common.weaviate.WeaviateClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RetrievalServiceTests {
    private final EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
    private final OllamaChatClient ollamaChatClient = mock(OllamaChatClient.class);
    private final WeaviateClient weaviateClient = mock(WeaviateClient.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final RetrievalService retrievalService = new RetrievalService(
            embeddingClient,
            ollamaChatClient,
            weaviateClient,
            jdbcTemplate
    );

    @Test
    void rejectsUnknownProjectBeforeSearchExecution() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("missing-project"))).thenReturn(0);

        assertThatThrownBy(() -> retrievalService.search(new SearchRequest(
                "missing-project",
                "query",
                5,
                "hybrid",
                null,
                null,
                null
        )))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).contains("Unknown projectId");
                });

        verifyNoInteractions(embeddingClient, weaviateClient);
    }

    @Test
    void rejectsUnsupportedModeBeforeSearchExecution() {
        assertThatThrownBy(() -> retrievalService.search(new SearchRequest(
                null,
                "query",
                5,
                "semantic-only",
                null,
                null,
                null
        )))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).contains("Unsupported search mode");
                });

        verifyNoInteractions(embeddingClient, weaviateClient);
    }
}
