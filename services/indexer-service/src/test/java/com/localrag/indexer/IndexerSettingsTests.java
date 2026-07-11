package com.localrag.indexer;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class IndexerSettingsTests {
    @Test
    void exposesTypedDurationsAsMillisForRuntimeClients() {
        IndexerSettings settings = new IndexerSettings(
                false,
                true,
                Duration.ofMinutes(5),
                Duration.ofSeconds(10)
        );

        assertThat(settings.watchDebounceMillis()).isEqualTo(10_000L);
        assertThat(settings.scanInterval()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void preservesExplicitWatchDebounce() {
        IndexerSettings settings = new IndexerSettings(
                false,
                true,
                Duration.ofMinutes(5),
                Duration.ofMillis(7_500)
        );

        assertThat(settings.watchDebounceMillis()).isEqualTo(7_500L);
    }
}
