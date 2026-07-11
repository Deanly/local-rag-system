package com.localrag.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "local-rag.services")
public record ServiceEndpointsProperties(
        String registryUrl,
        String indexerUrl,
        String retrievalUrl,
        String mcpBridgeUrl
) {
    public ServiceEndpointsProperties {
        registryUrl = required(registryUrl, "registryUrl");
        indexerUrl = required(indexerUrl, "indexerUrl");
        retrievalUrl = required(retrievalUrl, "retrievalUrl");
        mcpBridgeUrl = required(mcpBridgeUrl, "mcpBridgeUrl");
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
