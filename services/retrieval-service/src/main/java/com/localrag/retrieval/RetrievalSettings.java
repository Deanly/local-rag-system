package com.localrag.retrieval;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "local-rag.retrieval")
public record RetrievalSettings(
        String ollamaBaseUrl,
        String ollamaBaseUrls,
        String embeddingModel,
        String chatModel,
        String weaviateUrl,
        boolean embeddingFallbackEnabled,
        long ollamaConnectTimeoutMillis,
        long ollamaReadTimeoutMillis,
        boolean rerankerEnabled,
        String rerankerBaseUrl,
        long rerankerConnectTimeoutMillis,
        long rerankerReadTimeoutMillis,
        boolean scoreGateEnabled,
        double scoreGateSimilarityThreshold,
        double scoreGateRerankerThreshold,
        double scoreGateSimilarityWeight,
        double scoreGateBucket2Threshold,
        double scoreGateBucket3Threshold,
        int scoreGateMaxK
) {
    public RetrievalSettings {
        if (ollamaBaseUrl == null || ollamaBaseUrl.isBlank()) {
            ollamaBaseUrl = System.getenv().getOrDefault("RAG_OLLAMA_BASE_URL", "http://localhost:11434");
        }
        if (ollamaBaseUrls == null || ollamaBaseUrls.isBlank()) {
            ollamaBaseUrls = System.getenv().getOrDefault("RAG_OLLAMA_BASE_URLS", ollamaBaseUrl);
            if (ollamaBaseUrls == null || ollamaBaseUrls.isBlank()) {
                ollamaBaseUrls = ollamaBaseUrl;
            }
        }
        if (embeddingModel == null || embeddingModel.isBlank()) {
            embeddingModel = System.getenv().getOrDefault("RAG_EMBEDDING_MODEL", "qwen3-embedding:4b");
        }
        if (chatModel == null) {
            chatModel = System.getenv().getOrDefault("RAG_CHAT_MODEL", "");
        }
        if (weaviateUrl == null || weaviateUrl.isBlank()) {
            weaviateUrl = System.getenv().getOrDefault("RAG_WEAVIATE_URL", "http://weaviate:8080");
        }
        if (ollamaConnectTimeoutMillis <= 0) {
            ollamaConnectTimeoutMillis = Long.parseLong(System.getenv().getOrDefault("RAG_OLLAMA_CONNECT_TIMEOUT_MILLIS", "1500"));
        }
        if (ollamaReadTimeoutMillis <= 0) {
            ollamaReadTimeoutMillis = Long.parseLong(System.getenv().getOrDefault("RAG_OLLAMA_READ_TIMEOUT_MILLIS", "120000"));
        }
        if (!rerankerEnabled) {
            rerankerEnabled = truthy(System.getenv().getOrDefault("RAG_RERANKER_ENABLED", "false"));
        }
        if (rerankerBaseUrl == null || rerankerBaseUrl.isBlank()) {
            rerankerBaseUrl = System.getenv().getOrDefault("RAG_RERANKER_BASE_URL", "");
        }
        if (rerankerConnectTimeoutMillis <= 0) {
            rerankerConnectTimeoutMillis = Long.parseLong(System.getenv().getOrDefault("RAG_RERANKER_CONNECT_TIMEOUT_MILLIS", "500"));
        }
        if (rerankerReadTimeoutMillis <= 0) {
            rerankerReadTimeoutMillis = Long.parseLong(System.getenv().getOrDefault("RAG_RERANKER_READ_TIMEOUT_MILLIS", "5000"));
        }
        if (!scoreGateEnabled) {
            scoreGateEnabled = truthy(System.getenv().getOrDefault("RAG_SCOREGATE_ENABLED", "false"));
        }
        if (scoreGateSimilarityThreshold <= 0.0) {
            scoreGateSimilarityThreshold = Double.parseDouble(System.getenv().getOrDefault("RAG_SCOREGATE_SIMILARITY_THRESHOLD", "0.70"));
        }
        if (scoreGateRerankerThreshold <= 0.0) {
            scoreGateRerankerThreshold = Double.parseDouble(System.getenv().getOrDefault("RAG_SCOREGATE_RERANKER_THRESHOLD", "0.08"));
        }
        if (scoreGateSimilarityWeight <= 0.0) {
            scoreGateSimilarityWeight = Double.parseDouble(System.getenv().getOrDefault("RAG_SCOREGATE_SIMILARITY_WEIGHT", "0.30"));
        }
        if (scoreGateBucket2Threshold <= 0.0) {
            scoreGateBucket2Threshold = Double.parseDouble(System.getenv().getOrDefault("RAG_SCOREGATE_BUCKET2_THRESHOLD", "0.255"));
        }
        if (scoreGateBucket3Threshold <= 0.0) {
            scoreGateBucket3Threshold = Double.parseDouble(System.getenv().getOrDefault("RAG_SCOREGATE_BUCKET3_THRESHOLD", "0.15"));
        }
        if (scoreGateMaxK <= 0) {
            scoreGateMaxK = Integer.parseInt(System.getenv().getOrDefault("RAG_SCOREGATE_MAX_K", "10"));
        }
    }

    private static boolean truthy(String value) {
        if (value == null) {
            return false;
        }
        return switch (value.trim().toLowerCase()) {
            case "true", "yes", "1", "on", "enabled" -> true;
            default -> false;
        };
    }
}
