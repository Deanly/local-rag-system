package com.localrag.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "local-rag.ollama")
public record OllamaProperties(
        String baseUrl,
        String baseUrls,
        String embeddingModel,
        String chatModel,
        Duration connectTimeout,
        Duration readTimeout,
        Duration healthTimeout,
        int chatMaxTokens,
        double chatTemperature,
        boolean chatThinkingEnabled
) {
    public OllamaProperties {
        baseUrl = normalized(baseUrl);
        baseUrls = normalized(baseUrls);
        embeddingModel = normalized(embeddingModel);
        chatModel = normalized(chatModel);
        if (baseUrls.isBlank()) {
            baseUrls = baseUrl;
        }
        connectTimeout = required(connectTimeout, "connectTimeout");
        readTimeout = required(readTimeout, "readTimeout");
        healthTimeout = required(healthTimeout, "healthTimeout");
        if (chatMaxTokens <= 0) {
            throw new IllegalArgumentException("chatMaxTokens must be positive");
        }
        if (chatTemperature < 0.0) {
            throw new IllegalArgumentException("chatTemperature must not be negative");
        }
    }

    public long connectTimeoutMillis() {
        return connectTimeout.toMillis();
    }

    public long readTimeoutMillis() {
        return readTimeout.toMillis();
    }

    public long healthTimeoutMillis() {
        return healthTimeout.toMillis();
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim();
    }

    private static Duration required(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
