package com.localrag.common.ollama;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

public class OllamaChatClient {
    private final RestClient restClient;
    private final String model;

    public OllamaChatClient(String baseUrl, String model) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.model = model;
    }

    public String model() {
        return model;
    }

    public String chat(String systemPrompt, String userPrompt) {
        if (model == null || model.isBlank()) {
            throw new IllegalStateException("RAG_CHAT_MODEL must be configured to use local answer generation");
        }
        JsonNode response = restClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", model,
                        "stream", false,
                        "think", false,
                        "options", Map.of(
                                "num_predict", 512,
                                "temperature", 0.1
                        ),
                        "messages", List.of(
                                Map.of("role", "system", "content", systemPrompt),
                                Map.of("role", "user", "content", userPrompt)
                        )
                ))
                .retrieve()
                .body(JsonNode.class);
        String content = response == null ? null : response.path("message").path("content").asText(null);
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("Ollama chat response did not contain message.content");
        }
        return content.trim();
    }
}
