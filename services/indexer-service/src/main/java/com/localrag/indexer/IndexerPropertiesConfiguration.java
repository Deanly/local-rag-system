package com.localrag.indexer;

import com.localrag.common.config.OllamaProperties;
import com.localrag.common.config.SourceRegistryProperties;
import com.localrag.common.config.WeaviateProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:local-rag-defaults.properties")
@EnableConfigurationProperties({
        IndexerSettings.class,
        OllamaProperties.class,
        SourceRegistryProperties.class,
        WeaviateProperties.class
})
public class IndexerPropertiesConfiguration {
}
