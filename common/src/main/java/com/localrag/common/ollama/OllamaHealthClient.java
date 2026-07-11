package com.localrag.common.ollama;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OllamaHealthClient {
    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";

    private final List<Endpoint> endpoints;
    private final String embeddingModel;
    private final String chatModel;

    public OllamaHealthClient(String baseUrls, String embeddingModel, String chatModel, long connectTimeoutMillis, long healthTimeoutMillis) {
        this.endpoints = OllamaEndpointConfig.parseBaseUrls(baseUrls).stream()
                .map(baseUrl -> new Endpoint(baseUrl, OllamaEndpointConfig.restClient(baseUrl, connectTimeoutMillis, healthTimeoutMillis)))
                .toList();
        this.embeddingModel = normalize(embeddingModel);
        this.chatModel = normalize(chatModel);
    }

    public Map<String, Object> health() {
        List<Map<String, Object>> endpointResults = endpoints.stream().map(this::health).toList();
        boolean up = endpointResults.stream().anyMatch(result -> STATUS_UP.equals(result.get("status")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", up ? STATUS_UP : STATUS_DOWN);
        result.put("embeddingModel", embeddingModel);
        result.put("chatModelRequired", !chatModel.isBlank());
        if (!chatModel.isBlank()) {
            result.put("chatModel", chatModel);
        }
        result.put("endpoints", endpointResults);
        return result;
    }

    private Map<String, Object> health(Endpoint endpoint) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("baseUrl", endpoint.baseUrl());
        try {
            JsonNode response = endpoint.restClient().get()
                    .uri("/api/tags")
                    .retrieve()
                    .body(JsonNode.class);
            Set<String> models = modelNames(response);
            boolean embeddingModelAvailable = embeddingModel.isBlank() || models.contains(embeddingModel);
            boolean chatModelAvailable = chatModel.isBlank() || models.contains(chatModel);
            List<String> missingModels = new ArrayList<>();
            if (!embeddingModelAvailable) {
                missingModels.add(embeddingModel);
            }
            if (!chatModelAvailable) {
                missingModels.add(chatModel);
            }

            result.put("status", missingModels.isEmpty() ? STATUS_UP : STATUS_DOWN);
            result.put("reachable", true);
            result.put("modelCount", models.size());
            result.put("embeddingModelAvailable", embeddingModelAvailable);
            if (!chatModel.isBlank()) {
                result.put("chatModelAvailable", chatModelAvailable);
            }
            if (!missingModels.isEmpty()) {
                result.put("missingModels", missingModels);
            }
        } catch (RuntimeException exception) {
            result.put("status", STATUS_DOWN);
            result.put("reachable", false);
            result.put("error", conciseError(exception));
        }
        return result;
    }

    private static Set<String> modelNames(JsonNode response) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        JsonNode models = response == null ? null : response.get("models");
        if (models != null && models.isArray()) {
            for (JsonNode model : models) {
                addIfPresent(names, model.path("name").asText(""));
                addIfPresent(names, model.path("model").asText(""));
            }
        }
        return names;
    }

    private static void addIfPresent(Set<String> names, String name) {
        String normalized = normalize(name);
        if (!normalized.isBlank()) {
            names.add(normalized);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String conciseError(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        String error = root.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
        return error.length() > 220 ? error.substring(0, 217) + "..." : error;
    }

    private record Endpoint(String baseUrl, RestClient restClient) {
    }
}
