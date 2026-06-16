package com.localrag.retrieval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreGateOfflineEvaluationTests {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void validatesOfflineFixtureWithSelectorContract() throws IOException {
        Path fixturePath = fixturePath();
        OfflineFixture fixture = OBJECT_MAPPER.readValue(fixturePath.toFile(), OfflineFixture.class);

        assertThat(fixture.version()).isEqualTo(1);
        assertThat(fixture.cases()).isNotEmpty();

        ScoreGateCandidateSelector.Config config = fixture.config().toSelectorConfig();
        List<CaseReport> caseReports = new ArrayList<>();
        for (OfflineCase testCase : fixture.cases()) {
            assertThat(testCase.candidateWindowSize())
                    .as("candidate window should model top-N wider than fixed top-K for %s", testCase.id())
                    .isGreaterThanOrEqualTo(30);
            ScoreGateCandidateSelector.Result result = ScoreGateCandidateSelector.select(
                    testCase.candidates().stream()
                            .map(OfflineCandidate::toSelectorCandidate)
                            .toList(),
                    config
            );
            CaseReport report = report(testCase, config.maxK(), result);
            caseReports.add(report);

            assertThat(report.retainedCandidateIds())
                    .as("expected candidate should be retained for %s", testCase.id())
                    .contains(testCase.expectedCandidateId());
            assertThat(report.topRetainedSourceId())
                    .as("top retained source should match expected source for %s", testCase.id())
                    .isEqualTo(testCase.expectedSourceId());
            assertThat(report.retainedRelativePaths())
                    .as("expected relative path should be retained for %s", testCase.id())
                    .contains(testCase.expectedRelativePath());
            if (!testCase.mustDropCandidateIds().isEmpty()) {
                assertThat(report.retainedCandidateIds())
                        .as("must-drop candidates should not survive ScoreGate for %s", testCase.id())
                        .doesNotContainAnyElementsOf(testCase.mustDropCandidateIds());
            }
            assertThat(report.retainedRelativePaths())
                    .as("multi-hop coverage should be retained for %s", testCase.id())
                    .containsAll(testCase.requiredCoverageRelativePaths());

            if (testCase.requireB3Rescue()) {
                assertThat(report.expectedBucket())
                        .as("expected candidate should be a B3 rescue for %s", testCase.id())
                        .isEqualTo(ScoreGateCandidateSelector.Bucket.B3.name());
            }
            if (testCase.expectFixedTopKMiss()) {
                assertThat(report.fixedTopKRetainedExpected())
                        .as("fixed top-K should miss expected candidate for %s", testCase.id())
                        .isFalse();
            }
        }

        SummaryReport summary = summary(fixturePath, config, caseReports);
        assertThat(summary.hitAt5()).isEqualTo(1.0);
        assertThat(summary.sourceAccuracyAt1()).isEqualTo(1.0);
        assertThat(summary.multiHopCoverage()).isEqualTo(1.0);
        assertThat(summary.b3RescueCount()).isGreaterThanOrEqualTo(2);

        String outputFile = System.getenv("LOCAL_RAG_SCOREGATE_OUTPUT");
        if (outputFile != null && !outputFile.isBlank()) {
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(Path.of(outputFile).toFile(), summary);
        }
    }

    private static CaseReport report(
            OfflineCase testCase,
            int maxK,
            ScoreGateCandidateSelector.Result result
    ) {
        Map<String, OfflineCandidate> candidatesById = testCase.candidates().stream()
                .collect(Collectors.toMap(OfflineCandidate::id, candidate -> candidate));
        List<String> retainedIds = result.retained().stream()
                .map(decision -> decision.candidate().id())
                .toList();
        List<String> retainedPaths = retainedIds.stream()
                .map(candidateId -> candidatesById.get(candidateId).relativePath())
                .distinct()
                .toList();
        int expectedRank = retainedIds.indexOf(testCase.expectedCandidateId()) + 1;
        ScoreGateCandidateSelector.Decision expectedDecision = result.decisions().stream()
                .filter(decision -> decision.candidate().id().equals(testCase.expectedCandidateId()))
                .findFirst()
                .orElseThrow();
        int retainedTokens = retainedIds.stream()
                .mapToInt(candidateId -> candidatesById.get(candidateId).estimatedTokens())
                .sum();
        int fixedTopKTokens = testCase.candidates().stream()
                .filter(candidate -> candidate.rank() < maxK)
                .mapToInt(OfflineCandidate::estimatedTokens)
                .sum();
        boolean fixedTopKRetainedExpected = testCase.candidates().stream()
                .anyMatch(candidate -> candidate.id().equals(testCase.expectedCandidateId()) && candidate.rank() < maxK);
        String topSourceId = retainedIds.isEmpty() ? null : candidatesById.get(retainedIds.get(0)).sourceId();
        return new CaseReport(
                testCase.id(),
                testCase.groupId(),
                testCase.language(),
                testCase.intent(),
                expectedRank,
                testCase.expectedSourceId(),
                topSourceId,
                retainedIds,
                retainedPaths,
                retainedTokens,
                fixedTopKTokens,
                expectedDecision.bucket().name(),
                fixedTopKRetainedExpected
        );
    }

    private static SummaryReport summary(
            Path fixturePath,
            ScoreGateCandidateSelector.Config config,
            List<CaseReport> cases
    ) {
        int count = cases.size();
        double hitAt1 = ratio(cases.stream().filter(row -> row.expectedRank() == 1).count(), count);
        double hitAt5 = ratio(cases.stream().filter(row -> row.expectedRank() > 0 && row.expectedRank() <= 5).count(), count);
        double mrr = count == 0 ? 0.0 : cases.stream()
                .mapToDouble(row -> row.expectedRank() > 0 ? 1.0 / row.expectedRank() : 0.0)
                .sum() / count;
        double sourceAccuracy = ratio(cases.stream()
                .filter(row -> row.expectedSourceId().equals(row.topRetainedSourceId()))
                .count(), count);
        double fixedMissRescued = ratio(cases.stream()
                .filter(row -> !row.fixedTopKRetainedExpected() && row.expectedRank() > 0)
                .count(), count);
        double multiHopCoverage = ratio(cases.stream()
                .filter(row -> !"multi-hop-coverage".equals(row.intent()) || row.expectedRank() > 0)
                .count(), count);
        int retainedTokens = cases.stream().mapToInt(CaseReport::retainedTokens).sum();
        int fixedTokens = cases.stream().mapToInt(CaseReport::fixedTopKTokens).sum();
        int b3Rescues = (int) cases.stream()
                .filter(row -> ScoreGateCandidateSelector.Bucket.B3.name().equals(row.expectedBucket()))
                .count();
        return new SummaryReport(
                Instant.now().toString(),
                fixturePath.toString(),
                new OfflineConfig(
                        config.similarityThreshold(),
                        config.rerankerThreshold(),
                        config.similarityWeight(),
                        config.bucket2FusionThreshold(),
                        config.bucket3FusionThreshold(),
                        config.maxK()
                ),
                count,
                hitAt1,
                hitAt5,
                mrr,
                sourceAccuracy,
                fixedMissRescued,
                multiHopCoverage,
                retainedTokens,
                fixedTokens,
                b3Rescues,
                cases
        );
    }

    private static double ratio(long numerator, int denominator) {
        return denominator == 0 ? 0.0 : ((double) numerator) / denominator;
    }

    private static Path fixturePath() {
        String configured = System.getenv("LOCAL_RAG_SCOREGATE_FIXTURE");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        Path cursor = Path.of("").toAbsolutePath();
        while (cursor != null) {
            Path candidate = cursor.resolve("docs/evaluation/scoregate-offline-cases.json");
            if (Files.exists(candidate)) {
                return candidate;
            }
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("Could not find docs/evaluation/scoregate-offline-cases.json");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OfflineFixture(int version, String description, OfflineConfig config, List<OfflineCase> cases) {
        OfflineFixture {
            cases = cases == null ? List.of() : List.copyOf(cases);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OfflineConfig(
            double similarityThreshold,
            double rerankerThreshold,
            double similarityWeight,
            double bucket2FusionThreshold,
            double bucket3FusionThreshold,
            int maxK
    ) {
        ScoreGateCandidateSelector.Config toSelectorConfig() {
            return new ScoreGateCandidateSelector.Config(
                    similarityThreshold,
                    rerankerThreshold,
                    similarityWeight,
                    bucket2FusionThreshold,
                    bucket3FusionThreshold,
                    maxK
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OfflineCase(
            String id,
            String groupId,
            String language,
            String intent,
            String query,
            int candidateWindowSize,
            String expectedCandidateId,
            String expectedSourceId,
            String expectedRelativePath,
            boolean requireB3Rescue,
            boolean expectFixedTopKMiss,
            List<String> mustDropCandidateIds,
            List<String> requiredCoverageRelativePaths,
            List<OfflineCandidate> candidates
    ) {
        OfflineCase {
            mustDropCandidateIds = mustDropCandidateIds == null ? List.of() : List.copyOf(mustDropCandidateIds);
            requiredCoverageRelativePaths = requiredCoverageRelativePaths == null ? List.of() : List.copyOf(requiredCoverageRelativePaths);
            candidates = candidates == null ? List.of() : List.copyOf(candidates);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OfflineCandidate(
            String id,
            String sourceId,
            String relativePath,
            double similarityScore,
            double rerankerScore,
            int rank,
            int estimatedTokens,
            Map<String, Object> metadata
    ) {
        ScoreGateCandidateSelector.Candidate toSelectorCandidate() {
            return new ScoreGateCandidateSelector.Candidate(
                    id,
                    similarityScore,
                    rerankerScore,
                    rank,
                    metadata
            );
        }
    }

    record CaseReport(
            String id,
            String groupId,
            String language,
            String intent,
            int expectedRank,
            String expectedSourceId,
            String topRetainedSourceId,
            List<String> retainedCandidateIds,
            List<String> retainedRelativePaths,
            int retainedTokens,
            int fixedTopKTokens,
            String expectedBucket,
            boolean fixedTopKRetainedExpected
    ) {
    }

    record SummaryReport(
            String generatedAt,
            String fixture,
            OfflineConfig config,
            int caseCount,
            double hitAt1,
            double hitAt5,
            double mrr,
            double sourceAccuracyAt1,
            double fixedMissRescueRate,
            double multiHopCoverage,
            int retainedTokens,
            int fixedTopKTokens,
            int b3RescueCount,
            List<CaseReport> cases
    ) {
    }
}
