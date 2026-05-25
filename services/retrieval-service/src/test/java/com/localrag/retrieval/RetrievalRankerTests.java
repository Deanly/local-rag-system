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
                "crypto-bot",
                "크립토봇 사실 권한 리서치 표면 설계",
                5,
                "hybrid",
                null,
                null,
                null
        );
        RetrievalRanker.RankContext context = new RetrievalRanker.RankContext(
                "crypto-bot",
                "crypto-bot.docs",
                Set.of("worknote.wiki"),
                Map.of(
                        "crypto-bot.docs", new RetrievalRanker.SourceInfo(
                                "crypto-bot.docs",
                                "crypto-bot",
                                "docs",
                                "project-current-truth",
                                100
                        ),
                        "worknote.wiki", new RetrievalRanker.SourceInfo(
                                "worknote.wiki",
                                "worknote",
                                "compiled-wiki",
                                "cross-project-support",
                                80
                        )
                )
        );
        List<RetrievalRanker.RankCandidate> candidates = List.of(
                candidate("support-1", "support-doc", "worknote", "worknote.wiki", "lint/2026-05-14-backbone-promotion-batch-52.md", 0.90, 0),
                candidate("primary-1", "primary-doc", "crypto-bot", "crypto-bot.docs", "design/fact-authority-and-research-surface.md", 0.55, 1)
        );

        List<SearchResultItem> results = RetrievalRanker.rank(request, context, candidates, 5);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).sourceId()).isEqualTo("crypto-bot.docs");
        assertThat(results.get(0).relativePath()).isEqualTo("design/fact-authority-and-research-surface.md");
        assertThat(results.get(0).score()).containsKeys(
                "baseScore",
                "sourceWeight",
                "pathWeight",
                "matchWeight",
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
                Set.of("worknote.wiki"),
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

    private static RetrievalRanker.RankCandidate candidate(
            String chunkId,
            String documentId,
            String projectId,
            String sourceId,
            String relativePath,
            double score,
            int rawRank
    ) {
        SearchResultItem item = new SearchResultItem(
                chunkId,
                documentId,
                projectId,
                sourceId,
                "project-current-truth",
                relativePath,
                "",
                relativePath,
                "",
                Map.of("score", score)
        );
        return new RetrievalRanker.RankCandidate(item, chunkId + "-hash", rawRank);
    }
}
