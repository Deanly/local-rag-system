package com.localrag.retrieval;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalErrorHandlerTests {
    @Test
    void answerGenerationDisabledIsAStableNonRetryableContract() {
        var response = new RetrievalErrorHandler().answerGenerationDisabled();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody())
                .containsEntry("code", "ANSWER_GENERATION_DISABLED")
                .containsEntry("recommendedTool", "rag_search")
                .containsEntry("retryable", false);
        assertThat(response.getBody().get("message").toString()).contains("authorized caller");
    }
}
