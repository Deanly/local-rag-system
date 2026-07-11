package com.localrag.indexer;

import com.localrag.common.embedding.EmbeddingClient;
import com.localrag.common.config.OllamaProperties;
import com.localrag.common.config.WeaviateProperties;
import com.localrag.common.ollama.OllamaHealthClient;
import com.localrag.common.registry.RegistrySynchronizer;
import com.localrag.common.registry.SourceRegistryLoader;
import com.localrag.common.registry.SourceRegistryValidator;
import com.localrag.common.weaviate.WeaviateClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
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
    SourceRegistryLoader sourceRegistryLoader(Environment environment) {
        return new SourceRegistryLoader(environment::getProperty);
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
    EmbeddingClient embeddingClient(IndexerSettings settings, OllamaProperties ollama) {
        return new EmbeddingClient(
                ollama.baseUrls(),
                ollama.embeddingModel(),
                settings.embeddingFallbackEnabled(),
                ollama.connectTimeoutMillis(),
                ollama.readTimeoutMillis()
        );
    }

    @Bean
    OllamaHealthClient ollamaHealthClient(OllamaProperties ollama) {
        return new OllamaHealthClient(
                ollama.baseUrls(),
                ollama.embeddingModel(),
                "",
                ollama.connectTimeoutMillis(),
                ollama.healthTimeoutMillis()
        );
    }

    @Bean
    WeaviateClient weaviateClient(WeaviateProperties weaviate) {
        return new WeaviateClient(weaviate.url());
    }
}
