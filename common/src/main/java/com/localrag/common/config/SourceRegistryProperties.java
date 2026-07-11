package com.localrag.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "local-rag.registry")
public record SourceRegistryProperties(String path, boolean requirePaths) {
    public SourceRegistryProperties {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        path = path.trim();
    }
}
