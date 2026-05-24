package com.localrag.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "local-rag.gateway")
public record GatewaySettings(
        String registryUrl,
        String indexerUrl,
        String retrievalUrl,
        String mcpBridgeUrl
) {
    public GatewaySettings {
        if (registryUrl == null || registryUrl.isBlank()) {
            registryUrl = System.getenv().getOrDefault("RAG_SOURCE_REGISTRY_URL", "http://source-registry-service:42141");
        }
        if (indexerUrl == null || indexerUrl.isBlank()) {
            indexerUrl = System.getenv().getOrDefault("RAG_INDEXER_URL", "http://indexer-service:42142");
        }
        if (retrievalUrl == null || retrievalUrl.isBlank()) {
            retrievalUrl = System.getenv().getOrDefault("RAG_RETRIEVAL_URL", "http://retrieval-service:42143");
        }
        if (mcpBridgeUrl == null || mcpBridgeUrl.isBlank()) {
            mcpBridgeUrl = System.getenv().getOrDefault("RAG_MCP_BRIDGE_URL", "http://mcp-bridge:42144");
        }
    }
}
