package com.localrag.retrieval;

import com.localrag.common.dto.SearchResultItem;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministic baseline-vs-ReContext grounding benchmark for the /api/answer replay block.
 *
 * Baseline arm = pre-T0026 behavior (deduplicated replay of every retrieved snippet in retrieval
 * order, no budget). ReContext arm = AnswerEvidencePacker query-aware budget-bounded selection.
 * Scoring is rule-based (expected-citation hits, distractor exclusion, replay size), so the
 * benchmark runs without any LLM call and cannot be flaky.
 *
 * Run: mvn -pl services/retrieval-service -am test -Dtest=AnswerGroundingBenchmarkTests
 * Report: services/retrieval-service/target/answer-grounding-benchmark.md
 */
class AnswerGroundingBenchmarkTests {

    private static final AnswerEvidencePacker.Config RECONTEXT_CONFIG = AnswerEvidencePacker.Config.defaults();

    record BenchmarkCase(
            String name,
            String query,
            List<SearchResultItem> results,
            Set<String> expectedCitations,
            Set<String> distractorCitations
    ) {
    }

    record ArmMetrics(
            String arm,
            double evidenceHitRate,
            double distractorExclusionRate,
            int firstEvidencePosition,
            int selectedCount,
            int replayChars,
            long packMicros
    ) {
    }

