package com.localrag.retrieval;

import com.fasterxml.jackson.databind.JsonNode;
import com.localrag.common.dto.AnswerResponse;
import com.localrag.common.dto.SearchRequest;
import com.localrag.common.dto.SearchResponse;
import com.localrag.common.dto.SearchResultItem;
import com.localrag.common.embedding.EmbeddingClient;
import com.localrag.common.ollama.OllamaChatClient;
import com.localrag.common.weaviate.WeaviateClient;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
    private static final Set<String> SUPPORTED_SEARCH_MODES = Set.of("hybrid", "vector", "keyword");
    private static final int MAX_CANDIDATE_LIMIT = 50;
    private static final int MIN_CANDIDATE_LIMIT = 20;

    private final EmbeddingClient embeddingClient;
    private final OllamaChatClient ollamaChatClient;
    private final WeaviateClient weaviateClient;
    private final JdbcTemplate jdbcTemplate;

    public RetrievalService(
            EmbeddingClient embeddingClient,
            OllamaChatClient ollamaChatClient,
            WeaviateClient weaviateClient,
            JdbcTemplate jdbcTemplate
    ) {
        this.embeddingClient = embeddingClient;
        this.ollamaChatClient = ollamaChatClient;
        this.weaviateClient = weaviateClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public SearchResponse search(SearchRequest request) {
        String mode = normalizeMode(request);
        String projectId = normalizeProjectId(request.projectId());
        validateProject(projectId);
        SearchRequest normalizedRequest = new SearchRequest(
                projectId,
                request.query(),
                request.limit(),
                mode,
                request.includeSourceIds(),
                request.excludeSourceIds(),
                request.filters()
        );
        Instant started = Instant.now();
        SearchScope searchScope = resolveSearchScope(normalizedRequest);
        List<String> sourceIds = searchScope.sourceIds();
        int requestedLimit = normalizedRequest.effectiveLimit();
        int candidateLimit = candidateLimit(requestedLimit);
        String graphQl = buildQuery(normalizedRequest, mode, sourceIds, candidateLimit);
        JsonNode response = weaviateClient.graphQl(graphQl);
        List<RetrievalRanker.RankCandidate> candidates = parseCandidates(response);
        List<SearchResultItem> results = RetrievalRanker.rank(
                normalizedRequest,
                searchScope.rankContext(),
                candidates,
                requestedLimit
        );
        audit(projectId, normalizedRequest.query(), mode, requestedLimit, sourceIds, results.size(), started);
        return new SearchResponse(projectId, normalizedRequest.query(), mode, sourceIds, results);
    }

    public AnswerResponse answer(SearchRequest request) {
        SearchResponse search = search(request);
        String answer = ollamaChatClient.chat(systemPrompt(), answerPrompt(request.query(), search.results()));
        return new AnswerResponse(
                search.projectId(),
                search.query(),
                search.mode(),
                ollamaChatClient.model(),
                answer,
                search.sourcesSearched(),
                search.results().stream().map(SearchResultItem::citation).distinct().toList(),
                search.results()
        );
    }

    private SearchScope resolveSearchScope(SearchRequest request) {
        Set<String> excluded = request.excludeSourceIds() == null
                ? Set.of()
                : new HashSet<>(request.excludeSourceIds());
        ProjectContext projectContext = projectContext(request.projectId());
        List<String> sourceIds;
        if (request.includeSourceIds() != null && !request.includeSourceIds().isEmpty()) {
            sourceIds = activeSourceIds(request.includeSourceIds()).stream()
                    .filter(sourceId -> !excluded.contains(sourceId))
                    .toList();
            Map<String, RetrievalRanker.SourceInfo> sourceMetadata = sourceMetadata(sourceIds);
            return new SearchScope(sourceIds, new RetrievalRanker.RankContext(
                    request.projectId(),
                    projectContext.primarySourceId(),
                    Set.copyOf(projectContext.defaultContextSourceIds()),
                    sourceMetadata
            ));
        }
        if (request.projectId() == null || request.projectId().isBlank()) {
            sourceIds = jdbcTemplate.query("SELECT source_id FROM source_root WHERE active ORDER BY priority DESC", (rs, rowNum) -> rs.getString(1)).stream()
                    .filter(sourceId -> !excluded.contains(sourceId))
                    .toList();
            Map<String, RetrievalRanker.SourceInfo> sourceMetadata = sourceMetadata(sourceIds);
            return new SearchScope(sourceIds, new RetrievalRanker.RankContext(
                    null,
                    null,
                    Set.of(),
                    sourceMetadata
            ));
        }
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        if (projectContext.primarySourceId() != null && !projectContext.primarySourceId().isBlank()) {
            ordered.add(projectContext.primarySourceId());
        }
        ordered.addAll(projectContext.defaultContextSourceIds());
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
        sourceIds = activeSourceIds(new ArrayList<>(ordered)).stream()
                .filter(sourceId -> !excluded.contains(sourceId))
                .toList();
        Map<String, RetrievalRanker.SourceInfo> sourceMetadata = sourceMetadata(sourceIds);
        return new SearchScope(sourceIds, new RetrievalRanker.RankContext(
                request.projectId(),
                projectContext.primarySourceId(),
                Set.copyOf(projectContext.defaultContextSourceIds()),
                sourceMetadata
        ));
    }

    private String normalizeMode(SearchRequest request) {
        String mode = request.effectiveMode().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_SEARCH_MODES.contains(mode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported search mode: " + request.mode());
        }
        return mode;
    }

    private String normalizeProjectId(String projectId) {
        return projectId == null || projectId.isBlank() ? null : projectId.trim();
    }

    private void validateProject(String projectId) {
        if (projectId == null) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT count(*)
                        FROM project_registration
                        WHERE active AND project_id = ?
                        """,
                Integer.class,
                projectId);
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown projectId: " + projectId);
        }
    }

    private String buildQuery(SearchRequest request, String mode, List<String> sourceIds, int limit) {
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
                      sourceType
                      ssotRole
                      relativePath
                      headingPath
                      contentHash
                      content
                      _additional { score distance }
                    }
                  }
                }
                """.formatted(searchClause, where, limit);
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

    private List<RetrievalRanker.RankCandidate> parseCandidates(JsonNode response) {
        JsonNode rows = response.path("data").path("Get").path("LocalRagChunk");
        List<RetrievalRanker.RankCandidate> candidates = new ArrayList<>();
        if (!rows.isArray()) {
            return candidates;
        }
        int rawRank = 0;
        for (JsonNode row : rows) {
            String content = row.path("content").asText("");
            String relativePath = row.path("relativePath").asText("");
            String heading = row.path("headingPath").asText("");
            Map<String, Object> score = new LinkedHashMap<>();
            score.put("score", row.path("_additional").path("score").asText(null));
            if (!row.path("_additional").path("distance").isMissingNode()) {
                score.put("distance", row.path("_additional").path("distance").asDouble());
            }
            SearchResultItem item = new SearchResultItem(
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
            );
            candidates.add(new RetrievalRanker.RankCandidate(item, row.path("contentHash").asText(null), rawRank));
            rawRank++;
        }
        return candidates;
    }

    private void audit(String projectId, String query, String mode, int limit, List<String> sources, int resultCount, Instant started) {
        jdbcTemplate.update("""
                        INSERT INTO search_audit(project_id, query, mode, limit_requested, sources_searched, result_count, latency_ms)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                projectId, query, mode, limit,
                sources.toArray(String[]::new), resultCount, (int) (Instant.now().toEpochMilli() - started.toEpochMilli()));
    }

    private ProjectContext projectContext(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            return new ProjectContext(null, List.of());
        }
        return jdbcTemplate.query("""
                        SELECT primary_source_id, default_context_source_ids
                        FROM project_registration
                        WHERE active AND project_id = ?
                        """,
                rs -> {
                    if (!rs.next()) {
                        return new ProjectContext(null, List.of());
                    }
                    return new ProjectContext(
                            rs.getString("primary_source_id"),
                            readTextArray(rs.getArray("default_context_source_ids"))
                    );
                },
                projectId);
    }

    private Map<String, RetrievalRanker.SourceInfo> sourceMetadata(List<String> sourceIds) {
        if (sourceIds.isEmpty()) {
            return Map.of();
        }
        return jdbcTemplate.query("""
                        SELECT source_id, project_id, source_type, ssot_role, priority
                        FROM source_root
                        WHERE active AND source_id = ANY (?)
                        """,
                ps -> ps.setArray(1, ps.getConnection().createArrayOf("text", sourceIds.toArray(String[]::new))),
                rs -> {
                    Map<String, RetrievalRanker.SourceInfo> metadata = new LinkedHashMap<>();
                    while (rs.next()) {
                        metadata.put(rs.getString("source_id"), new RetrievalRanker.SourceInfo(
                                rs.getString("source_id"),
                                rs.getString("project_id"),
                                rs.getString("source_type"),
                                rs.getString("ssot_role"),
                                rs.getInt("priority")
                        ));
                    }
                    return metadata;
                });
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

    private static String systemPrompt() {
        return """
                You are a local-only RAG assistant. Answer only from the supplied context.
                Cite sources using the citation labels in square brackets.
                If the context is insufficient, say that the indexed local sources do not contain enough evidence.
                Answer in the same language as the question.
                """;
    }

    private static String answerPrompt(String query, List<SearchResultItem> results) {
        String context = results.stream()
                .map(result -> "[%s]\n%s".formatted(result.citation(), result.snippet()))
                .collect(Collectors.joining("\n\n"));
        if (context.isBlank()) {
            context = "No retrieved context.";
        }
        return """
                Question:
                %s

                Retrieved context:
                %s
                """.formatted(query, context);
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private static int candidateLimit(int requestedLimit) {
        return Math.min(MAX_CANDIDATE_LIMIT, Math.max(MIN_CANDIDATE_LIMIT, requestedLimit * 4));
    }

    private record ProjectContext(String primarySourceId, List<String> defaultContextSourceIds) {
        private ProjectContext {
            defaultContextSourceIds = defaultContextSourceIds == null ? List.of() : List.copyOf(defaultContextSourceIds);
        }
    }

    private record SearchScope(List<String> sourceIds, RetrievalRanker.RankContext rankContext) {
    }
}
