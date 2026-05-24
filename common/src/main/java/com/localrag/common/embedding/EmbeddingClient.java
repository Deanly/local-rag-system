package com.localrag.common.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class EmbeddingClient {
    private static final int FALLBACK_DIMENSIONS = 384;

    private final RestClient restClient;
    private final String model;
    private final boolean fallbackEnabled;

    public EmbeddingClient(String baseUrl, String model, boolean fallbackEnabled) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.model = model;
        this.fallbackEnabled = fallbackEnabled;
    }

    public List<Double> embed(String text) {
        try {
            JsonNode response = restClient.post()
                    .uri("/api/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("model", model, "prompt", text))
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode embedding = response == null ? null : response.get("embedding");
            if (embedding != null && embedding.isArray() && !embedding.isEmpty()) {
                List<Double> vector = new ArrayList<>();
                embedding.forEach(value -> vector.add(value.asDouble()));
                return vector;
            }
            throw new IllegalStateException("Ollama embedding response did not contain embedding");
        } catch (RuntimeException exception) {
            if (!fallbackEnabled) {
                throw exception;
            }
            return deterministicVector(text);
        }
    }

    public List<List<Double>> embedAll(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }
        try {
            JsonNode response = restClient.post()
                    .uri("/api/embed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("model", model, "input", texts))
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
                    result.add(vector);
                }
                return result;
            }
            throw new IllegalStateException("Ollama embed response did not contain matching embeddings");
        } catch (RuntimeException exception) {
            if (fallbackEnabled) {
                return texts.stream().map(EmbeddingClient::deterministicVector).toList();
            }
            return texts.stream().map(this::embed).toList();
        }
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
}
