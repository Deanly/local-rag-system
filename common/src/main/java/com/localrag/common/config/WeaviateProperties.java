package com.localrag.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "local-rag.weaviate")
public record WeaviateProperties(String url) {
    public WeaviateProperties {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url must not be blank");
        }
        url = url.trim();
    }
}
