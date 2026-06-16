package com.localrag.retrieval;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ScoreGateCandidateSelector {
    private ScoreGateCandidateSelector() {
    }

    static Result select(List<Candidate> candidates, Config config) {
        if (candidates == null || candidates.isEmpty()) {
            return new Result(List.of(), List.of());
        }
        Config effectiveConfig = config == null ? Config.paperReferenceDefaults() : config;
        List<PreliminaryDecision> preliminaryDecisions = java.util.stream.IntStream.range(0, candidates.size())
                .mapToObj(index -> decide(index, candidates.get(index), effectiveConfig))
                .toList();

        Set<Integer> retainedIndexes = preliminaryDecisions.stream()
                .filter(PreliminaryDecision::retained)
                .sorted(Comparator.comparingDouble(PreliminaryDecision::fusionScore).reversed()
                        .thenComparingInt(decision -> decision.candidate().rank()))
                .limit(effectiveConfig.maxK())
                .map(PreliminaryDecision::index)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        List<Decision> decisions = preliminaryDecisions.stream()
                .map(decision -> finalizeDecision(decision, retainedIndexes))
                .toList();
        List<Decision> retained = decisions.stream()
                .filter(Decision::retained)
                .sorted(Comparator.comparingDouble(Decision::fusionScore).reversed()
                        .thenComparingInt(decision -> decision.candidate().rank()))
                .toList();
        return new Result(decisions, retained);
    }

    private static PreliminaryDecision decide(int index, Candidate candidate, Config config) {
        Bucket bucket = bucket(candidate, config);
        double fusionScore = fusionScore(candidate, config);
        return switch (bucket) {
            case B1 -> new PreliminaryDecision(index, candidate, bucket, fusionScore, true, "kept-b1-both-scores-above-threshold");
            case B2 -> fusionScore >= config.bucket2FusionThreshold()
                    ? new PreliminaryDecision(index, candidate, bucket, fusionScore, true, "kept-b2-fusion-above-threshold")
                    : new PreliminaryDecision(index, candidate, bucket, fusionScore, false, "dropped-b2-fusion-below-threshold");
            case B3 -> fusionScore >= config.bucket3FusionThreshold()
                    ? new PreliminaryDecision(index, candidate, bucket, fusionScore, true, "kept-b3-reranker-rescue")
                    : new PreliminaryDecision(index, candidate, bucket, fusionScore, false, "dropped-b3-fusion-below-threshold");
            case B4 -> new PreliminaryDecision(index, candidate, bucket, fusionScore, false, "dropped-b4-both-scores-below-threshold");
        };
    }

    private static Decision finalizeDecision(PreliminaryDecision decision, Set<Integer> retainedIndexes) {
        if (!decision.retained()) {
            return new Decision(
                    decision.candidate(),
                    decision.bucket(),
                    decision.fusionScore(),
                    false,
                    decision.reason()
            );
        }
        if (retainedIndexes.contains(decision.index())) {
            return new Decision(
                    decision.candidate(),
                    decision.bucket(),
                    decision.fusionScore(),
                    true,
                    decision.reason()
            );
        }
        return new Decision(
                decision.candidate(),
                decision.bucket(),
                decision.fusionScore(),
                false,
                "dropped-max-k-cap"
        );
    }

    private static Bucket bucket(Candidate candidate, Config config) {
        boolean highSimilarity = candidate.similarityScore() >= config.similarityThreshold();
        boolean highReranker = candidate.rerankerScore() >= config.rerankerThreshold();
        if (highSimilarity && highReranker) {
            return Bucket.B1;
        }
        if (highSimilarity) {
            return Bucket.B2;
        }
        if (highReranker) {
            return Bucket.B3;
        }
        return Bucket.B4;
    }

    private static double fusionScore(Candidate candidate, Config config) {
        return (config.similarityWeight() * candidate.similarityScore())
                + ((1.0 - config.similarityWeight()) * candidate.rerankerScore());
    }

    private static void requireProbability(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be a finite value in [0, 1]");
        }
    }

    enum Bucket {
        B1,
        B2,
        B3,
        B4
    }

    record Config(
            double similarityThreshold,
            double rerankerThreshold,
            double similarityWeight,
            double bucket2FusionThreshold,
            double bucket3FusionThreshold,
            int maxK
    ) {
        Config {
            requireProbability("similarityThreshold", similarityThreshold);
            requireProbability("rerankerThreshold", rerankerThreshold);
            requireProbability("similarityWeight", similarityWeight);
            requireProbability("bucket2FusionThreshold", bucket2FusionThreshold);
            requireProbability("bucket3FusionThreshold", bucket3FusionThreshold);
            if (maxK <= 0) {
                throw new IllegalArgumentException("maxK must be greater than 0");
            }
        }

        static Config paperReferenceDefaults() {
            return new Config(0.70, 0.08, 0.30, 0.255, 0.15, 10);
        }
    }

    record Candidate(
            String id,
            double similarityScore,
            double rerankerScore,
            int rank,
            Map<String, Object> metadata
    ) {
        Candidate {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("id must not be blank");
            }
            requireProbability("similarityScore", similarityScore);
            requireProbability("rerankerScore", rerankerScore);
            if (rank < 0) {
                throw new IllegalArgumentException("rank must be greater than or equal to 0");
            }
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }

    record Decision(
            Candidate candidate,
            Bucket bucket,
            double fusionScore,
            boolean retained,
            String reason
    ) {
    }

    record Result(List<Decision> decisions, List<Decision> retained) {
        Result {
            decisions = decisions == null ? List.of() : List.copyOf(decisions);
            retained = retained == null ? List.of() : List.copyOf(retained);
        }
    }

    private record PreliminaryDecision(
            int index,
            Candidate candidate,
            Bucket bucket,
            double fusionScore,
            boolean retained,
            String reason
    ) {
    }
}
