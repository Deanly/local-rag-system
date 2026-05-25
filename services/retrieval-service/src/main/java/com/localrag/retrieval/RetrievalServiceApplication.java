package com.localrag.retrieval;

import com.localrag.common.embedding.EmbeddingClient;
import com.localrag.common.ollama.OllamaChatClient;
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
    EmbeddingClient embeddingClient(RetrievalSettings settings) {
        return new EmbeddingClient(settings.ollamaBaseUrl(), settings.embeddingModel(), settings.embeddingFallbackEnabled());
    }

    @Bean
    OllamaChatClient ollamaChatClient(RetrievalSettings settings) {
        return new OllamaChatClient(settings.ollamaBaseUrl(), settings.chatModel());
    }

    @Bean
    WeaviateClient weaviateClient(RetrievalSettings settings) {
        return new WeaviateClient(settings.weaviateUrl());
    }
}
