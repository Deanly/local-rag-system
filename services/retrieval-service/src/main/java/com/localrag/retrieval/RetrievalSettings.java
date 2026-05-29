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
        long ollamaReadTimeoutMillis
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
    }
}
