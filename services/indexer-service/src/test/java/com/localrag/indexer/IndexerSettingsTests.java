package com.localrag.indexer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IndexerSettingsTests {
    @Test
    void defaultsWatchDebounceToTenSeconds() {
        IndexerSettings settings = new IndexerSettings(
                null,
                false,
                null,
                null,
                null,
                false,
                true,
                0,
                0
        );

        assertThat(settings.watchDebounceMillis()).isEqualTo(10_000L);
    }

    @Test
    void preservesExplicitWatchDebounceMillis() {
        IndexerSettings settings = new IndexerSettings(
                "/config/source-registry.yaml",
                true,
                "http://ollama:11434",
                "embedding-model",
                "http://weaviate:8080",
                false,
                true,
                300_000,
                7_500
        );

        assertThat(settings.watchDebounceMillis()).isEqualTo(7_500L);
    }
}
