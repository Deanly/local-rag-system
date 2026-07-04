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
 * Deterministic decision-grade grounding benchmark for the /api/answer replay block.
 *
 * Arms:
 * - baseline arm = T0025 replay-everything behavior (deduplicated replay of every retrieved
 *   snippet in retrieval order, no budget). This is the pre-packer replay behavior; the true
 *   pre-candidate production prompt on origin/main has no replay block at all, so replay chars
 *   in either arm are the additive prompt overhead versus main.
 * - recontext arm = AnswerEvidencePacker query-aware budget-bounded selection (T0026 candidate).
 *
 * Scoring is rule-based (expected-citation hits, distractor exclusion, replay size), so the
 * benchmark runs without any LLM call and cannot be flaky.
 *
 * Case categories: direct fact, distractor-heavy, multi-evidence, distractor-led ranking,
 * long/noisy context, Korean query, bilingual evidence, near-duplicate dedupe, conflicting
 * near-duplicate evidence, budget overflow, no-results insufficient evidence, no-lexical-overlap
 * fallback, paraphrase-drop regression risk (expected packer miss, kept honest), and
 * lexical-trap survival.
 *
 * Decision thresholds asserted here:
 * - T1: recontext replays every expected citation in all cases not marked as regression risk.
 * - T2: macro-average recontext evidence hit >= 0.90 including regression-risk cases.
 * - T3: macro-average distractor exclusion improves by >= 0.30 absolute over baseline.
 * - T4: total replay chars shrink by >= 20% versus baseline.
 * - T5: replay char budget (1600) and evidence count budget (6) hold in every case.
 *
 * Run: mvn -pl services/retrieval-service -am test -Dtest=AnswerGroundingBenchmarkTests
 * Report: services/retrieval-service/target/answer-grounding-benchmark.md
 */
class AnswerGroundingBenchmarkTests {

    private static final AnswerEvidencePacker.Config RECONTEXT_CONFIG = AnswerEvidencePacker.Config.defaults();
    private static final double MACRO_HIT_THRESHOLD = 0.90;
    private static final double EXCLUSION_IMPROVEMENT_THRESHOLD = 0.30;
    private static final double CHARS_REDUCTION_THRESHOLD = 0.20;

