package com.localrag.common.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.localrag.common.ollama.OllamaEndpointConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class EmbeddingClient {
    private static final int FALLBACK_DIMENSIONS = 384;
    private static final int MAX_EMBED_BATCH_SIZE = 16;

    private final List<Endpoint> endpoints;
    private final EmbeddingRequestProfile profile;
    private final boolean fallbackEnabled;

    public EmbeddingClient(String baseUrl, String model, boolean fallbackEnabled) {
        this(OllamaEndpointConfig.parseBaseUrls(baseUrl), EmbeddingRequestProfile.direct(model, "rag-bulk"), fallbackEnabled,
                OllamaEndpointConfig.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                OllamaEndpointConfig.DEFAULT_READ_TIMEOUT_MILLIS);
    }

    public EmbeddingClient(String baseUrls, String model, boolean fallbackEnabled, long connectTimeoutMillis, long readTimeoutMillis) {
        this(OllamaEndpointConfig.parseBaseUrls(baseUrls), EmbeddingRequestProfile.direct(model, "rag-bulk"), fallbackEnabled, connectTimeoutMillis, readTimeoutMillis);
    }

    public EmbeddingClient(List<String> baseUrls, String model, boolean fallbackEnabled, long connectTimeoutMillis, long readTimeoutMillis) {
        this(baseUrls, EmbeddingRequestProfile.direct(model, "rag-bulk"), fallbackEnabled, connectTimeoutMillis, readTimeoutMillis);
    }

    public EmbeddingClient(String baseUrls, EmbeddingRequestProfile profile, boolean fallbackEnabled, long connectTimeoutMillis, long readTimeoutMillis) {
        this(OllamaEndpointConfig.parseBaseUrls(baseUrls), profile, fallbackEnabled, connectTimeoutMillis, readTimeoutMillis);
    }

    public EmbeddingClient(List<String> baseUrls, EmbeddingRequestProfile profile, boolean fallbackEnabled, long connectTimeoutMillis, long readTimeoutMillis) {
        this.endpoints = OllamaEndpointConfig.parseBaseUrls(baseUrls).stream()
                .map(baseUrl -> new Endpoint(baseUrl, OllamaEndpointConfig.restClient(baseUrl, connectTimeoutMillis, readTimeoutMillis)))
                .toList();
        this.profile = profile;
        if (fallbackEnabled && (profile.authenticationRequired() || !profile.bearerTokenFile().isBlank())) {
            throw new IllegalArgumentException("authenticated embedding bindings cannot enable fallback vectors");
        }
        this.fallbackEnabled = fallbackEnabled;
    }

    public EmbeddingRequestProfile profile() {
        return profile;
    }

    public List<Double> embed(String text) {
        List<RuntimeException> failures = new ArrayList<>();
        for (Endpoint endpoint : endpoints) {
            try {
                Map<String, Object> requestBody = new LinkedHashMap<>();
                requestBody.put("model", profile.requestModel());
                requestBody.put("prompt", text);
                if (profile.keepAliveSeconds() != null) {
                    requestBody.put("keep_alive", profile.keepAliveSeconds());
                }
                var request = endpoint.restClient().post()
                        .uri("/api/embeddings")
                        .contentType(MediaType.APPLICATION_JSON);
                String authorization = profile.authorizationHeader();
                if (authorization != null) {
                    request.header(HttpHeaders.AUTHORIZATION, authorization);
                }
                JsonNode response = request.body(requestBody)
                        .retrieve()
                        .body(JsonNode.class);
                JsonNode embedding = response == null ? null : response.get("embedding");
                if (embedding != null && embedding.isArray() && !embedding.isEmpty()) {
                    List<Double> vector = new ArrayList<>();
                    embedding.forEach(value -> vector.add(value.asDouble()));
                    profile.validateDimensions(vector.size());
                    return vector;
                }
                throw new IllegalStateException("Ollama embedding response did not contain embedding");
            } catch (EmbeddingContractException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                failures.add(new IllegalStateException("Ollama embedding failed at " + endpoint.baseUrl(), exception));
            }
        }
        if (fallbackEnabled) {
            return deterministicVector(text);
        }
        throw combinedFailure("Ollama embedding failed for all configured endpoints", failures);
    }

    public List<List<Double>> embedAll(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }
        if (texts.size() > MAX_EMBED_BATCH_SIZE) {
            List<List<Double>> result = new ArrayList<>(texts.size());
            for (int start = 0; start < texts.size(); start += MAX_EMBED_BATCH_SIZE) {
                int end = Math.min(start + MAX_EMBED_BATCH_SIZE, texts.size());
                result.addAll(embedAll(texts.subList(start, end)));
            }
            return result;
        }
        List<RuntimeException> failures = new ArrayList<>();
        for (Endpoint endpoint : endpoints) {
            try {
                Map<String, Object> requestBody = new LinkedHashMap<>();
                requestBody.put("model", profile.requestModel());
                requestBody.put("input", texts);
                if (profile.keepAliveSeconds() != null) {
                    requestBody.put("keep_alive", profile.keepAliveSeconds());
                }
                var request = endpoint.restClient().post()
                        .uri("/api/embed")
                        .contentType(MediaType.APPLICATION_JSON);
                String authorization = profile.authorizationHeader();
                if (authorization != null) {
                    request.header(HttpHeaders.AUTHORIZATION, authorization);
                }
                JsonNode response = request.body(requestBody)
                        .retrieve()
                        .body(JsonNode.class);
                JsonNode embeddings = response == null ? null : response.get("embeddings");
                if (embeddings != null && embeddings.isArray() && embeddings.size() == texts.size()) {
                    List<List<Double>> result = new ArrayList<>();
                    for (JsonNode embedding : embeddings) {
                        if (!embedding.isArray() || embedding.isEmpty()) {
                            throw new IllegalStateException("Ollama embed response contained an empty embedding");
                        }
                        List<Double> vector = new ArrayList<>();
                        embedding.forEach(value -> vector.add(value.asDouble()));
                        profile.validateDimensions(vector.size());
                        result.add(vector);
                    }
                    return result;
                }
                throw new IllegalStateException("Ollama embed response did not contain matching embeddings");
            } catch (EmbeddingContractException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                failures.add(new IllegalStateException("Ollama batch embed failed at " + endpoint.baseUrl(), exception));
            }
        }
        if (fallbackEnabled) {
            return texts.stream().map(EmbeddingClient::deterministicVector).toList();
        }
        try {
            return texts.stream().map(this::embed).toList();
        } catch (RuntimeException exception) {
            failures.add(exception);
            throw combinedFailure("Ollama embed failed for all configured endpoints", failures);
        }
    }

    private RuntimeException combinedFailure(String message, List<RuntimeException> failures) {
        String urls = endpoints.stream().map(Endpoint::baseUrl).collect(Collectors.joining(", "));
        IllegalStateException exception = new IllegalStateException(message + ": " + urls);
        failures.forEach(exception::addSuppressed);
        return exception;
    }

    public static List<Double> deterministicVector(String text) {
        byte[] hash = sha256(text);
        long seed = 0;
        for (int i = 0; i < Long.BYTES; i++) {
            seed = (seed << 8) | (hash[i] & 0xffL);
        }
        Random random = new Random(seed);
        List<Double> vector = new ArrayList<>(FALLBACK_DIMENSIONS);
        double norm = 0.0;
        for (int i = 0; i < FALLBACK_DIMENSIONS; i++) {
            double value = random.nextDouble(-1.0, 1.0);
            vector.add(value);
            norm += value * value;
        }
        double length = Math.sqrt(norm);
        for (int i = 0; i < vector.size(); i++) {
            vector.set(i, vector.get(i) / length);
        }
        return vector;
    }

    private static byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    public static String sha256Hex(String text) {
        return HexFormat.of().formatHex(sha256(text));
    }

    private record Endpoint(String baseUrl, RestClient restClient) {
    }
}
