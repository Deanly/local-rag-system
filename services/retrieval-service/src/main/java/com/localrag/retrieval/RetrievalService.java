package com.localrag.retrieval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RetrievalService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> SUPPORTED_SEARCH_MODES = Set.of("hybrid", "vector", "keyword");
    private static final Pattern TASK_ID_WITH_SUFFIX_PATTERN = Pattern.compile("(?i)\\b(T\\d{4})(?=\\p{L})");
    private static final Map<String, String> FILTER_FIELD_ALIASES = Map.of(
            "ssotRole", "ssotRole",
            "sourceType", "sourceType",
            "status", "frontmatterStatus",
            "frontmatterStatus", "frontmatterStatus",
            "authority", "authority",
            "sensitivity", "sensitivity",
            "docType", "docType"
    );
    private static final int MAX_CANDIDATE_LIMIT = 50;
    private static final int MIN_CANDIDATE_LIMIT = 20;

    private final EmbeddingClient embeddingClient;
    private final OllamaChatClient ollamaChatClient;
    private final WeaviateClient weaviateClient;
    private final JdbcTemplate jdbcTemplate;
    private final RetrievalSettings settings;
    private final LocalRerankerClient localRerankerClient;
    private volatile boolean auditSchemaReady;

    public RetrievalService(
            EmbeddingClient embeddingClient,
            OllamaChatClient ollamaChatClient,
            WeaviateClient weaviateClient,
            JdbcTemplate jdbcTemplate,
            RetrievalSettings settings,
            LocalRerankerClient localRerankerClient
    ) {
        this.embeddingClient = embeddingClient;
        this.ollamaChatClient = ollamaChatClient;
        this.weaviateClient = weaviateClient;
        this.jdbcTemplate = jdbcTemplate;
        this.settings = settings;
        this.localRerankerClient = localRerankerClient == null ? LocalRerankerClient.disabled() : localRerankerClient;
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
        weaviateClient.ensureSchema();
        SearchScope searchScope = resolveSearchScope(normalizedRequest);
        List<String> sourceIds = searchScope.sourceIds();
        int requestedLimit = normalizedRequest.effectiveLimit();
        int candidateLimit = candidateLimit(requestedLimit);
        ScoreGateMode scoreGateMode = scoreGateMode(normalizedRequest);
        String retrievalQuery = retrievalQuery(normalizedRequest.query());
        long embeddingStarted = System.currentTimeMillis();
        List<Double> vector = searchVector(mode, retrievalQuery);
        int embeddingLatencyMs = (int) (System.currentTimeMillis() - embeddingStarted);
        String graphQl = buildQuery(normalizedRequest, mode, sourceIds, candidateLimit, retrievalQuery, vector);
        long weaviateStarted = System.currentTimeMillis();
        JsonNode response = weaviateClient.graphQl(graphQl);
        int weaviateLatencyMs = (int) (System.currentTimeMillis() - weaviateStarted);
        List<RetrievalRanker.RankCandidate> candidates = parseCandidates(response);
        long weightingStarted = System.currentTimeMillis();
        int rankLimit = scoreGateMode == ScoreGateMode.OFF ? requestedLimit : candidateLimit;
        List<SearchResultItem> rankedResults = RetrievalRanker.rank(
                normalizedRequest,
                searchScope.rankContext(),
                candidates,
                rankLimit
        );
        int weightingLatencyMs = (int) (System.currentTimeMillis() - weightingStarted);
        ScoreGateSelection scoreGateSelection = applyScoreGate(
                normalizedRequest,
                scoreGateMode,
                rankedResults,
                requestedLimit
        );
        List<SearchResultItem> results = scoreGateSelection.results();
        audit(new SearchAuditRecord(
                projectId,
                normalizedRequest.query(),
                mode,
                requestedLimit,
                sourceIds,
                results.size(),
                candidateLimit,
                candidates.size(),
                results.size(),
                embeddingLatencyMs,
                weaviateLatencyMs,
                weightingLatencyMs,
                scoreGateSelection.rerankLatencyMs(),
                (int) (Instant.now().toEpochMilli() - started.toEpochMilli()),
                sourceDistribution(results),
                results.isEmpty() ? null : results.get(0).sourceId(),
                results.isEmpty() ? null : results.get(0).relativePath(),
                results.isEmpty() ? Map.of() : results.get(0).score()
        ));
        return new SearchResponse(projectId, normalizedRequest.query(), mode, sourceIds, results);
    }

    public AnswerResponse answer(SearchRequest request) {
        if (settings.chatModel() == null || settings.chatModel().isBlank()) {
            throw new AnswerGenerationDisabledException();
        }
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

    private ScoreGateSelection applyScoreGate(
            SearchRequest request,
            ScoreGateMode mode,
            List<SearchResultItem> rankedResults,
            int requestedLimit
    ) {
        if (mode == ScoreGateMode.OFF || rankedResults.isEmpty()) {
            return new ScoreGateSelection(rankedResults.stream().limit(requestedLimit).toList(), 0);
        }

        List<LocalRerankerClient.CandidateText> candidateTexts = rankedResults.stream()
                .map(item -> new LocalRerankerClient.CandidateText(item.chunkId(), rerankerText(item)))
                .toList();
        LocalRerankerClient.RerankOutcome outcome = localRerankerClient.rerank(request.query(), candidateTexts);
        if (!outcome.successful()) {
            return new ScoreGateSelection(
                    rankedResults.stream()
                            .limit(requestedLimit)
                            .map(item -> withScoreGateFallback(item, mode, outcome))
                            .toList(),
                    outcome.latencyMs()
            );
        }

        List<ScoreGateCandidateSelector.Candidate> selectorCandidates = new ArrayList<>();
        Map<String, SearchResultItem> itemsById = new LinkedHashMap<>();
        for (int index = 0; index < rankedResults.size(); index++) {
            SearchResultItem item = rankedResults.get(index);
            Double rerankerScore = outcome.scores().get(item.chunkId());
            if (rerankerScore == null) {
                continue;
            }
            itemsById.put(item.chunkId(), item);
            selectorCandidates.add(new ScoreGateCandidateSelector.Candidate(
                    item.chunkId(),
                    normalizedSimilarityScore(item.score()),
                    rerankerScore,
                    index,
                    selectorMetadata(item)
            ));
        }
        if (selectorCandidates.isEmpty()) {
            LocalRerankerClient.RerankOutcome emptyOutcome = LocalRerankerClient.RerankOutcome.failure(
                    "reranker-response-contained-no-matching-candidate-ids",
                    outcome.latencyMs()
            );
            return new ScoreGateSelection(
                    rankedResults.stream()
                            .limit(requestedLimit)
                            .map(item -> withScoreGateFallback(item, mode, emptyOutcome))
                            .toList(),
                    outcome.latencyMs()
            );
        }

        ScoreGateCandidateSelector.Result result = ScoreGateCandidateSelector.select(
                selectorCandidates,
                scoreGateConfig(requestedLimit)
        );
        Map<String, ScoreGateCandidateSelector.Decision> decisionsById = new LinkedHashMap<>();
        for (ScoreGateCandidateSelector.Decision decision : result.decisions()) {
            decisionsById.put(decision.candidate().id(), decision);
        }

        if (mode == ScoreGateMode.DEBUG) {
            return new ScoreGateSelection(
                    rankedResults.stream()
                            .limit(requestedLimit)
                            .map(item -> {
                                ScoreGateCandidateSelector.Decision decision = decisionsById.get(item.chunkId());
                                return decision == null
                                        ? withScoreGateFallback(item, mode, outcome)
                                        : withScoreGateDecision(item, decision, mode, outcome);
                            })
                            .toList(),
                    outcome.latencyMs()
            );
        }

        List<SearchResultItem> selected = result.retained().stream()
                .limit(requestedLimit)
                .map(decision -> withScoreGateDecision(itemsById.get(decision.candidate().id()), decision, mode, outcome))
                .toList();
        return new ScoreGateSelection(selected, outcome.latencyMs());
    }

    private ScoreGateCandidateSelector.Config scoreGateConfig(int requestedLimit) {
        return new ScoreGateCandidateSelector.Config(
                settings.scoreGateSimilarityThreshold(),
                settings.scoreGateRerankerThreshold(),
                settings.scoreGateSimilarityWeight(),
                settings.scoreGateBucket2Threshold(),
                settings.scoreGateBucket3Threshold(),
                Math.max(1, Math.min(settings.scoreGateMaxK(), requestedLimit))
        );
    }

    private ScoreGateMode scoreGateMode(SearchRequest request) {
        List<String> values = scoreGateFilterValues(request.filters());
        for (String value : values) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (Set.of("debug", "audit", "explain").contains(normalized)) {
                return ScoreGateMode.DEBUG;
            }
            if (Set.of("on", "true", "yes", "1", "enabled", "apply").contains(normalized)) {
                return ScoreGateMode.ON;
            }
            if (Set.of("off", "false", "no", "0", "disabled").contains(normalized)) {
                return ScoreGateMode.OFF;
            }
        }
        return settings.scoreGateEnabled() ? ScoreGateMode.ON : ScoreGateMode.OFF;
    }

    private static List<String> scoreGateFilterValues(Map<String, List<String>> filters) {
        if (filters == null || filters.isEmpty()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String key : List.of("scoreGate", "scoregate", "score_gate")) {
            List<String> keyValues = filters.get(key);
            if (keyValues != null) {
                values.addAll(keyValues);
            }
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    private static String rerankerText(SearchResultItem item) {
        Map<String, Object> metadata = item.metadata() == null ? Map.of() : item.metadata();
        return List.of(
                        item.relativePath(),
                        item.headingPath(),
                        String.valueOf(metadata.getOrDefault("title", "")),
                        String.valueOf(metadata.getOrDefault("chunkContext", "")),
                        item.snippet()
                ).stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .collect(Collectors.joining("\n"));
    }

    private static Map<String, Object> selectorMetadata(SearchResultItem item) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (item.metadata() != null) {
            metadata.putAll(item.metadata());
        }
        metadata.put("sourceId", item.sourceId());
        metadata.put("relativePath", item.relativePath());
        metadata.put("headingPath", item.headingPath());
        return metadata;
    }

    private static SearchResultItem withScoreGateDecision(
            SearchResultItem item,
            ScoreGateCandidateSelector.Decision decision,
            ScoreGateMode mode,
            LocalRerankerClient.RerankOutcome outcome
    ) {
        Map<String, Object> score = new LinkedHashMap<>();
        if (item.score() != null) {
            score.putAll(item.score());
        }
        score.put("scoreGateMode", mode.name().toLowerCase(Locale.ROOT));
        score.put("scoreGateApplied", true);
        score.put("similarityScore", decision.candidate().similarityScore());
        score.put("crossEncoderScore", decision.candidate().rerankerScore());
        score.put("scoreGateBucket", decision.bucket().name());
        score.put("scoreGateFusionScore", decision.fusionScore());
        score.put("scoreGateRetained", decision.retained());
        score.put("scoreGateDecisionReason", decision.reason());
        score.put("scoreGateRerankLatencyMs", outcome.latencyMs());
        if (outcome.model() != null && !outcome.model().isBlank()) {
            score.put("scoreGateModel", outcome.model());
        }
        return copyWithScore(item, score);
    }

    private static SearchResultItem withScoreGateFallback(
            SearchResultItem item,
            ScoreGateMode mode,
            LocalRerankerClient.RerankOutcome outcome
    ) {
        Map<String, Object> score = new LinkedHashMap<>();
        if (item.score() != null) {
            score.putAll(item.score());
        }
        score.put("scoreGateMode", mode.name().toLowerCase(Locale.ROOT));
        score.put("scoreGateApplied", false);
        score.put("scoreGateRerankAttempted", outcome.attempted());
        score.put("scoreGateRerankLatencyMs", outcome.latencyMs());
        score.put("scoreGateFallbackReason", outcome.fallbackReason());
        return copyWithScore(item, score);
    }

    private static SearchResultItem copyWithScore(SearchResultItem item, Map<String, Object> score) {
        return new SearchResultItem(
                item.chunkId(),
                item.documentId(),
                item.projectId(),
                item.sourceId(),
                item.ssotRole(),
                item.relativePath(),
                item.headingPath(),
                item.citation(),
                item.snippet(),
                item.metadata(),
                score
        );
    }

    private static double normalizedSimilarityScore(Map<String, Object> score) {
        if (score == null) {
            return 0.0;
        }
        Double rawScore = scoreNumber(score.get("score"));
        if (rawScore != null) {
            return clampProbability(rawScore);
        }
        Double distance = scoreNumber(score.get("distance"));
        if (distance != null) {
            return clampProbability(1.0 / (1.0 + Math.max(0.0, distance)));
        }
        Double baseScore = scoreNumber(score.get("baseScore"));
        return baseScore == null ? 0.0 : clampProbability(baseScore);
    }

    private static Double scoreNumber(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static double clampProbability(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
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

    private List<Double> searchVector(String mode, String retrievalQuery) {
        if ("keyword".equals(mode)) {
            return List.of();
        }
        return embeddingClient.embed(retrievalQuery);
    }

    private String buildQuery(SearchRequest request, String mode, List<String> sourceIds, int limit, String retrievalQuery, List<Double> vectorValues) {
        String where = buildWhere(request, sourceIds);
        String searchClause;
        if ("vector".equals(mode)) {
            String vector = vectorValues.stream().map(String::valueOf).collect(Collectors.joining(","));
            searchClause = "nearVector:{vector:[" + vector + "]}";
        } else if ("keyword".equals(mode)) {
            searchClause = "bm25:{query:" + quote(retrievalQuery) + "}";
        } else {
            String vector = vectorValues.stream().map(String::valueOf).collect(Collectors.joining(","));
            searchClause = "hybrid:{query:" + quote(retrievalQuery) + ", vector:[" + vector + "], alpha:0.5}";
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
                      fileName
                      folder
                      extension
                      title
                      docType
                      frontmatterStatus
                      authority
                      updated
                      supersedes
                      supersededBy
                      headingPath
                      headingPathSegments
                      headingDepth
                      headingSlug
                      chunkContext
                      contentHash
                      sensitivity
                      tags
                      links
                      content
                      _additional { score distance }
                    }
                  }
                }
                """.formatted(searchClause, where, limit);
    }

    static String buildWhere(SearchRequest request, List<String> sourceIds) {
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
        operands.addAll(filterOperands(request.filters()));
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

    private static List<String> filterOperands(Map<String, List<String>> filters) {
        if (filters == null || filters.isEmpty()) {
            return List.of();
        }
        List<String> operands = new ArrayList<>();
        for (Map.Entry<String, String> entry : FILTER_FIELD_ALIASES.entrySet()) {
            List<String> values = normalizedFilterValues(filters.get(entry.getKey()));
            if (values.isEmpty()) {
                continue;
            }
            operands.add(textFilterOperand(entry.getValue(), values));
        }
        return operands;
    }

    private static List<String> normalizedFilterValues(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private static String textFilterOperand(String field, List<String> values) {
        if (values.size() == 1) {
            return "{path:[\"" + field + "\"], operator:Equal, valueText:" + quote(values.get(0)) + "}";
        }
        String valueOperands = values.stream()
                .map(value -> "{path:[\"" + field + "\"], operator:Equal, valueText:" + quote(value) + "}")
                .collect(Collectors.joining(","));
        return "{operator:Or, operands:[" + valueOperands + "]}";
    }

    static List<RetrievalRanker.RankCandidate> parseCandidates(JsonNode response) {
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
            Map<String, Object> metadata = metadata(row);
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
                    citation(relativePath, heading, metadata),
                    snippet(content),
                    metadata,
                    score
            );
            candidates.add(new RetrievalRanker.RankCandidate(item, row.path("contentHash").asText(null), rawRank));
            rawRank++;
        }
        return candidates;
    }

    private void audit(SearchAuditRecord record) {
        ensureAuditSchema();
        jdbcTemplate.update("""
                        INSERT INTO search_audit(
                            project_id,
                            query,
                            mode,
                            limit_requested,
                            sources_searched,
                            result_count,
                            latency_ms,
                            candidate_limit,
                            raw_candidate_count,
                            final_result_count,
                            embedding_latency_ms,
                            weaviate_latency_ms,
                            weighting_latency_ms,
                            rerank_latency_ms,
                            total_latency_ms,
                            source_distribution,
                            top_result_source_id,
                            top_result_relative_path,
                            top_result_score
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, CAST(? AS jsonb))
                        """,
                record.projectId(),
                record.query(),
                record.mode(),
                record.limitRequested(),
                record.sourcesSearched().toArray(String[]::new),
                record.resultCount(),
                record.totalLatencyMs(),
                record.candidateLimit(),
                record.rawCandidateCount(),
                record.finalResultCount(),
                record.embeddingLatencyMs(),
                record.weaviateLatencyMs(),
                record.weightingLatencyMs(),
                record.rerankLatencyMs(),
                record.totalLatencyMs(),
                toJson(record.sourceDistribution()),
                record.topResultSourceId(),
                record.topResultRelativePath(),
                toJson(record.topResultScore()));
    }

    private void ensureAuditSchema() {
        if (auditSchemaReady) {
            return;
        }
        synchronized (this) {
            if (auditSchemaReady) {
                return;
            }
            jdbcTemplate.execute("""
                    ALTER TABLE search_audit
                        ADD COLUMN IF NOT EXISTS candidate_limit INTEGER NOT NULL DEFAULT 0,
                        ADD COLUMN IF NOT EXISTS raw_candidate_count INTEGER NOT NULL DEFAULT 0,
                        ADD COLUMN IF NOT EXISTS final_result_count INTEGER NOT NULL DEFAULT 0,
                        ADD COLUMN IF NOT EXISTS embedding_latency_ms INTEGER,
                        ADD COLUMN IF NOT EXISTS weaviate_latency_ms INTEGER,
                        ADD COLUMN IF NOT EXISTS weighting_latency_ms INTEGER,
                        ADD COLUMN IF NOT EXISTS rerank_latency_ms INTEGER,
                        ADD COLUMN IF NOT EXISTS total_latency_ms INTEGER,
                        ADD COLUMN IF NOT EXISTS source_distribution JSONB NOT NULL DEFAULT '{}'::jsonb,
                        ADD COLUMN IF NOT EXISTS top_result_source_id TEXT,
                        ADD COLUMN IF NOT EXISTS top_result_relative_path TEXT,
                        ADD COLUMN IF NOT EXISTS top_result_score JSONB NOT NULL DEFAULT '{}'::jsonb
                    """);
            auditSchemaReady = true;
        }
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

    private static Map<String, Object> metadata(JsonNode row) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        putText(metadata, row, "sourceType");
        putText(metadata, row, "fileName");
        putText(metadata, row, "folder");
        putText(metadata, row, "extension");
        putText(metadata, row, "title");
        putText(metadata, row, "docType");
        putText(metadata, row, "frontmatterStatus");
        putText(metadata, row, "authority");
        putText(metadata, row, "updated");
        putText(metadata, row, "sensitivity");
        putText(metadata, row, "headingSlug");
        putText(metadata, row, "chunkContext");
        if (row.hasNonNull("headingDepth")) {
            metadata.put("headingDepth", row.path("headingDepth").asInt());
        }
        metadata.put("supersedes", readStringList(row.path("supersedes")));
        metadata.put("supersededBy", readStringList(row.path("supersededBy")));
        metadata.put("headingPathSegments", readStringList(row.path("headingPathSegments")));
        metadata.put("tags", readStringList(row.path("tags")));
        metadata.put("links", readStringList(row.path("links")));
        return metadata;
    }

    private static void putText(Map<String, Object> metadata, JsonNode row, String key) {
        if (row.hasNonNull(key)) {
            metadata.put(key, row.path(key).asText(""));
        }
    }

    private static List<String> readStringList(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(value -> {
            String text = value.asText("");
            if (!text.isBlank()) {
                values.add(text);
            }
        });
        return List.copyOf(values);
    }

    private static String citation(String relativePath, String heading, Map<String, Object> metadata) {
        Object slug = metadata.get("headingSlug");
        if (slug != null && !String.valueOf(slug).isBlank()) {
            return relativePath + "#" + slug;
        }
        return heading == null || heading.isBlank() ? relativePath : relativePath + "#" + heading;
    }

    private static String systemPrompt() {
        return """
                You are a local-only RAG assistant. Answer only from the supplied context.
                Cite sources using the citation labels in square brackets.
                Prefer current, canonical, accepted, and project-current-truth sources over draft, raw, deprecated, or superseded sources.
                Use deprecated or superseded sources only when the question explicitly asks for history or migration context.
                If the context is insufficient, say that the indexed local sources do not contain enough evidence.
                Answer in the same language as the question.
                """;
    }

    static String answerPrompt(String query, List<SearchResultItem> results) {
        String context = results.stream()
                .map(result -> """
                        [%s]
                        Source priority: %s
                        Snippet: %s
                        """.formatted(result.citation(), sourcePriorityLine(result), result.snippet()).trim())
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

    private static String sourcePriorityLine(SearchResultItem result) {
        Map<String, Object> metadata = result.metadata() == null ? Map.of() : result.metadata();
        return "title=%s; sourceId=%s; ssotRole=%s; docType=%s; status=%s; authority=%s; updated=%s; supersededBy=%s"
                .formatted(
                        metadataValue(metadata, "title", result.relativePath()),
                        result.sourceId(),
                        result.ssotRole(),
                        metadataValue(metadata, "docType", "unknown"),
                        metadataValue(metadata, "frontmatterStatus", "unknown"),
                        metadataValue(metadata, "authority", "source-default"),
                        metadataValue(metadata, "updated", "unknown"),
                        metadataValue(metadata, "supersededBy", "[]")
                );
    }

    private static String metadataValue(Map<String, Object> metadata, String key, String defaultValue) {
        Object value = metadata.get(key);
        if (value == null) {
            return defaultValue;
        }
        return String.valueOf(value);
    }

    static Map<String, Integer> sourceDistribution(List<SearchResultItem> results) {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        for (SearchResultItem result : results) {
            distribution.merge(result.sourceId(), 1, Integer::sum);
        }
        return distribution;
    }

    private static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize search audit JSON", exception);
        }
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    static String retrievalQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        return TASK_ID_WITH_SUFFIX_PATTERN.matcher(query).replaceAll("$1 ").trim();
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

    private enum ScoreGateMode {
        OFF,
        DEBUG,
        ON
    }

    private record ScoreGateSelection(List<SearchResultItem> results, int rerankLatencyMs) {
        private ScoreGateSelection {
            results = results == null ? List.of() : List.copyOf(results);
        }
    }

    private record SearchAuditRecord(
            String projectId,
            String query,
            String mode,
            int limitRequested,
            List<String> sourcesSearched,
            int resultCount,
            int candidateLimit,
            int rawCandidateCount,
            int finalResultCount,
            int embeddingLatencyMs,
            int weaviateLatencyMs,
            int weightingLatencyMs,
            int rerankLatencyMs,
            int totalLatencyMs,
            Map<String, Integer> sourceDistribution,
            String topResultSourceId,
            String topResultRelativePath,
            Map<String, Object> topResultScore
    ) {
        private SearchAuditRecord {
            sourcesSearched = sourcesSearched == null ? List.of() : List.copyOf(sourcesSearched);
            sourceDistribution = sourceDistribution == null ? Map.of() : Map.copyOf(sourceDistribution);
            topResultScore = topResultScore == null ? Map.of() : Map.copyOf(topResultScore);
        }
    }
}
