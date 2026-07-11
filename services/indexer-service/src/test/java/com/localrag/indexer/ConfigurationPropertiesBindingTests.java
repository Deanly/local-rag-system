package com.localrag.indexer;

import com.localrag.common.config.OllamaProperties;
import com.localrag.common.config.SourceRegistryProperties;
import com.localrag.common.config.WeaviateProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationPropertiesBindingTests {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(IndexerPropertiesConfiguration.class);

    @Test
    void bindsSharedDefaultsAndTypedOverridesWithoutDirectEnvironmentReads() {
        contextRunner
                .withPropertyValues(
                        "local-rag.ollama.base-url=http://configured-ollama:11434",
                        "local-rag.ollama.connect-timeout=250ms",
                        "local-rag.indexer.watch-debounce=7500ms",
                        "local-rag.registry.path=/configured/source-registry.yaml"
                )
                .run(context -> {
                    OllamaProperties ollama = context.getBean(OllamaProperties.class);
                    IndexerSettings indexer = context.getBean(IndexerSettings.class);
                    SourceRegistryProperties registry = context.getBean(SourceRegistryProperties.class);
                    WeaviateProperties weaviate = context.getBean(WeaviateProperties.class);

                    assertThat(ollama.baseUrls()).isEqualTo("http://configured-ollama:11434");
                    assertThat(ollama.connectTimeout()).isEqualTo(Duration.ofMillis(250));
                    assertThat(ollama.chatMaxTokens()).isEqualTo(512);
                    assertThat(indexer.watchDebounce()).isEqualTo(Duration.ofMillis(7_500));
                    assertThat(registry.path()).isEqualTo("/configured/source-registry.yaml");
                    assertThat(weaviate.url()).isEqualTo("http://weaviate:8080");
                });
    }
}
