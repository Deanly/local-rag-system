package com.localrag.mcp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "local-rag.mcp")
public record McpSettings(
        String registryUrl,
        String retrievalUrl,
        String indexerUrl
) {
    public McpSettings {
        if (registryUrl == null || registryUrl.isBlank()) {
            registryUrl = System.getenv().getOrDefault("RAG_SOURCE_REGISTRY_URL", "http://source-registry-service:42141");
        }
        if (retrievalUrl == null || retrievalUrl.isBlank()) {
            retrievalUrl = System.getenv().getOrDefault("RAG_RETRIEVAL_URL", "http://retrieval-service:42143");
        }
        if (indexerUrl == null || indexerUrl.isBlank()) {
            indexerUrl = System.getenv().getOrDefault("RAG_INDEXER_URL", "http://indexer-service:42142");
        }
    }
}
