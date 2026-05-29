package com.localrag.common.ollama;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OllamaChatClient {
    private final List<Endpoint> endpoints;
    private final String model;

    public OllamaChatClient(String baseUrl, String model) {
        this(baseUrl, model,
                OllamaEndpointConfig.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                OllamaEndpointConfig.DEFAULT_READ_TIMEOUT_MILLIS);
    }

    public OllamaChatClient(String baseUrls, String model, long connectTimeoutMillis, long readTimeoutMillis) {
        this.endpoints = OllamaEndpointConfig.parseBaseUrls(baseUrls).stream()
                .map(baseUrl -> new Endpoint(baseUrl, OllamaEndpointConfig.restClient(baseUrl, connectTimeoutMillis, readTimeoutMillis)))
                .toList();
        this.model = model;
    }

    public String model() {
        return model;
    }

    public String chat(String systemPrompt, String userPrompt) {
        if (model == null || model.isBlank()) {
            throw new IllegalStateException("RAG_CHAT_MODEL must be configured to use local answer generation");
        }
        List<RuntimeException> failures = new ArrayList<>();
        for (Endpoint endpoint : endpoints) {
            try {
                JsonNode response = endpoint.restClient().post()
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
            } catch (RuntimeException exception) {
                failures.add(new IllegalStateException("Ollama chat failed at " + endpoint.baseUrl(), exception));
            }
        }
        String urls = endpoints.stream().map(Endpoint::baseUrl).collect(Collectors.joining(", "));
        IllegalStateException exception = new IllegalStateException("Ollama chat failed for all configured endpoints: " + urls);
        failures.forEach(exception::addSuppressed);
        throw exception;
    }

    private record Endpoint(String baseUrl, RestClient restClient) {
    }
}