    @Test
    void recontextPackingBeatsBaselineOnGroundingProxiesAndStaysInBudget() throws IOException {
        List<BenchmarkCase> cases = benchmarkCases();
        StringBuilder report = new StringBuilder();
        report.append("# Answer Grounding Benchmark: baseline vs ReContext packing\n\n");
        report.append("Deterministic rule-based proxy metrics; no LLM in the loop.\n\n");
        report.append("| Case | Arm | Evidence hit | Distractor exclusion | First evidence pos | Selected | Replay chars | Pack us |\n");
        report.append("| --- | --- | --- | --- | --- | --- | --- | --- |\n");

        double baselineExclusionSum = 0;
        double recontextExclusionSum = 0;
        int baselineCharsSum = 0;
        int recontextCharsSum = 0;

        for (BenchmarkCase benchmarkCase : cases) {
            ArmMetrics baseline = measure("baseline", benchmarkCase, null);
            ArmMetrics recontext = measure("recontext", benchmarkCase, RECONTEXT_CONFIG);
            appendRow(report, benchmarkCase.name(), baseline);
            appendRow(report, benchmarkCase.name(), recontext);

            baselineExclusionSum += baseline.distractorExclusionRate();
            recontextExclusionSum += recontext.distractorExclusionRate();
            baselineCharsSum += baseline.replayChars();
            recontextCharsSum += recontext.replayChars();

            if (!benchmarkCase.expectedCitations().isEmpty()) {
                assertThat(recontext.evidenceHitRate())
                        .as("case %s: recontext must replay every expected evidence citation", benchmarkCase.name())
                        .isEqualTo(1.0);
                assertThat(recontext.evidenceHitRate())
                        .as("case %s: recontext hit rate must not regress below baseline", benchmarkCase.name())
                        .isGreaterThanOrEqualTo(baseline.evidenceHitRate());
            }
            assertThat(recontext.distractorExclusionRate())
                    .as("case %s: recontext must not include more distractors than baseline", benchmarkCase.name())
                    .isGreaterThanOrEqualTo(baseline.distractorExclusionRate());
            assertThat(recontext.replayChars())
                    .as("case %s: recontext replay must respect the char budget", benchmarkCase.name())
                    .isLessThanOrEqualTo(RECONTEXT_CONFIG.replayCharBudget());
            assertThat(recontext.selectedCount())
                    .as("case %s: recontext replay must respect the evidence count budget", benchmarkCase.name())
                    .isLessThanOrEqualTo(RECONTEXT_CONFIG.maxReplayEvidence());
        }

        double baselineExclusionAvg = baselineExclusionSum / cases.size();
        double recontextExclusionAvg = recontextExclusionSum / cases.size();

        report.append("\n## Summary\n\n");
        report.append("- Cases: ").append(cases.size()).append('\n');
        report.append("- Avg distractor exclusion: baseline ").append(format(baselineExclusionAvg))
                .append(" vs recontext ").append(format(recontextExclusionAvg)).append('\n');
        report.append("- Total replay chars: baseline ").append(baselineCharsSum)
                .append(" vs recontext ").append(recontextCharsSum).append('\n');
        report.append("- Recontext config: charBudget=").append(RECONTEXT_CONFIG.replayCharBudget())
                .append(", maxEvidence=").append(RECONTEXT_CONFIG.maxReplayEvidence()).append('\n');

        assertThat(recontextExclusionAvg)
                .as("recontext must exclude distractors better than replay-everything baseline on average")
                .isGreaterThan(baselineExclusionAvg);
        assertThat(recontextCharsSum)
                .as("recontext must use less total replay context than baseline")
                .isLessThan(baselineCharsSum);

        Path reportPath = Path.of("target", "answer-grounding-benchmark.md");
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, report.toString());
        System.out.println(report);
    }

    @Test
    void distractorLedRankingCaseReplaysTrueEvidenceFirst() {
        BenchmarkCase benchmarkCase = distractorLedRankingCase();
        ArmMetrics baseline = measure("baseline", benchmarkCase, null);
        ArmMetrics recontext = measure("recontext", benchmarkCase, RECONTEXT_CONFIG);

        assertThat(recontext.firstEvidencePosition())
                .as("recontext must lead the replay block with true evidence")
                .isEqualTo(1);
        assertThat(baseline.firstEvidencePosition())
                .as("baseline replays distractors first because it keeps retrieval order")
                .isGreaterThan(1);
    }

    private static ArmMetrics measure(String arm, BenchmarkCase benchmarkCase, AnswerEvidencePacker.Config config) {
        long started = System.nanoTime();
        AnswerEvidencePacker.Packed packed = config == null
                ? AnswerEvidencePacker.baselinePack(benchmarkCase.results())
                : AnswerEvidencePacker.pack(benchmarkCase.query(), benchmarkCase.results(), config);
        long packMicros = (System.nanoTime() - started) / 1_000;

        List<String> selectedCitations = packed.selected().stream()
                .map(evidence -> evidence.item().citation())
                .toList();
        Set<String> selectedSet = new LinkedHashSet<>(selectedCitations);

        double hitRate = benchmarkCase.expectedCitations().isEmpty()
                ? 1.0
                : (double) benchmarkCase.expectedCitations().stream().filter(selectedSet::contains).count()
                        / benchmarkCase.expectedCitations().size();
        double exclusionRate = benchmarkCase.distractorCitations().isEmpty()
                ? 1.0
                : 1.0 - (double) benchmarkCase.distractorCitations().stream().filter(selectedSet::contains).count()
                        / benchmarkCase.distractorCitations().size();
        int firstEvidencePosition = -1;
        for (int index = 0; index < selectedCitations.size(); index++) {
            if (benchmarkCase.expectedCitations().contains(selectedCitations.get(index))) {
                firstEvidencePosition = index + 1;
                break;
            }
        }
        return new ArmMetrics(
                arm,
                hitRate,
                exclusionRate,
                firstEvidencePosition,
                packed.selected().size(),
                packed.replaySnippetChars(),
                packMicros
        );
    }

    private static void appendRow(StringBuilder report, String caseName, ArmMetrics metrics) {
        report.append("| ").append(caseName)
                .append(" | ").append(metrics.arm())
                .append(" | ").append(format(metrics.evidenceHitRate()))
                .append(" | ").append(format(metrics.distractorExclusionRate()))
                .append(" | ").append(metrics.firstEvidencePosition())
                .append(" | ").append(metrics.selectedCount())
                .append(" | ").append(metrics.replayChars())
                .append(" | ").append(metrics.packMicros())
                .append(" |\n");
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static List<BenchmarkCase> benchmarkCases() {
        List<BenchmarkCase> cases = new ArrayList<>();
        cases.add(multiEvidenceLongQueryCase());
        cases.add(distractorLedRankingCase());
        cases.add(duplicateEvidenceCase());
        cases.add(koreanQueryCase());
        cases.add(budgetOverflowCase());
        cases.add(noLexicalOverlapFallbackCase());
        return cases;
    }

    private static BenchmarkCase multiEvidenceLongQueryCase() {
        String query = "How does the indexer handle malformed YAML frontmatter during a source scan,"
                + " and which audit record is written when the document is skipped?";
        List<SearchResultItem> results = List.of(
                item("d1", "guide/ports.md#gateway", "The api-gateway binds to 127.0.0.1:42120 and proxies MCP bridge calls."),
                item("e1", "design/indexing.md#frontmatter", "A document with malformed YAML frontmatter is skipped and the source scan continues with the remaining files."),
                item("d2", "guide/models.md#embedding", "The default embedding model is qwen3-embedding:4b served by local Ollama."),
                item("e2", "design/indexing.md#audit", "When the indexer skips a document it writes an index_audit record with reason frontmatter-parse-error."),
                item("d3", "design/storage.md#weaviate", "Weaviate stores chunk text with externally supplied vectors."),
                item("e3", "guide/operations.md#rescan", "After fixing frontmatter, trigger a rescan so the skipped document re-enters the scan queue."),
                item("d4", "design/storage.md#postgres", "PostgreSQL keeps registry, job, and failure state for the control plane."),
                item("d5", "guide/compose.md#profiles", "Docker Compose profiles separate runtime services from evaluation sidecars.")
        );
        return new BenchmarkCase(
                "multi-evidence-long-query",
                query,
                results,
                Set.of("design/indexing.md#frontmatter", "design/indexing.md#audit", "guide/operations.md#rescan"),
                Set.of("guide/ports.md#gateway", "guide/models.md#embedding", "design/storage.md#weaviate", "design/storage.md#postgres", "guide/compose.md#profiles")
        );
    }

    private static BenchmarkCase distractorLedRankingCase() {
        String query = "Which citation format does the answer endpoint use for replayed evidence?";
        List<SearchResultItem> results = List.of(
                item("d1", "design/search.md#modes", "Search modes are keyword, vector, and hybrid with a shared result shape."),
                item("d2", "design/search.md#limits", "Requested limit is clamped between the minimum and maximum candidate limits."),
                item("d3", "design/search.md#weighting", "Primary project sources outrank default context sources after first-stage retrieval."),
                item("e1", "design/answer.md#citation", "The answer endpoint labels replayed evidence with the citation in square brackets, the same format as retrieved context."),
                item("e2", "design/answer.md#replay", "Replayed evidence keeps the citation and source priority line so the model can cite it directly.")
        );
        return new BenchmarkCase(
                "distractor-led-ranking",
                query,
                results,
                Set.of("design/answer.md#citation", "design/answer.md#replay"),
                Set.of("design/search.md#modes", "design/search.md#limits", "design/search.md#weighting")
        );
    }

    private static BenchmarkCase duplicateEvidenceCase() {
        String query = "How are duplicate snippets handled in the grounded evidence replay block?";
        List<SearchResultItem> results = List.of(
                item("e1", "design/answer.md#dedupe", "Duplicate snippets are collapsed by citation and whitespace-normalized text before evidence replay."),
                item("e2", "design/answer.md#dedupe", "Duplicate   snippets are collapsed by citation and whitespace-normalized text before evidence replay."),
                item("d1", "guide/backup.md#schedule", "Nightly backups copy the PostgreSQL volume to the archive disk.")
        );
        return new BenchmarkCase(
                "duplicate-evidence",
                query,
                results,
                Set.of("design/answer.md#dedupe"),
                Set.of("guide/backup.md#schedule")
        );
    }

    private static BenchmarkCase koreanQueryCase() {
        String query = "인덱서가 frontmatter 오류 문서를 어떻게 처리하고 어떤 감사 기록을 남기나요?";
        List<SearchResultItem> results = List.of(
                item("d1", "guide/ports.md#gateway", "게이트웨이는 127.0.0.1:42120에서 요청을 받는다."),
                item("e1", "design/indexing.md#frontmatter-ko", "인덱서는 frontmatter 오류가 있는 문서를 건너뛰고 나머지 파일 스캔을 계속한다."),
                item("e2", "design/indexing.md#audit-ko", "건너뛴 문서에 대해 frontmatter-parse-error 사유의 감사 기록을 남긴다."),
                item("d2", "guide/models.md#chat", "채팅 모델은 로컬 Ollama에서 제공된다.")
        );
        return new BenchmarkCase(
                "korean-query",
                query,
                results,
                Set.of("design/indexing.md#frontmatter-ko", "design/indexing.md#audit-ko"),
                Set.of("guide/ports.md#gateway", "guide/models.md#chat")
        );
    }

    private static BenchmarkCase budgetOverflowCase() {
        String filler = " The section also restates general operational guidance about local-only deployment,"
                + " read-only source mounts, and citation-bearing hybrid search that applies to every service"
                + " in the compose stack regardless of the question being asked here.";
        String query = "What triggers a stale chunk delete during incremental reindexing?";
        List<SearchResultItem> results = List.of(
                item("e1", "design/indexing.md#stale", "A stale chunk delete is triggered when the source file is removed or its content hash changes during incremental reindexing." + filler),
                item("d1", "design/storage.md#weaviate", "Weaviate keeps one object per chunk with citation metadata." + filler),
                item("e2", "design/indexing.md#hash", "Incremental reindexing compares stored content hashes to decide which documents changed." + filler),
                item("d2", "guide/compose.md#volumes", "Source folders are mounted read-only into the indexer container." + filler),
                item("d3", "guide/models.md#embedding", "Embedding batches are limited to keep local Ollama memory stable." + filler),
                item("d4", "design/search.md#hybrid", "Hybrid search fuses keyword and vector scores." + filler)
        );
        return new BenchmarkCase(
                "budget-overflow",
                query,
                results,
                Set.of("design/indexing.md#stale", "design/indexing.md#hash"),
                Set.of("design/storage.md#weaviate", "guide/compose.md#volumes", "guide/models.md#embedding", "design/search.md#hybrid")
        );
    }

    private static BenchmarkCase noLexicalOverlapFallbackCase() {
        String query = "zzz qqq xxx";
        List<SearchResultItem> results = List.of(
                item("f1", "a.md#1", "Weaviate stores chunk vectors."),
                item("f2", "b.md#2", "PostgreSQL stores registry state.")
        );
        return new BenchmarkCase(
                "no-lexical-overlap-fallback",
                query,
                results,
                Set.of(),
                Set.of()
        );
    }

    private static SearchResultItem item(String chunkId, String citation, String snippet) {
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
