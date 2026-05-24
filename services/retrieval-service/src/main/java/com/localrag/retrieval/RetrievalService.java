package com.localrag.retrieval;

import com.fasterxml.jackson.databind.JsonNode;
import com.localrag.common.dto.SearchRequest;
import com.localrag.common.dto.SearchResponse;
import com.localrag.common.dto.SearchResultItem;
import com.localrag.common.embedding.EmbeddingClient;
import com.localrag.common.weaviate.WeaviateClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Array;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RetrievalService {
    private final EmbeddingClient embeddingClient;
    private final WeaviateClient weaviateClient;
    private final JdbcTemplate jdbcTemplate;

    public RetrievalService(EmbeddingClient embeddingClient, WeaviateClient weaviateClient, JdbcTemplate jdbcTemplate) {
        this.embeddingClient = embeddingClient;
        this.weaviateClient = weaviateClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public SearchResponse search(SearchRequest request) {
        String mode = request.effectiveMode().toLowerCase(Locale.ROOT);
        String projectId = request.projectId();
        List<String> sourceIds = resolveSources(request);
        String graphQl = buildQuery(request, mode, sourceIds);
        Instant started = Instant.now();
        JsonNode response = weaviateClient.graphQl(graphQl);
        List<SearchResultItem> results = parseResults(response);
        audit(projectId, request.query(), mode, request.effectiveLimit(), sourceIds, results.size(), started);
        return new SearchResponse(projectId, request.query(), mode, sourceIds, results);
    }

    private List<String> resolveSources(SearchRequest request) {
        Set<String> excluded = request.excludeSourceIds() == null
                ? Set.of()
                : new HashSet<>(request.excludeSourceIds());
        if (request.includeSourceIds() != null && !request.includeSourceIds().isEmpty()) {
            return activeSourceIds(request.includeSourceIds()).stream()
                    .filter(sourceId -> !excluded.contains(sourceId))
                    .toList();
        }
        if (request.projectId() == null || request.projectId().isBlank()) {
            return jdbcTemplate.query("SELECT source_id FROM source_root WHERE active ORDER BY priority DESC", (rs, rowNum) -> rs.getString(1)).stream()
                    .filter(sourceId -> !excluded.contains(sourceId))
                    .toList();
        }
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        jdbcTemplate.query("""
                        SELECT primary_source_id, default_context_source_ids
                        FROM project_registration
                        WHERE active AND project_id = ?
                        """,
                rs -> {
                    if (rs.next()) {
                        ordered.add(rs.getString("primary_source_id"));
                        ordered.addAll(readTextArray(rs.getArray("default_context_source_ids")));
                    }
                    return null;
                },
                request.projectId());
        jdbcTemplate.query("""
                        SELECT source_id
                        FROM source_root
                        WHERE active AND project_id = ?
                        ORDER BY priority DESC
                        """,
                rs -> {
                    while (rs.next()) {
                        ordered.add(rs.getString("source_id"));
                    }
                    return null;
                },
                request.projectId());
        return activeSourceIds(new ArrayList<>(ordered)).stream()
                .filter(sourceId -> !excluded.contains(sourceId))
                .toList();
    }

    private String buildQuery(SearchRequest request, String mode, List<String> sourceIds) {
        String where = buildWhere(request, sourceIds);
        String searchClause;
        if ("vector".equals(mode)) {
            String vector = embeddingClient.embed(request.query()).stream().map(String::valueOf).collect(Collectors.joining(","));
            searchClause = "nearVector:{vector:[" + vector + "]}";
        } else if ("keyword".equals(mode)) {
            searchClause = "bm25:{query:" + quote(request.query()) + "}";
        } else {
            String vector = embeddingClient.embed(request.query()).stream().map(String::valueOf).collect(Collectors.joining(","));
            searchClause = "hybrid:{query:" + quote(request.query()) + ", vector:[" + vector + "], alpha:0.5}";
        }
        return """
                {
                  Get {
                    LocalRagChunk(%s, %s, limit:%d) {
                      chunkId
                      documentId
                      projectId
                      sourceId
                      ssotRole
                      relativePath
                      headingPath
                      content
                      _additional { score distance }
                    }
                  }
                }
                """.formatted(searchClause, where, request.effectiveLimit());
    }

    private String buildWhere(SearchRequest request, List<String> sourceIds) {
        List<String> operands = new ArrayList<>();
        if (sourceIds.isEmpty() && hasScopedSourceRequest(request)) {
            return "where:{path:[\"sourceId\"], operator:Equal, valueText:\"__local_rag_no_active_source__\"}";
        }
        if (!sourceIds.isEmpty()) {
            if (sourceIds.size() == 1) {
                operands.add("{path:[\"sourceId\"], operator:Equal, valueText:" + quote(sourceIds.get(0)) + "}");
            } else {
                String sourceOperands = sourceIds.stream()
                        .map(sourceId -> "{path:[\"sourceId\"], operator:Equal, valueText:" + quote(sourceId) + "}")
                        .collect(Collectors.joining(","));
                operands.add("{operator:Or, operands:[" + sourceOperands + "]}");
            }
        }
        if (sourceIds.isEmpty() && request.projectId() != null && !request.projectId().isBlank()) {
            operands.add("{path:[\"projectId\"], operator:Equal, valueText:" + quote(request.projectId()) + "}");
        }
        if (operands.isEmpty()) {
            return "where:{operator:Like, path:[\"content\"], valueText:\"*\"}";
        }
        if (operands.size() == 1) {
            return "where:" + operands.get(0);
        }
        return "where:{operator:And, operands:[" + String.join(",", operands) + "]}";
    }

    private static boolean hasScopedSourceRequest(SearchRequest request) {
        return (request.projectId() != null && !request.projectId().isBlank())
                || (request.includeSourceIds() != null && !request.includeSourceIds().isEmpty());
    }

    private List<SearchResultItem> parseResults(JsonNode response) {
        JsonNode rows = response.path("data").path("Get").path("LocalRagChunk");
        List<SearchResultItem> results = new ArrayList<>();
        if (!rows.isArray()) {
            return results;
        }
        for (JsonNode row : rows) {
            String content = row.path("content").asText("");
            String relativePath = row.path("relativePath").asText("");
            String heading = row.path("headingPath").asText("");
            Map<String, Object> score = new LinkedHashMap<>();
            score.put("score", row.path("_additional").path("score").asText(null));
            if (!row.path("_additional").path("distance").isMissingNode()) {
                score.put("distance", row.path("_additional").path("distance").asDouble());
            }
            results.add(new SearchResultItem(
                    row.path("chunkId").asText(),
                    row.path("documentId").asText(),
                    row.path("projectId").asText(),
                    row.path("sourceId").asText(),
                    row.path("ssotRole").asText(),
                    relativePath,
                    heading,
                    heading.isBlank() ? relativePath : relativePath + "#" + heading,
                    snippet(content),
                    score
            ));
        }
        return results;
    }

    private void audit(String projectId, String query, String mode, int limit, List<String> sources, int resultCount, Instant started) {
        jdbcTemplate.update("""
                        INSERT INTO search_audit(project_id, query, mode, limit_requested, sources_searched, result_count, latency_ms)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                projectId, query, mode, limit,
                sources.toArray(String[]::new), resultCount, (int) (Instant.now().toEpochMilli() - started.toEpochMilli()));
    }

    private List<String> activeSourceIds(List<String> requestedSourceIds) {
        if (requestedSourceIds.isEmpty()) {
            return List.of();
        }
        List<String> active = jdbcTemplate.query("""
                        SELECT source_id
                        FROM source_root
                        WHERE active AND source_id = ANY (?)
                        """,
                ps -> ps.setArray(1, ps.getConnection().createArrayOf("text", requestedSourceIds.toArray(String[]::new))),
                (rs, rowNum) -> rs.getString(1));
        Set<String> activeSet = new HashSet<>(active);
        return requestedSourceIds.stream()
                .filter(activeSet::contains)
                .distinct()
                .toList();
    }

    private static List<String> readTextArray(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }
        Object raw = array.getArray();
        if (raw instanceof String[] values) {
            return List.of(values);
        }
        Object[] values = (Object[]) raw;
        List<String> result = new ArrayList<>(values.length);
        for (Object value : values) {
            result.add(String.valueOf(value));
        }
        return result;
    }

    private static String snippet(String content) {
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 497) + "...";
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
