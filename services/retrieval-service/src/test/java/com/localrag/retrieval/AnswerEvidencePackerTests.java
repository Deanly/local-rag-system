package com.localrag.retrieval;

import com.localrag.common.dto.SearchResultItem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerEvidencePackerTests {

    @Test
    void packOrdersEvidenceByQueryOverlapAndDropsUngroundedCandidates() {
        SearchResultItem distractor = result("chunk-1", "guide/ports.md#gateway", "127.0.0.1:42120 accepts local client requests.");
        SearchResultItem weakEvidence = result("chunk-2", "guide/operations.md#rescan", "Trigger a rescan after fixing the source document.");
        SearchResultItem strongEvidence = result("chunk-3", "guide/indexing.md#frontmatter", "Malformed YAML frontmatter aborts the source scan for that document.");

        AnswerEvidencePacker.Packed packed = AnswerEvidencePacker.pack(
                "How does malformed frontmatter affect the source scan?",
                List.of(distractor, weakEvidence, strongEvidence),
                AnswerEvidencePacker.Config.defaults()
        );

        assertThat(packed.lexicallyGrounded()).isTrue();
        assertThat(packed.selected()).extracting(evidence -> evidence.item().citation())
                .containsExactly("guide/indexing.md#frontmatter", "guide/operations.md#rescan");
        assertThat(packed.selected().get(0).groundingScore()).isGreaterThan(packed.selected().get(1).groundingScore());
    }

    @Test
    void packKeepsRetrievalOrderWhenNoCandidateMatchesTheQuery() {
        SearchResultItem first = result("chunk-1", "a.md#x", "Weaviate stores chunk vectors.");
        SearchResultItem second = result("chunk-2", "b.md#y", "PostgreSQL stores registry state.");

        AnswerEvidencePacker.Packed packed = AnswerEvidencePacker.pack(
                "완전히 무관한 질문",
                List.of(first, second),
                AnswerEvidencePacker.Config.defaults()
        );

        assertThat(packed.lexicallyGrounded()).isFalse();
        assertThat(packed.selected()).extracting(evidence -> evidence.item().chunkId())
                .containsExactly("chunk-1", "chunk-2");
    }

    @Test
    void packEnforcesEvidenceCountAndCharBudget() {
        String longSnippet = "frontmatter scan audit ".repeat(30).trim();
        List<SearchResultItem> results = List.of(
                result("chunk-1", "a.md#1", longSnippet + " one"),
                result("chunk-2", "b.md#2", longSnippet + " two"),
                result("chunk-3", "c.md#3", longSnippet + " three"),
                result("chunk-4", "d.md#4", longSnippet + " four")
        );

        AnswerEvidencePacker.Packed packed = AnswerEvidencePacker.pack(
                "frontmatter scan audit",
                results,
                new AnswerEvidencePacker.Config(1500, 6)
        );

        assertThat(packed.replaySnippetChars()).isLessThanOrEqualTo(1500);
        assertThat(packed.selected().size()).isLessThan(results.size());
        assertThat(packed.selected()).isNotEmpty();
    }

    @Test
    void packAlwaysKeepsTheBestCandidateEvenWhenItExceedsTheBudget() {
        SearchResultItem oversized = result("chunk-1", "a.md#1", "frontmatter ".repeat(400).trim());

        AnswerEvidencePacker.Packed packed = AnswerEvidencePacker.pack(
                "frontmatter",
                List.of(oversized),
                new AnswerEvidencePacker.Config(100, 6)
        );

        assertThat(packed.selected()).hasSize(1);
    }

    @Test
    void packDeduplicatesByCitationAndNormalizedSnippet() {
        SearchResultItem first = result("chunk-1", "a.md#1", "Grounded evidence should be replayed.");
        SearchResultItem duplicate = result("chunk-2", "a.md#1", "Grounded   evidence should be replayed.");

        AnswerEvidencePacker.Packed packed = AnswerEvidencePacker.pack(
                "How is grounded evidence replayed?",
                List.of(first, duplicate),
                AnswerEvidencePacker.Config.defaults()
        );

        assertThat(packed.selected()).hasSize(1);
        assertThat(packed.candidateCount()).isEqualTo(1);
    }

    @Test
    void packMatchesKoreanQueryTermsByContainment() {
        SearchResultItem distractor = result("chunk-1", "a.md#1", "게이트웨이 포트는 42120이다.");
        SearchResultItem evidence = result("chunk-2", "b.md#2", "인덱서는 frontmatter 오류가 있는 문서를 건너뛰고 감사 레코드를 남긴다.");

        AnswerEvidencePacker.Packed packed = AnswerEvidencePacker.pack(
                "인덱서 frontmatter 오류 처리",
                List.of(distractor, evidence),
                AnswerEvidencePacker.Config.defaults()
        );

        assertThat(packed.lexicallyGrounded()).isTrue();
        assertThat(packed.selected().get(0).item().chunkId()).isEqualTo("chunk-2");
    }

    @Test
    void baselinePackReplaysAllDeduplicatedCandidatesInRetrievalOrder() {
        List<SearchResultItem> results = List.of(
                result("chunk-1", "a.md#1", "First snippet."),
                result("chunk-2", "b.md#2", "Second snippet."),
                result("chunk-3", "a.md#1", "First snippet.")
        );

        AnswerEvidencePacker.Packed packed = AnswerEvidencePacker.baselinePack(results);

        assertThat(packed.selected()).extracting(evidence -> evidence.item().chunkId())
                .containsExactly("chunk-1", "chunk-2");
        assertThat(packed.lexicallyGrounded()).isFalse();
    }

    @Test
    void packIgnoresBlankSnippets() {
        SearchResultItem blank = result("chunk-1", "a.md#1", "  ");
        SearchResultItem evidence = result("chunk-2", "b.md#2", "frontmatter handling");

        AnswerEvidencePacker.Packed packed = AnswerEvidencePacker.pack(
                "frontmatter",
                List.of(blank, evidence),
                AnswerEvidencePacker.Config.defaults()
        );

        assertThat(packed.selected()).hasSize(1);
        assertThat(packed.selected().get(0).item().chunkId()).isEqualTo("chunk-2");
    }

    private static SearchResultItem result(String chunkId, String citation, String snippet) {
        return new SearchResultItem(
                chunkId,
                chunkId + "-doc",
                "local-rag-system",
                "local-rag-system.docs",
                "project-current-truth",
                citation.contains("#") ? citation.substring(0, citation.indexOf('#')) : citation,
                "Heading",
                citation,
                snippet,
                Map.of("title", "fixture", "docType", "doc"),
                Map.of("rerankScore", 1.0)
        );
    }
}
