package com.localrag.indexer;

import com.localrag.common.embedding.EmbeddingClient;
import com.localrag.common.registry.RegistrySynchronizer;
import com.localrag.common.registry.SourceRegistryLoader;
import com.localrag.common.registry.SourceRegistryValidator;
import com.localrag.common.weaviate.WeaviateClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.localrag.indexer", "com.localrag.common"})
public class IndexerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IndexerServiceApplication.class, args);
    }

    @Bean
    SourceRegistryLoader sourceRegistryLoader() {
        return new SourceRegistryLoader();
    }

    @Bean
    SourceRegistryValidator sourceRegistryValidator() {
        return new SourceRegistryValidator();
    }

    @Bean
    RegistrySynchronizer registrySynchronizer(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        return new RegistrySynchronizer(jdbcTemplate, new TransactionTemplate(transactionManager));
    }

    @Bean
    EmbeddingClient embeddingClient(IndexerSettings settings) {
        return new EmbeddingClient(
                settings.ollamaBaseUrls(),
                settings.embeddingModel(),
                settings.embeddingFallbackEnabled(),
                settings.ollamaConnectTimeoutMillis(),
                settings.ollamaReadTimeoutMillis()
        );
    }

    @Bean
    WeaviateClient weaviateClient(IndexerSettings settings) {
        return new WeaviateClient(settings.weaviateUrl());
    }
}
