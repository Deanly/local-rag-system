package com.localrag.retrieval;

import com.fasterxml.jackson.databind.JsonNode;
import com.localrag.common.ollama.OllamaEndpointConfig;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class HttpLocalRerankerClient implements LocalRerankerClient {
    private final boolean enabled;
    private final String baseUrl;
    private final RestClient restClient;

    HttpLocalRerankerClient(RetrievalSettings settings) {
        this.enabled = settings.rerankerEnabled() && settings.rerankerBaseUrl() != null && !settings.rerankerBaseUrl().isBlank();
        this.baseUrl = settings.rerankerBaseUrl();
        this.restClient = enabled
                ? OllamaEndpointConfig.restClient(
                settings.rerankerBaseUrl(),
                settings.rerankerConnectTimeoutMillis(),
                settings.rerankerReadTimeoutMillis())
                : null;
    }

    @Override
    public RerankOutcome rerank(String query, List<CandidateText> candidates) {
        if (!enabled) {
            return RerankOutcome.disabled("reranker-not-enabled-or-base-url-missing");
        }
        if (candidates == null || candidates.isEmpty()) {
            return RerankOutcome.success(null, Map.of(), 0);
        }
        Instant started = Instant.now();
        try {
            JsonNode response = restClient.post()
                    .uri("/rerank")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "query", query == null ? "" : query,
                            "normalize", true,
                            "batchSize", 8,
                            "candidates", candidates.stream()
                                    .map(candidate -> Map.of("id", candidate.id(), "text", candidate.text()))
                                    .toList()
                    ))
                    .retrieve()
                    .body(JsonNode.class);
            return parseResponse(response, elapsedMs(started));
        } catch (RuntimeException exception) {
            String reason = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            return RerankOutcome.failure("reranker-http-failed-at-" + baseUrl + ": " + reason, elapsedMs(started));
        }
    }

    private static RerankOutcome parseResponse(JsonNode response, int latencyMs) {
        if (response == null || !response.path("scores").isArray()) {
            return RerankOutcome.failure("reranker-response-missing-scores", latencyMs);
        }
        Map<String, Double> scores = new LinkedHashMap<>();
        response.path("scores").forEach(row -> {
            String id = row.path("id").asText("");
            if (!id.isBlank()) {
                scores.put(id, clampProbability(row.path("score").asDouble(0.0)));
            }
        });
        return RerankOutcome.success(response.path("model").asText(null), scores, latencyMs);
    }

    private static double clampProbability(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static int elapsedMs(Instant started) {
        long elapsed = Instant.now().toEpochMilli() - started.toEpochMilli();
        return elapsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) elapsed;
    }
}
