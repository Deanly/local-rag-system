package com.localrag.registryservice;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "local-rag.registry")
public record RegistryConfig(
        String path,
        boolean requirePaths
) {
    public RegistryConfig {
        if (path == null || path.isBlank()) {
            path = System.getenv().getOrDefault("LOCAL_RAG_SOURCE_REGISTRY", "/config/source-registry.example.yaml");
        }
    }
}
