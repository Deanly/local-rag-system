package com.localrag.retrieval;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScoreGateCandidateSelectorTests {
    private static final ScoreGateCandidateSelector.Config CONFIG =
            ScoreGateCandidateSelector.Config.paperReferenceDefaults();

    @Test
    void classifiesBucketsAndAppliesFusionThresholds() {
        List<ScoreGateCandidateSelector.Candidate> candidates = List.of(
                candidate("b1", 0.90, 0.20, 0),
                candidate("b2-keep", 0.70, 0.07, 1),
                candidate("b2-drop", 0.70, 0.06, 2),
                candidate("b3-keep", 0.10, 0.18, 3),
                candidate("b3-drop", 0.10, 0.17, 4),
                candidate("b4", 0.10, 0.01, 5)
        );

        ScoreGateCandidateSelector.Result result = ScoreGateCandidateSelector.select(candidates, CONFIG);

        assertThat(result.decisions())
                .extracting(decision -> decision.candidate().id(), ScoreGateCandidateSelector.Decision::bucket,
                        ScoreGateCandidateSelector.Decision::retained, ScoreGateCandidateSelector.Decision::reason)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("b1", ScoreGateCandidateSelector.Bucket.B1, true,
                                "kept-b1-both-scores-above-threshold"),
                        org.assertj.core.groups.Tuple.tuple("b2-keep", ScoreGateCandidateSelector.Bucket.B2, true,
                                "kept-b2-fusion-above-threshold"),
                        org.assertj.core.groups.Tuple.tuple("b2-drop", ScoreGateCandidateSelector.Bucket.B2, false,
                                "dropped-b2-fusion-below-threshold"),
                        org.assertj.core.groups.Tuple.tuple("b3-keep", ScoreGateCandidateSelector.Bucket.B3, true,
                                "kept-b3-reranker-rescue"),
                        org.assertj.core.groups.Tuple.tuple("b3-drop", ScoreGateCandidateSelector.Bucket.B3, false,
                                "dropped-b3-fusion-below-threshold"),
                        org.assertj.core.groups.Tuple.tuple("b4", ScoreGateCandidateSelector.Bucket.B4, false,
                                "dropped-b4-both-scores-below-threshold")
                );
        assertThat(result.retained())
                .extracting(decision -> decision.candidate().id())
                .containsExactly("b1", "b2-keep", "b3-keep");
    }

    @Test
    void rescuesLowSimilarityCandidateWhenRerankerScoreIsStrongEnough() {
        ScoreGateCandidateSelector.Result result = ScoreGateCandidateSelector.select(List.of(
                candidate("lexical-mismatch", 0.24, 0.80, 0),
                candidate("both-low", 0.24, 0.04, 1)
        ), CONFIG);

        assertThat(result.decisions().get(0).bucket()).isEqualTo(ScoreGateCandidateSelector.Bucket.B3);
        assertThat(result.decisions().get(0).retained()).isTrue();
        assertThat(result.decisions().get(0).reason()).isEqualTo("kept-b3-reranker-rescue");
        assertThat(result.decisions().get(1).bucket()).isEqualTo(ScoreGateCandidateSelector.Bucket.B4);
        assertThat(result.decisions().get(1).retained()).isFalse();
    }

    @Test
    void maxKCapRetainsHighestFusionCandidatesAcrossBuckets() {
        ScoreGateCandidateSelector.Config maxTwo = new ScoreGateCandidateSelector.Config(
                0.70,
                0.08,
                0.30,
                0.255,
                0.15,
                2
        );

        ScoreGateCandidateSelector.Result result = ScoreGateCandidateSelector.select(List.of(
                candidate("low-fusion-b1", 0.80, 0.10, 0),
                candidate("high-fusion-b3", 0.69, 0.90, 1),
                candidate("high-fusion-b1", 0.90, 0.70, 2)
        ), maxTwo);

        assertThat(result.retained())
                .extracting(decision -> decision.candidate().id())
                .containsExactly("high-fusion-b3", "high-fusion-b1");
        assertThat(result.decisions())
                .filteredOn(decision -> decision.candidate().id().equals("low-fusion-b1"))
                .singleElement()
                .satisfies(decision -> {
                    assertThat(decision.bucket()).isEqualTo(ScoreGateCandidateSelector.Bucket.B1);
                    assertThat(decision.retained()).isFalse();
                    assertThat(decision.reason()).isEqualTo("dropped-max-k-cap");
                });
    }

    @Test
    void validatesConfigAndNormalizedCandidateScores() {
        assertThatThrownBy(() -> new ScoreGateCandidateSelector.Config(1.2, 0.08, 0.30, 0.255, 0.15, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("similarityThreshold");
        assertThatThrownBy(() -> new ScoreGateCandidateSelector.Config(0.70, 0.08, 0.30, 0.255, 0.15, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxK");
        assertThatThrownBy(() -> candidate("not-normalized", 1.4, 0.20, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("similarityScore");
        assertThatThrownBy(() -> candidate("bad-rank", 0.40, 0.20, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rank");
    }

    private static ScoreGateCandidateSelector.Candidate candidate(
            String id,
            double similarityScore,
            double rerankerScore,
            int rank
    ) {
        return new ScoreGateCandidateSelector.Candidate(
                id,
                similarityScore,
                rerankerScore,
                rank,
                Map.of()
        );
    }
}
