package com.localrag.retrieval;

import com.localrag.common.embedding.EmbeddingClient;
import com.localrag.common.config.OllamaProperties;
import com.localrag.common.config.WeaviateProperties;
import com.localrag.common.ollama.OllamaChatClient;
import com.localrag.common.ollama.OllamaHealthClient;
import com.localrag.common.weaviate.WeaviateClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = {"com.localrag.retrieval", "com.localrag.common"})
public class RetrievalServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RetrievalServiceApplication.class, args);
    }

    @Bean
    EmbeddingClient embeddingClient(RetrievalSettings settings, OllamaProperties ollama) {
        return new EmbeddingClient(
                ollama.baseUrls(),
                ollama.embeddingModel(),
                settings.embeddingFallbackEnabled(),
                ollama.connectTimeoutMillis(),
                ollama.readTimeoutMillis()
        );
    }

    @Bean
    OllamaChatClient ollamaChatClient(OllamaProperties ollama) {
        return new OllamaChatClient(
                ollama.baseUrls(),
                ollama.chatModel(),
                ollama.connectTimeoutMillis(),
                ollama.readTimeoutMillis(),
                ollama.chatMaxTokens(),
                ollama.chatTemperature(),
                ollama.chatThinkingEnabled()
        );
    }

    @Bean
    OllamaHealthClient ollamaHealthClient(OllamaProperties ollama) {
        return new OllamaHealthClient(
                ollama.baseUrls(),
                ollama.embeddingModel(),
                ollama.chatModel(),
                ollama.connectTimeoutMillis(),
                ollama.healthTimeoutMillis()
        );
    }

    @Bean
    WeaviateClient weaviateClient(WeaviateProperties weaviate) {
        return new WeaviateClient(weaviate.url());
    }

    @Bean
    LocalRerankerClient localRerankerClient(RetrievalSettings settings) {
        return new HttpLocalRerankerClient(settings);
    }
}
