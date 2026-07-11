package com.localrag.indexer;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "local-rag.indexer")
public record IndexerSettings(
        boolean embeddingFallbackEnabled,
        boolean watchEnabled,
        Duration scanInterval,
        Duration watchDebounce
) {
    public IndexerSettings {
        scanInterval = positive(scanInterval, "scanInterval");
        watchDebounce = positive(watchDebounce, "watchDebounce");
    }

    public long watchDebounceMillis() {
        return watchDebounce.toMillis();
    }

    private static Duration positive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
