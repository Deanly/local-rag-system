package com.localrag.common.weaviate;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WeaviateClient {
    public static final String CLASS_NAME = "LocalRagChunk";
    private static final List<Map<String, Object>> PROPERTIES = List.of(
            property("chunkId", "text"),
            property("documentId", "text"),
            property("projectId", "text"),
            property("sourceId", "text"),
            property("sourceType", "text"),
            property("ssotRole", "text"),
            property("relativePath", "text"),
            property("fileName", "text"),
            property("folder", "text"),
            property("extension", "text"),
            property("title", "text"),
            property("docType", "text"),
            property("frontmatterStatus", "text"),
            property("authority", "text"),
            property("updated", "date"),
            property("supersedes", "text[]"),
            property("supersededBy", "text[]"),
            property("headingPath", "text"),
            property("headingPathSegments", "text[]"),
            property("headingDepth", "int"),
            property("headingSlug", "text"),
            property("chunkContext", "text"),
            property("chunkIndex", "int"),
            property("content", "text"),
            property("contentHash", "text"),
            property("sensitivity", "text"),
            property("tags", "text[]"),
            property("links", "text[]"),
            property("fileMtimeNs", "number"),
            property("indexedAt", "date"),
            property("embeddingModel", "text"),
            property("embeddingDimensions", "int"),
            property("embeddingBindingId", "text"),
            property("embeddingLane", "text")
    );

    private final RestClient restClient;

    public WeaviateClient(String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void ensureSchema() {
        try {
            JsonNode schema = restClient.get().uri("/v1/schema/{className}", CLASS_NAME).retrieve().body(JsonNode.class);
            ensureProperties(schema);
            return;
        } catch (HttpClientErrorException.NotFound ignored) {
            // Create the schema below.
        }
        restClient.post()
                .uri("/v1/schema")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "class", CLASS_NAME,
                        "vectorizer", "none",
                        "properties", PROPERTIES
                ))
                .retrieve()
                .toBodilessEntity();
    }

    public void upsert(String id, List<Double> vector, Map<String, Object> properties) {
        deleteQuietly(UUID.fromString(id));
        restClient.post()
                .uri("/v1/objects")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("class", CLASS_NAME, "id", id, "vector", vector, "properties", properties))
                .retrieve()
                .toBodilessEntity();
    }

    public void deleteQuietly(UUID id) {
        if (id == null) {
            return;
        }
        try {
            restClient.delete().uri("/v1/objects/{className}/{id}", CLASS_NAME, id).retrieve().toBodilessEntity();
        } catch (RuntimeException ignored) {
            // Derived index cleanup should be idempotent.
        }
    }

    public JsonNode graphQl(String query) {
        return restClient.post()
                .uri("/v1/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("query", query))
                .retrieve()
                .body(JsonNode.class);
    }

    private static Map<String, Object> property(String name, String dataType) {
        return Map.of("name", name, "dataType", List.of(dataType));
    }

    private void ensureProperties(JsonNode schema) {
        Set<String> existing = new HashSet<>();
        schema.path("properties").forEach(property -> existing.add(property.path("name").asText()));
        for (Map<String, Object> property : PROPERTIES) {
            if (!existing.contains(property.get("name"))) {
                restClient.post()
                        .uri("/v1/schema/{className}/properties", CLASS_NAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(property)
                        .retrieve()
                        .toBodilessEntity();
            }
        }
    }
}
