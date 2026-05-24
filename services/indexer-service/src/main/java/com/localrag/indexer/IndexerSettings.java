package com.localrag.indexer;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "local-rag.indexer")
public record IndexerSettings(
        String registryPath,
        boolean registryRequirePaths,
        String ollamaBaseUrl,
        String embeddingModel,
        String weaviateUrl,
        boolean embeddingFallbackEnabled,
        boolean watchEnabled,
        long scanIntervalMillis
) {
    public IndexerSettings {
        if (registryPath == null || registryPath.isBlank()) {
            registryPath = System.getenv().getOrDefault("LOCAL_RAG_SOURCE_REGISTRY", "/config/source-registry.example.yaml");
        }
        if (ollamaBaseUrl == null || ollamaBaseUrl.isBlank()) {
            ollamaBaseUrl = System.getenv().getOrDefault("RAG_OLLAMA_BASE_URL", "http://localhost:11434");
        }
        if (embeddingModel == null || embeddingModel.isBlank()) {
            embeddingModel = System.getenv().getOrDefault("RAG_EMBEDDING_MODEL", "qwen3-embedding:4b");
        }
        if (weaviateUrl == null || weaviateUrl.isBlank()) {
            weaviateUrl = System.getenv().getOrDefault("RAG_WEAVIATE_URL", "http://weaviate:8080");
        }
        watchEnabled = Boolean.parseBoolean(System.getenv().getOrDefault(
                "RAG_WATCH_ENABLED",
                Boolean.toString(watchEnabled)
        ));
        if (scanIntervalMillis <= 0) {
            String seconds = System.getenv().getOrDefault("RAG_SCAN_INTERVAL_SECONDS", "300");
            scanIntervalMillis = Long.parseLong(seconds) * 1000L;
        }
    }
}