    record BenchmarkCase(
            String name,
            String category,
            String query,
            List<SearchResultItem> results,
            Set<String> expectedCitations,
            Set<String> distractorCitations,
            boolean expectRecontextHit
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
    void recontextPackingMeetsDecisionThresholdsAcrossRepresentativeCases() throws IOException {
        long benchmarkStarted = System.nanoTime();
        List<BenchmarkCase> cases = benchmarkCases();
        assertThat(cases.size()).isGreaterThanOrEqualTo(12);

        StringBuilder report = new StringBuilder();
        report.append("# Answer Grounding Benchmark: baseline (replay-all) vs ReContext packing\n\n");
        report.append("Deterministic rule-based proxy metrics; no LLM in the loop.\n");
        report.append("Baseline arm reproduces the T0025 replay-everything block; the origin/main pre-candidate prompt has no replay block (replay overhead 0, no evidence emphasis).\n\n");
        report.append("| Case | Category | Arm | Evidence hit | Distractor exclusion | First evidence pos | Selected | Replay chars | Pack us |\n");
        report.append("| --- | --- | --- | --- | --- | --- | --- | --- | --- |\n");

        double baselineHitSum = 0;
        double recontextHitSum = 0;
        double baselineExclusionSum = 0;
        double recontextExclusionSum = 0;
        int baselineCharsSum = 0;
        int recontextCharsSum = 0;

        for (BenchmarkCase benchmarkCase : cases) {
            ArmMetrics baseline = measure("baseline", benchmarkCase, null);
            ArmMetrics recontext = measure("recontext", benchmarkCase, RECONTEXT_CONFIG);
            appendRow(report, benchmarkCase, baseline);
            appendRow(report, benchmarkCase, recontext);

            baselineHitSum += baseline.evidenceHitRate();
            recontextHitSum += recontext.evidenceHitRate();
            baselineExclusionSum += baseline.distractorExclusionRate();
            recontextExclusionSum += recontext.distractorExclusionRate();
            baselineCharsSum += baseline.replayChars();
            recontextCharsSum += recontext.replayChars();

            if (benchmarkCase.expectRecontextHit() && !benchmarkCase.expectedCitations().isEmpty()) {
                assertThat(recontext.evidenceHitRate())
                        .as("T1 case %s: recontext must replay every expected evidence citation", benchmarkCase.name())
                        .isEqualTo(1.0);
                assertThat(recontext.evidenceHitRate())
                        .as("T1 case %s: recontext hit rate must not regress below baseline", benchmarkCase.name())
                        .isGreaterThanOrEqualTo(baseline.evidenceHitRate());
            }
            assertThat(recontext.distractorExclusionRate())
                    .as("case %s: recontext must not include more distractors than baseline", benchmarkCase.name())
                    .isGreaterThanOrEqualTo(baseline.distractorExclusionRate());
            assertThat(recontext.replayChars())
                    .as("T5 case %s: recontext replay must respect the char budget", benchmarkCase.name())
                    .isLessThanOrEqualTo(RECONTEXT_CONFIG.replayCharBudget());
            assertThat(recontext.selectedCount())
                    .as("T5 case %s: recontext replay must respect the evidence count budget", benchmarkCase.name())
                    .isLessThanOrEqualTo(RECONTEXT_CONFIG.maxReplayEvidence());
        }

        double baselineHitAvg = baselineHitSum / cases.size();
        double recontextHitAvg = recontextHitSum / cases.size();
        double baselineExclusionAvg = baselineExclusionSum / cases.size();
        double recontextExclusionAvg = recontextExclusionSum / cases.size();
        double charsReduction = baselineCharsSum == 0
                ? 0.0
                : 1.0 - (double) recontextCharsSum / baselineCharsSum;
        long benchmarkMillis = (System.nanoTime() - benchmarkStarted) / 1_000_000;

        report.append("\n## Summary\n\n");
        report.append("- Cases: ").append(cases.size()).append('\n');
        report.append("- Macro evidence hit: baseline ").append(format(baselineHitAvg))
                .append(" vs recontext ").append(format(recontextHitAvg)).append('\n');
        report.append("- Macro distractor exclusion: baseline ").append(format(baselineExclusionAvg))
                .append(" vs recontext ").append(format(recontextExclusionAvg)).append('\n');
        report.append("- Total replay chars: baseline ").append(baselineCharsSum)
                .append(" vs recontext ").append(recontextCharsSum)
                .append(" (reduction ").append(format(charsReduction * 100)).append("%)\n");
        report.append("- Recontext config: charBudget=").append(RECONTEXT_CONFIG.replayCharBudget())
                .append(", maxEvidence=").append(RECONTEXT_CONFIG.maxReplayEvidence()).append('\n');
        report.append("- Benchmark wall time: ").append(benchmarkMillis).append(" ms\n");

        report.append("\n## Decision Thresholds\n\n");
        report.append("| Threshold | Target | Observed | Pass |\n");
        report.append("| --- | --- | --- | --- |\n");
        report.append(thresholdRow("T1 non-regression case hit", "1.00 each", "asserted per case", true));
        report.append(thresholdRow("T2 macro recontext hit", ">= " + format(MACRO_HIT_THRESHOLD),
                format(recontextHitAvg), recontextHitAvg >= MACRO_HIT_THRESHOLD));
        report.append(thresholdRow("T3 exclusion improvement", ">= +" + format(EXCLUSION_IMPROVEMENT_THRESHOLD),
                "+" + format(recontextExclusionAvg - baselineExclusionAvg),
                recontextExclusionAvg - baselineExclusionAvg >= EXCLUSION_IMPROVEMENT_THRESHOLD));
        report.append(thresholdRow("T4 replay chars reduction", ">= " + format(CHARS_REDUCTION_THRESHOLD * 100) + "%",
                format(charsReduction * 100) + "%", charsReduction >= CHARS_REDUCTION_THRESHOLD));
        report.append(thresholdRow("T5 budget compliance", "always", "asserted per case", true));

        assertThat(recontextHitAvg)
                .as("T2: macro recontext evidence hit must stay at or above %.2f", MACRO_HIT_THRESHOLD)
                .isGreaterThanOrEqualTo(MACRO_HIT_THRESHOLD);
        assertThat(recontextExclusionAvg - baselineExclusionAvg)
                .as("T3: distractor exclusion must improve by at least %.2f absolute", EXCLUSION_IMPROVEMENT_THRESHOLD)
                .isGreaterThanOrEqualTo(EXCLUSION_IMPROVEMENT_THRESHOLD);
        assertThat(charsReduction)
                .as("T4: total replay chars must shrink by at least %.0f%%", CHARS_REDUCTION_THRESHOLD * 100)
                .isGreaterThanOrEqualTo(CHARS_REDUCTION_THRESHOLD);

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

    @Test
    void paraphraseRegressionCaseKeepsDroppedEvidenceInRetrievedContext() {
        BenchmarkCase benchmarkCase = paraphraseEvidenceRegressionCase();
        ArmMetrics recontext = measure("recontext", benchmarkCase, RECONTEXT_CONFIG);

        assertThat(recontext.evidenceHitRate())
                .as("documented regression: paraphrase-only evidence is dropped from the replay block")
                .isLessThan(1.0);
        String prompt = RetrievalService.answerPrompt(
                benchmarkCase.query(),
                benchmarkCase.results(),
                RECONTEXT_CONFIG
        );
        for (String citation : benchmarkCase.expectedCitations()) {
            assertThat(prompt)
                    .as("mitigation: the full retrieved context block must still carry the dropped evidence")
                    .contains("[" + citation + "]");
        }
    }

    @Test
    void noResultsCaseKeepsExplicitInsufficientEvidenceFallbacks() {
        String prompt = RetrievalService.answerPrompt("What evidence exists?", List.of(), RECONTEXT_CONFIG);

        assertThat(prompt).contains("No replayable evidence.", "No retrieved context.");
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

    private static void appendRow(StringBuilder report, BenchmarkCase benchmarkCase, ArmMetrics metrics) {
        report.append("| ").append(benchmarkCase.name())
                .append(" | ").append(benchmarkCase.category())
                .append(" | ").append(metrics.arm())
                .append(" | ").append(format(metrics.evidenceHitRate()))
                .append(" | ").append(format(metrics.distractorExclusionRate()))
                .append(" | ").append(metrics.firstEvidencePosition())
                .append(" | ").append(metrics.selectedCount())
                .append(" | ").append(metrics.replayChars())
                .append(" | ").append(metrics.packMicros())
                .append(" |\n");
    }

    private static String thresholdRow(String name, String target, String observed, boolean pass) {
        return "| " + name + " | " + target + " | " + observed + " | " + (pass ? "PASS" : "FAIL") + " |\n";
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static List<BenchmarkCase> benchmarkCases() {
        List<BenchmarkCase> cases = new ArrayList<>();
        cases.add(directFactCase());
        cases.add(distractorHeavyCase());
        cases.add(multiEvidenceLongQueryCase());
        cases.add(distractorLedRankingCase());
        cases.add(longNoisyContextCase());
        cases.add(koreanQueryCase());
        cases.add(bilingualEvidenceCase());
        cases.add(duplicateEvidenceCase());
        cases.add(conflictingNearDuplicateCase());
        cases.add(budgetOverflowCase());
        cases.add(noResultsCase());
        cases.add(noLexicalOverlapFallbackCase());
        cases.add(paraphraseEvidenceRegressionCase());
        cases.add(lexicalTrapSurvivalCase());
        return cases;
    }

    private static BenchmarkCase directFactCase() {
        String query = "Which port does the api-gateway bind on?";
        List<SearchResultItem> results = List.of(
                item("d1", "design/storage.md#weaviate", "Weaviate keeps one object per chunk with citation metadata."),
                item("e1", "guide/ports.md#gateway", "The api-gateway binds local port 42120 at 127.0.0.1."),
                item("d2", "design/storage.md#postgres", "PostgreSQL keeps registry and failure state."),
                item("d3", "guide/models.md#chat", "The chat model runs through local Ollama only.")
        );
        return new BenchmarkCase(
                "direct-fact",
                "direct fact retrieval",
                query,
                results,
                Set.of("guide/ports.md#gateway"),
                Set.of("design/storage.md#weaviate", "design/storage.md#postgres", "guide/models.md#chat"),
                true
        );
    }

    private static BenchmarkCase distractorHeavyCase() {
        String query = "How does stale-source demotion affect ranking for deprecated documents?";
        List<SearchResultItem> results = List.of(
                item("d1", "guide/ports.md#gateway", "The api-gateway binds to 127.0.0.1:42120."),
                item("d2", "design/storage.md#weaviate", "Weaviate keeps one object per chunk."),
                item("e1", "design/governance.md#demotion", "Stale-source demotion lowers the ranking weight of deprecated documents after first-stage retrieval."),
                item("d3", "design/storage.md#postgres", "PostgreSQL keeps registry and job state."),
                item("d4", "guide/models.md#embedding", "Embedding batches keep local Ollama memory stable."),
                item("d5", "guide/compose.md#profiles", "Docker Compose profiles separate runtime containers from evaluation sidecars."),
                item("d6", "guide/backup.md#schedule", "Nightly backups copy the database volume to the archive disk."),
                item("e2", "design/governance.md#historical", "A deprecated document can still be retrieved with the historical opt-in filter, but its ranking stays demoted."),
                item("d7", "design/bridge.md#rest", "The MCP bridge exposes REST endpoints through the gateway."),
                item("d8", "design/watcher.md#scan", "The file watcher detects changes and schedules scans."),
                item("d9", "design/chunking.md#headings", "Markdown chunking splits sections by heading hierarchy."),
                item("d10", "design/audit.md#latency", "Audit rows capture phase latency per request.")
        );
        return new BenchmarkCase(
                "distractor-heavy",
                "distractor-heavy retrieval",
                query,
                results,
                Set.of("design/governance.md#demotion", "design/governance.md#historical"),
                Set.of("guide/ports.md#gateway", "design/storage.md#weaviate", "design/storage.md#postgres",
                        "guide/models.md#embedding", "guide/compose.md#profiles", "guide/backup.md#schedule",
                        "design/bridge.md#rest", "design/watcher.md#scan", "design/chunking.md#headings",
                        "design/audit.md#latency"),
                true
        );
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
                "multi-evidence answer",
                query,
                results,
                Set.of("design/indexing.md#frontmatter", "design/indexing.md#audit", "guide/operations.md#rescan"),
                Set.of("guide/ports.md#gateway", "guide/models.md#embedding", "design/storage.md#weaviate",
                        "design/storage.md#postgres", "guide/compose.md#profiles"),
                true
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
                "distractor-led ranking",
                query,
                results,
                Set.of("design/answer.md#citation", "design/answer.md#replay"),
                Set.of("design/search.md#modes", "design/search.md#limits", "design/search.md#weighting"),
                true
        );
    }

    private static BenchmarkCase longNoisyContextCase() {
        String noise = " This paragraph restates general compose profile guidance and local-only mount rules"
                + " that apply to every runtime container regardless of the question, including read-only"
                + " mounts, health endpoints, and citation-bearing hybrid retrieval conventions.";
        String query = "Where are failed indexing jobs recorded and retried?";
        List<SearchResultItem> results = List.of(
                item("d1", "guide/ports.md#gateway", "The api-gateway binds to 127.0.0.1:42120." + noise),
                item("e1", "design/storage.md#failure-record", "Failed indexing jobs are recorded in the failure_record table." + noise),
                item("d2", "design/storage.md#weaviate", "Weaviate keeps one object per chunk." + noise),
                item("d3", "guide/models.md#embedding", "Embedding batches keep local Ollama memory stable." + noise),
                item("e2", "guide/operations.md#retry", "A recorded failure is retried by triggering a force scan for the affected document." + noise),
                item("d4", "guide/compose.md#volumes", "Volumes are mounted read-only into the runtime containers." + noise),
                item("d5", "guide/backup.md#schedule", "Nightly backups copy the database volume to the archive disk." + noise),
                item("e3", "design/storage.md#index-job", "The index_job table keeps one row per indexing run with its terminal status." + noise),
                item("d6", "design/bridge.md#rest", "The MCP bridge exposes REST endpoints through the gateway." + noise),
                item("d7", "design/chunking.md#headings", "Markdown chunking splits sections by heading hierarchy." + noise),
                item("d8", "design/audit.md#latency", "Audit rows capture phase latency per request." + noise),
                item("d9", "guide/models.md#chat", "The chat model runs through local Ollama only." + noise)
        );
        return new BenchmarkCase(
                "long-noisy-context",
                "long/noisy context",
                query,
                results,
                Set.of("design/storage.md#failure-record", "guide/operations.md#retry", "design/storage.md#index-job"),
                Set.of("guide/ports.md#gateway", "design/storage.md#weaviate", "guide/models.md#embedding",
                        "guide/compose.md#volumes", "guide/backup.md#schedule", "design/bridge.md#rest",
                        "design/chunking.md#headings", "design/audit.md#latency", "guide/models.md#chat"),
                true
        );
    }

    private static BenchmarkCase koreanQueryCase() {
        String query = "인덱서가 frontmatter 오류 문서를 어떻게 처리하고 어떤 감사 기록을 남기나요?";
        List<SearchResultItem> results = List.of(
                item("d1", "guide/ports.md#gateway", "게이트웨이는 127.0.0.1:42120에서 요청을 받는다."),
                item("e1", "design/indexing.md#frontmatter-ko", "인덱서는 frontmatter 오류가 있는 문서를 건너뛰고 나머지 파일 스캔을 계속한다."),
                item("e2", "design/indexing.md#audit-ko", "건너뛴 문서에 대해 frontmatter-parse-error 사유의 감사 기록을 남긴다."),
                item("d2", "guide/models.md#chat-ko", "채팅 모델은 로컬 Ollama에서 제공된다.")
        );
        return new BenchmarkCase(
                "korean-query",
                "Korean query",
                query,
                results,
                Set.of("design/indexing.md#frontmatter-ko", "design/indexing.md#audit-ko"),
                Set.of("guide/ports.md#gateway", "guide/models.md#chat-ko"),
                true
        );
    }

    private static BenchmarkCase bilingualEvidenceCase() {
        String query = "하이브리드 검색 keyword vector fusion 방식";
        List<SearchResultItem> results = List.of(
                item("d1", "guide/ports.md#gateway-ko", "게이트웨이 포트는 42120이다."),
                item("e1", "design/search.md#fusion", "Hybrid search fuses keyword and vector scores with a deterministic fusion weight."),
                item("e2", "design/search.md#fusion-ko", "하이브리드 검색은 keyword 점수와 vector 점수를 결합한다."),
                item("d2", "guide/backup.md#schedule", "Nightly backups copy the database volume to the archive disk.")
        );
        return new BenchmarkCase(
                "bilingual-evidence",
                "bilingual content",
                query,
                results,
                Set.of("design/search.md#fusion", "design/search.md#fusion-ko"),
                Set.of("guide/ports.md#gateway-ko", "guide/backup.md#schedule"),
                true
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
                "near-duplicate dedupe",
                query,
                results,
                Set.of("design/answer.md#dedupe"),
                Set.of("guide/backup.md#schedule"),
                true
        );
    }

    private static BenchmarkCase conflictingNearDuplicateCase() {
        String query = "What is the default replay char budget for answer evidence packing?";
        List<SearchResultItem> results = List.of(
                item("e1", "design/answer.md#budget", "The default replay char budget for answer evidence packing is 1600."),
                item("e2", "design/answer-draft.md#budget", "An older draft set the default replay char budget to 2000 before packing was tuned."),
                item("d1", "guide/backup.md#schedule", "Nightly backups copy the database volume to the archive disk.")
        );
        return new BenchmarkCase(
                "conflicting-near-duplicate",
                "conflicting/near-duplicate evidence",
                query,
                results,
                Set.of("design/answer.md#budget", "design/answer-draft.md#budget"),
                Set.of("guide/backup.md#schedule"),
                true
        );
    }

    private static BenchmarkCase budgetOverflowCase() {
        String filler = " The section also restates general operational guidance about local-only deployment,"
                + " read-only mounts, and citation-bearing hybrid retrieval that applies to every container"
                + " in the compose stack regardless of the question being asked here.";
        String query = "What triggers a stale chunk delete during incremental reindexing?";
        List<SearchResultItem> results = List.of(
                item("e1", "design/indexing.md#stale", "A stale chunk delete is triggered when the file is removed or its content hash changes during incremental reindexing." + filler),
                item("d1", "design/storage.md#weaviate", "Weaviate keeps one object per chunk with citation metadata." + filler),
                item("e2", "design/indexing.md#hash", "Incremental reindexing compares stored content hashes to decide which files changed." + filler),
                item("d2", "guide/compose.md#volumes", "Folders are mounted read-only into the indexer container." + filler),
                item("d3", "guide/models.md#embedding", "Embedding batches are limited to keep local Ollama memory stable." + filler),
                item("d4", "design/search.md#hybrid", "Hybrid retrieval fuses keyword and vector scores." + filler)
        );
        return new BenchmarkCase(
                "budget-overflow",
                "long/noisy context (budget)",
                query,
                results,
                Set.of("design/indexing.md#stale", "design/indexing.md#hash"),
                Set.of("design/storage.md#weaviate", "guide/compose.md#volumes", "guide/models.md#embedding",
                        "design/search.md#hybrid"),
                true
        );
    }

    private static BenchmarkCase noResultsCase() {
        return new BenchmarkCase(
                "no-results",
                "no-answer/insufficient evidence",
                "Does the system support distributed sharded reindexing?",
                List.of(),
                Set.of(),
                Set.of(),
                true
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
                "fallback safety",
                query,
                results,
                Set.of(),
                Set.of(),
                true
        );
    }

    private static BenchmarkCase paraphraseEvidenceRegressionCase() {
        String query = "How do I roll back a bad chunk layout change?";
        List<SearchResultItem> results = List.of(
                item("e1", "guide/operations.md#restore", "Restore the previous Weaviate class definition and reindex affected registered folders."),
                item("d1", "design/audit.md#columns", "The audit layout adds phase latency columns to the retrieval log.")
        );
        return new BenchmarkCase(
                "paraphrase-evidence-regression",
                "regression risk: paraphrase drop",
                query,
                results,
                Set.of("guide/operations.md#restore"),
                Set.of("design/audit.md#columns"),
                false
        );
    }

    private static BenchmarkCase lexicalTrapSurvivalCase() {
        String query = "Which filter fields does the search API accept?";
        List<SearchResultItem> results = List.of(
                item("d1", "design/audit.md#api", "The search audit table logs every api call latency."),
                item("d2", "design/errors.md#api", "Search api errors return 400 for unknown projects."),
                item("e1", "design/search.md#filters", "Filter fields accepted by the search api: ssotRole, docType, status, authority, sensitivity.")
        );
        return new BenchmarkCase(
                "lexical-trap-survival",
                "regression risk: lexical trap",
                query,
                results,
                Set.of("design/search.md#filters"),
                Set.of("design/audit.md#api", "design/errors.md#api"),
                true
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
