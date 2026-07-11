package com.localrag.retrieval;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "local-rag.retrieval")
public record RetrievalSettings(
        boolean embeddingFallbackEnabled,
        boolean rerankerEnabled,
        String rerankerBaseUrl,
        Duration rerankerConnectTimeout,
        Duration rerankerReadTimeout,
        boolean scoreGateEnabled,
        double scoreGateSimilarityThreshold,
        double scoreGateRerankerThreshold,
        double scoreGateSimilarityWeight,
        double scoreGateBucket2Threshold,
        double scoreGateBucket3Threshold,
        int scoreGateMaxK
) {
    public RetrievalSettings {
        rerankerBaseUrl = rerankerBaseUrl == null ? "" : rerankerBaseUrl.trim();
        rerankerConnectTimeout = positive(rerankerConnectTimeout, "rerankerConnectTimeout");
        rerankerReadTimeout = positive(rerankerReadTimeout, "rerankerReadTimeout");
        positive(scoreGateSimilarityThreshold, "scoreGateSimilarityThreshold");
        positive(scoreGateRerankerThreshold, "scoreGateRerankerThreshold");
        positive(scoreGateSimilarityWeight, "scoreGateSimilarityWeight");
        positive(scoreGateBucket2Threshold, "scoreGateBucket2Threshold");
        positive(scoreGateBucket3Threshold, "scoreGateBucket3Threshold");
        if (scoreGateMaxK <= 0) {
            throw new IllegalArgumentException("scoreGateMaxK must be positive");
        }
    }

    public long rerankerConnectTimeoutMillis() {
        return rerankerConnectTimeout.toMillis();
    }

    public long rerankerReadTimeoutMillis() {
        return rerankerReadTimeout.toMillis();
    }

    private static Duration positive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static void positive(double value, String name) {
        if (value <= 0.0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
