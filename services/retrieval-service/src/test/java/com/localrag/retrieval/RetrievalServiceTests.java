package com.localrag.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.localrag.common.dto.SearchRequest;
import com.localrag.common.dto.SearchResultItem;
import com.localrag.common.embedding.EmbeddingClient;
import com.localrag.common.ollama.OllamaChatClient;
import com.localrag.common.weaviate.WeaviateClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RetrievalServiceTests {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
    private final OllamaChatClient ollamaChatClient = mock(OllamaChatClient.class);
    private final WeaviateClient weaviateClient = mock(WeaviateClient.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final RetrievalSettings settings = defaultSettings();
    private final RetrievalService retrievalService = new RetrievalService(
            embeddingClient,
            ollamaChatClient,
            weaviateClient,
            jdbcTemplate,
            settings,
            LocalRerankerClient.disabled()
    );

    @Test
    void rejectsUnknownProjectBeforeSearchExecution() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("missing-project"))).thenReturn(0);

        assertThatThrownBy(() -> retrievalService.search(new SearchRequest(
                "missing-project",
                "query",
                5,
                "hybrid",
                null,
                null,
                null
        )))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).contains("Unknown projectId");
                });

        verifyNoInteractions(embeddingClient, weaviateClient);
    }

    @Test
    void rejectsUnsupportedModeBeforeSearchExecution() {
        assertThatThrownBy(() -> retrievalService.search(new SearchRequest(
                null,
                "query",
                5,
                "semantic-only",
                null,
                null,
                null
        )))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).contains("Unsupported search mode");
                });

        verifyNoInteractions(embeddingClient, weaviateClient);
    }

    @Test
    void parsesDocumentAuthorityMetadataFromWeaviateRows() throws Exception {
        var response = OBJECT_MAPPER.readTree("""
                {
                  "data": {
                    "Get": {
                      "LocalRagChunk": [
                        {
                          "chunkId": "chunk-1",
                          "documentId": "doc-1",
                          "projectId": "local-rag-system",
                          "sourceId": "local-rag-system.docs",
                          "sourceType": "project-docs",
                          "ssotRole": "project-current-truth",
                          "relativePath": "design/retrieval-quality-improvement-design.md",
                          "fileName": "retrieval-quality-improvement-design.md",
                          "folder": "design",
                          "extension": "md",
                          "title": "retrieval-quality-improvement-design",
                          "docType": "design",
                          "frontmatterStatus": "current",
                          "authority": "canonical",
                          "updated": "2026-05-30T00:00:00Z",
                          "supersedes": ["old-design.md"],
                          "supersededBy": [],
                          "headingPath": "retrieval-quality-improvement-design > Chunking Improvement",
                          "headingPathSegments": ["retrieval-quality-improvement-design", "Chunking Improvement"],
                          "headingDepth": 2,
                          "headingSlug": "retrieval-quality-improvement-design-chunking-improvement",
                          "chunkContext": "Title: retrieval-quality-improvement-design",
                          "contentHash": "hash-1",
                          "sensitivity": "private",
                          "tags": ["retrieval-quality"],
                          "links": ["docs/tasks/T0013-retrieval-chunking-and-document-authority-hardening.md"],
                          "content": "Title: retrieval-quality-improvement-design\\n\\nChunk text",
                          "_additional": {"score": "0.8"}
                        }
                      ]
                    }
                  }
                }
                """);

        List<RetrievalRanker.RankCandidate> candidates = RetrievalService.parseCandidates(response);

        assertThat(candidates).hasSize(1);
        SearchResultItem item = candidates.get(0).item();
        assertThat(item.citation()).isEqualTo("design/retrieval-quality-improvement-design.md#retrieval-quality-improvement-design-chunking-improvement");
        assertThat(item.metadata())
                .containsEntry("title", "retrieval-quality-improvement-design")
                .containsEntry("docType", "design")
                .containsEntry("frontmatterStatus", "current")
                .containsEntry("authority", "canonical")
                .containsEntry("headingDepth", 2);
        assertThat(item.metadata())
                .containsEntry("supersedes", List.of("old-design.md"))
                .containsEntry("headingPathSegments", List.of("retrieval-quality-improvement-design", "Chunking Improvement"));
    }

    @Test
    void buildsWhereClauseWithSupportedMetadataFilters() {
        SearchRequest request = new SearchRequest(
                "local-rag-system",
                "query",
                5,
                "hybrid",
                null,
                null,
                Map.of(
                        "status", List.of("current", "active"),
                        "authority", List.of("canonical"),
                        "docType", List.of("design"),
                        "includeHistorical", List.of("true"),
                        "unknownFilter", List.of("ignored")
                )
        );

        String where = RetrievalService.buildWhere(request, List.of("local-rag-system.docs"));

        assertThat(where).contains("path:[\"sourceId\"]", "local-rag-system.docs");
        assertThat(where).contains("path:[\"frontmatterStatus\"]", "current", "active");
        assertThat(where).contains("path:[\"authority\"]", "canonical");
        assertThat(where).contains("path:[\"docType\"]", "design");
        assertThat(where).doesNotContain("includeHistorical", "unknownFilter");
    }

    @Test
    void retrievalQuerySeparatesTaskIdFromKoreanSuffix() {
        assertThat(RetrievalService.retrievalQuery("T0014의 목적은 무엇인가?"))
                .isEqualTo("T0014 의 목적은 무엇인가?");
    }

    @Test
    void scoreGateDebugAnnotatesWithoutChangingTopKSelection() throws Exception {
        RetrievalService service = new RetrievalService(
                embeddingClient,
                ollamaChatClient,
                weaviateClient,
                jdbcTemplate,
                settings,
                fakeReranker()
        );
        when(jdbcTemplate.query(
                startsWith("SELECT source_id FROM source_root WHERE active ORDER BY priority DESC"),
                any(RowMapper.class)
        )).thenReturn(List.of());
        when(weaviateClient.graphQl(anyString())).thenReturn(scoreGateCandidateResponse());

        var response = service.search(new SearchRequest(
                null,
                "배포 릴리즈 기록 어디 남기지?",
                1,
                "keyword",
                null,
                null,
                Map.of("scoreGate", List.of("debug"))
        ));

        assertThat(response.results()).hasSize(1);
        SearchResultItem top = response.results().get(0);
        assertThat(top.chunkId()).isEqualTo("chunk-vector-top");
        assertThat(top.score())
                .containsEntry("scoreGateMode", "debug")
                .containsEntry("scoreGateApplied", true)
                .containsEntry("scoreGateBucket", "B2")
                .containsEntry("scoreGateRetained", false);
    }

    @Test
    void scoreGateOnUsesCrossEncoderScoreToRescueB3Candidate() throws Exception {
        RetrievalService service = new RetrievalService(
                embeddingClient,
                ollamaChatClient,
                weaviateClient,
                jdbcTemplate,
                settings,
                fakeReranker()
        );
        when(jdbcTemplate.query(
                startsWith("SELECT source_id FROM source_root WHERE active ORDER BY priority DESC"),
                any(RowMapper.class)
        )).thenReturn(List.of());
        when(weaviateClient.graphQl(anyString())).thenReturn(scoreGateCandidateResponse());

        var response = service.search(new SearchRequest(
                null,
                "배포 릴리즈 기록 어디 남기지?",
                1,
                "keyword",
                null,
                null,
                Map.of("scoreGate", List.of("on"))
        ));

        assertThat(response.results()).hasSize(1);
        SearchResultItem top = response.results().get(0);
        assertThat(top.chunkId()).isEqualTo("chunk-b3-rescue");
        assertThat(top.score())
                .containsEntry("scoreGateMode", "on")
                .containsEntry("scoreGateApplied", true)
                .containsEntry("scoreGateBucket", "B3")
                .containsEntry("scoreGateRetained", true)
                .containsEntry("crossEncoderScore", 0.90);
    }

    @Test
    void answerPromptIncludesSourcePriorityMetadata() {
        SearchResultItem result = new SearchResultItem(
                "chunk-1",
                "doc-1",
                "local-rag-system",
                "local-rag-system.docs",
                "project-current-truth",
                "tasks/T0014-search-filter-and-answer-context-governance.md",
                "T0014 > Purpose",
                "tasks/T0014-search-filter-and-answer-context-governance.md#purpose",
                "Filters should use metadata.",
                Map.of(
                        "title", "search-filter-and-answer-context-governance",
                        "docType", "task",
                        "frontmatterStatus", "active",
                        "authority", "canonical",
                        "updated", "2026-05-30T00:00:00Z",
                        "supersededBy", List.of()
                ),
                Map.of()
        );

        String prompt = RetrievalService.answerPrompt("How should filters work?", List.of(result));

        assertThat(prompt).contains(
                "Source priority:",
                "title=search-filter-and-answer-context-governance",
                "sourceId=local-rag-system.docs",
                "ssotRole=project-current-truth",
                "docType=task",
                "status=active",
                "authority=canonical",
                "updated=2026-05-30T00:00:00Z",
                "Snippet: Filters should use metadata."
        );
    }

    @Test
    void sourceDistributionCountsFinalResultsBySourceId() {
        List<SearchResultItem> results = List.of(
                resultFrom("local-rag-system.docs", "tasks/T0016-retrieval-audit-observability-expansion.md"),
                resultFrom("local-rag-system.docs", "docs/design/retrieval-quality-improvement-design.md"),
                resultFrom("support-notes.wiki", "knowledge.md")
        );

        assertThat(RetrievalService.sourceDistribution(results))
                .containsEntry("local-rag-system.docs", 2)
                .containsEntry("support-notes.wiki", 1);
    }

    private static SearchResultItem resultFrom(String sourceId, String relativePath) {
        return new SearchResultItem(
                sourceId + ":" + relativePath,
                sourceId + ":doc",
                "local-rag-system",
                sourceId,
                "project-current-truth",
                relativePath,
                "",
                relativePath,
                "",
                Map.of(),
                Map.of("rerankScore", 1.0)
        );
    }

    private static RetrievalSettings defaultSettings() {
        return new RetrievalSettings(
                false,
                false,
                "",
                Duration.ofMillis(500),
                Duration.ofSeconds(5),
                false,
                0.70,
                0.08,
                0.30,
                0.255,
                0.15,
                10
        );
    }

    private static LocalRerankerClient fakeReranker() {
        return (query, candidates) -> {
            Map<String, Double> scores = new LinkedHashMap<>();
            scores.put("chunk-vector-top", 0.01);
            scores.put("chunk-b3-rescue", 0.90);
            return LocalRerankerClient.RerankOutcome.success("fake-cross-encoder", scores, 7);
        };
    }

    private static com.fasterxml.jackson.databind.JsonNode scoreGateCandidateResponse() throws Exception {
        return OBJECT_MAPPER.readTree("""
                {
                  "data": {
                    "Get": {
                      "LocalRagChunk": [
                        {
                          "chunkId": "chunk-vector-top",
                          "documentId": "doc-1",
                          "projectId": "local-rag-system",
                          "sourceId": "local-rag-system.docs",
                          "sourceType": "project-docs",
                          "ssotRole": "project-current-truth",
                          "relativePath": "docs/tasks/T9999-unrelated.md",
                          "fileName": "T9999-unrelated.md",
                          "folder": "docs/tasks",
                          "extension": "md",
                          "title": "unrelated",
                          "docType": "task",
                          "frontmatterStatus": "current",
                          "authority": "canonical",
                          "updated": "2026-06-16T00:00:00Z",
                          "supersedes": [],
                          "supersededBy": [],
                          "headingPath": "unrelated",
                          "headingPathSegments": ["unrelated"],
                          "headingDepth": 1,
                          "headingSlug": "unrelated",
                          "chunkContext": "Title: unrelated",
                          "contentHash": "hash-1",
                          "sensitivity": "private",
                          "tags": [],
                          "links": [],
                          "content": "Generic deployment text that should rank high by first-stage score only.",
                          "_additional": {"score": "0.80"}
                        },
                        {
                          "chunkId": "chunk-b3-rescue",
                          "documentId": "doc-2",
                          "projectId": "local-rag-system",
                          "sourceId": "local-rag-system.docs",
                          "sourceType": "project-docs",
                          "ssotRole": "project-current-truth",
                          "relativePath": "docs/design/retrieval-quality-improvement-design.md",
                          "fileName": "retrieval-quality-improvement-design.md",
                          "folder": "docs/design",
                          "extension": "md",
                          "title": "retrieval-quality-improvement-design",
                          "docType": "design",
                          "frontmatterStatus": "current",
                          "authority": "canonical",
                          "updated": "2026-06-16T00:00:00Z",
                          "supersedes": [],
                          "supersededBy": [],
                          "headingPath": "ScoreGate Adaptive Context Selection",
                          "headingPathSegments": ["ScoreGate Adaptive Context Selection"],
                          "headingDepth": 2,
                          "headingSlug": "scoregate-adaptive-context-selection",
                          "chunkContext": "release registry and deployment note guidance",
                          "contentHash": "hash-2",
                          "sensitivity": "private",
                          "tags": ["scoregate"],
                          "links": [],
                          "content": "Release registry entries and deployment notes record where deployed changes are tracked.",
                          "_additional": {"score": "0.20"}
                        }
                      ]
                    }
                  }
                }
                """);
    }
}
