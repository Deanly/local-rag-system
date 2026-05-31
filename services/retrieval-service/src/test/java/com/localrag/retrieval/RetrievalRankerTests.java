package com.localrag.retrieval;

import com.localrag.common.dto.SearchRequest;
import com.localrag.common.dto.SearchResultItem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalRankerTests {
    @Test
    void promotesPrimarySourceOverSupportContext() {
        SearchRequest request = new SearchRequest(
                "project-alpha",
                "프로젝트 알파 current truth 설계",
                5,
                "hybrid",
                null,
                null,
                null
        );
        RetrievalRanker.RankContext context = new RetrievalRanker.RankContext(
                "project-alpha",
                "project-alpha.docs",
                Set.of("support-notes.wiki"),
                Map.of(
                        "project-alpha.docs", new RetrievalRanker.SourceInfo(
                                "project-alpha.docs",
                                "project-alpha",
                                "docs",
                                "project-current-truth",
                                100
                        ),
                        "support-notes.wiki", new RetrievalRanker.SourceInfo(
                                "support-notes.wiki",
                                "support-notes",
                                "compiled-wiki",
                                "cross-project-support",
                                80
                        )
                )
        );
        List<RetrievalRanker.RankCandidate> candidates = List.of(
                candidate("support-1", "support-doc", "support-notes", "support-notes.wiki", "lint/2026-05-14-project-alpha-summary.md", 0.90, 0),
                candidate("primary-1", "primary-doc", "project-alpha", "project-alpha.docs", "design/current-truth-design.md", 0.55, 1)
        );

        List<SearchResultItem> results = RetrievalRanker.rank(request, context, candidates, 5);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).sourceId()).isEqualTo("project-alpha.docs");
        assertThat(results.get(0).relativePath()).isEqualTo("design/current-truth-design.md");
        assertThat(results.get(0).score()).containsKeys(
                "baseScore",
                "sourceWeight",
                "pathWeight",
                "matchWeight",
                "governanceWeight",
                "rerankScore",
                "rawCandidateCount",
                "finalResultCount"
        );
    }

    @Test
    void limitsRepeatedChunksFromSameDocumentInTopWindow() {
        SearchRequest request = new SearchRequest(
                "local-rag-system",
                "source registry project ssot registered source root write policy",
                5,
                "hybrid",
                null,
                null,
                null
        );
        RetrievalRanker.RankContext context = new RetrievalRanker.RankContext(
                "local-rag-system",
                "local-rag-system.docs",
                Set.of("support-notes.wiki"),
                Map.of("local-rag-system.docs", new RetrievalRanker.SourceInfo(
                        "local-rag-system.docs",
                        "local-rag-system",
                        "docs",
                        "project-current-truth",
                        100
                ))
        );
        List<RetrievalRanker.RankCandidate> candidates = List.of(
                candidate("chunk-1", "doc-a", "local-rag-system", "local-rag-system.docs", "design/source-registry-and-project-ssot.md", 0.95, 0),
                candidate("chunk-2", "doc-a", "local-rag-system", "local-rag-system.docs", "design/source-registry-and-project-ssot.md", 0.94, 1),
                candidate("chunk-3", "doc-a", "local-rag-system", "local-rag-system.docs", "design/source-registry-and-project-ssot.md", 0.93, 2),
                candidate("chunk-4", "doc-b", "local-rag-system", "local-rag-system.docs", "design/local-rag-system-development-direction.md", 0.80, 3),
                candidate("chunk-5", "doc-c", "local-rag-system", "local-rag-system.docs", "tasks/T0011-retrieval-quality-hardening.md", 0.70, 4)
        );

        List<SearchResultItem> results = RetrievalRanker.rank(request, context, candidates, 5);

        assertThat(results).hasSize(4);
        assertThat(results.stream()
                .filter(result -> result.documentId().equals("doc-a"))
                .count()).isLessThanOrEqualTo(2);
        assertThat(results.stream()
                .map(SearchResultItem::documentId)
                .distinct()
                .count()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void demotesDeprecatedAndSupersededSourcesByDefault() {
        SearchRequest request = new SearchRequest(
                "local-rag-system",
                "retrieval governance current policy",
                5,
                "hybrid",
                null,
                null,
                null
        );
        RetrievalRanker.RankContext context = new RetrievalRanker.RankContext(
                "local-rag-system",
                "local-rag-system.docs",
                Set.of(),
                Map.of("local-rag-system.docs", new RetrievalRanker.SourceInfo(
                        "local-rag-system.docs",
                        "local-rag-system",
                        "project-docs",
                        "project-current-truth",
                        100
                ))
        );

        List<SearchResultItem> results = RetrievalRanker.rank(request, context, List.of(
                candidate("stale-1", "doc-old", "local-rag-system", "local-rag-system.docs", "guide/old-policy.md", 0.99, 0,
                        Map.of("frontmatterStatus", "deprecated", "authority", "raw", "supersededBy", List.of("guide/current-policy.md"))),
                candidate("current-1", "doc-new", "local-rag-system", "local-rag-system.docs", "guide/current-policy.md", 0.60, 1,
                        Map.of("frontmatterStatus", "current", "authority", "canonical"))
        ), 5);

        assertThat(results.get(0).relativePath()).isEqualTo("guide/current-policy.md");
        assertThat((double) results.get(0).score().get("governanceWeight")).isPositive();
        assertThat((double) results.get(1).score().get("governanceWeight")).isLessThan(-1.0);
    }

    @Test
    void historicalOptInAllowsDeprecatedSourcesToCompete() {
        SearchRequest request = new SearchRequest(
                "local-rag-system",
                "retrieval governance migration history",
                5,
                "hybrid",
                null,
                null,
                Map.of("includeHistorical", List.of("true"))
        );
        RetrievalRanker.RankContext context = new RetrievalRanker.RankContext(
                "local-rag-system",
                "local-rag-system.docs",
                Set.of(),
                Map.of("local-rag-system.docs", new RetrievalRanker.SourceInfo(
                        "local-rag-system.docs",
                        "local-rag-system",
                        "project-docs",
                        "project-current-truth",
                        100
                ))
        );

        List<SearchResultItem> results = RetrievalRanker.rank(request, context, List.of(
                candidate("stale-1", "doc-old", "local-rag-system", "local-rag-system.docs", "guide/old-policy.md", 1.00, 0,
                        Map.of("frontmatterStatus", "deprecated", "authority", "reference", "supersededBy", List.of("guide/current-policy.md"))),
                candidate("current-1", "doc-new", "local-rag-system", "local-rag-system.docs", "guide/current-policy.md", 0.50, 1,
                        Map.of("frontmatterStatus", "current", "authority", "canonical"))
        ), 5);

        assertThat(results.get(0).relativePath()).isEqualTo("guide/old-policy.md");
        assertThat((double) results.get(0).score().get("governanceWeight")).isEqualTo(0.0);
    }

    @Test
    void koreanRetrievalQualityQueryPrefersDomainDesignOverTermGlossary() {
        SearchRequest request = new SearchRequest(
                "local-rag-system",
                "검색 품질 평가 하네스와 deterministic rerank 설계",
                5,
                "hybrid",
                null,
                null,
                null
        );
        RetrievalRanker.RankContext context = new RetrievalRanker.RankContext(
                "local-rag-system",
                "local-rag-system.docs",
                Set.of(),
                Map.of("local-rag-system.docs", new RetrievalRanker.SourceInfo(
                        "local-rag-system.docs",
                        "local-rag-system",
                        "project-docs",
                        "project-current-truth",
                        100
                ))
        );

        List<SearchResultItem> results = RetrievalRanker.rank(request, context, List.of(
                candidate("term-1", "doc-glossary", "local-rag-system", "local-rag-system.docs",
                        "design/ubiquitous-language.md", "Retrieval Terms > rerank", 0.63, 0,
                        Map.of("frontmatterStatus", "current")),
                candidate("design-1", "doc-design", "local-rag-system", "local-rag-system.docs",
                        "design/retrieval-quality-improvement-design.md",
                        "Reranking Architecture > Stage 1: Deterministic Local Rerank",
                        0.52, 1,
                        Map.of("frontmatterStatus", "current"))
        ), 5);

        assertThat(results.get(0).relativePath()).isEqualTo("design/retrieval-quality-improvement-design.md");
        assertThat((double) results.get(0).score().get("pathWeight")).isGreaterThan(0.20);
    }

    private static RetrievalRanker.RankCandidate candidate(
            String chunkId,
            String documentId,
            String projectId,
            String sourceId,
            String relativePath,
            double score,
            int rawRank
    ) {
        return candidate(chunkId, documentId, projectId, sourceId, relativePath, "", score, rawRank, Map.of());
    }

    private static RetrievalRanker.RankCandidate candidate(
            String chunkId,
            String documentId,
            String projectId,
            String sourceId,
            String relativePath,
            double score,
            int rawRank,
            Map<String, Object> metadata
    ) {
        return candidate(chunkId, documentId, projectId, sourceId, relativePath, "", score, rawRank, metadata);
    }

    private static RetrievalRanker.RankCandidate candidate(
            String chunkId,
            String documentId,
            String projectId,
            String sourceId,
            String relativePath,
            String headingPath,
            double score,
            int rawRank,
            Map<String, Object> metadata
    ) {
        SearchResultItem item = new SearchResultItem(
                chunkId,
                documentId,
                projectId,
                sourceId,
                "project-current-truth",
                relativePath,
                headingPath,
                relativePath,
                "",
                metadata,
                Map.of("score", score)
        );
        return new RetrievalRanker.RankCandidate(item, chunkId + "-hash", rawRank);
    }
}
