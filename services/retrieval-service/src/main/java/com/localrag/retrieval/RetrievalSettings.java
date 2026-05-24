package com.localrag.retrieval;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "local-rag.retrieval")
public record RetrievalSettings(
        String ollamaBaseUrl,
        String embeddingModel,
        String weaviateUrl,
        boolean embeddingFallbackEnabled
) {
    public RetrievalSettings {
        if (ollamaBaseUrl == null || ollamaBaseUrl.isBlank()) {
            ollamaBaseUrl = System.getenv().getOrDefault("RAG_OLLAMA_BASE_URL", "http://localhost:11434");
        }
        if (embeddingModel == null || embeddingModel.isBlank()) {
            embeddingModel = System.getenv().getOrDefault("RAG_EMBEDDING_MODEL", "qwen3-embedding:4b");
        }
        if (weaviateUrl == null || weaviateUrl.isBlank()) {
            weaviateUrl = System.getenv().getOrDefault("RAG_WEAVIATE_URL", "http://weaviate:8080");
        }
    }
}
