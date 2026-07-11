package com.localrag.retrieval;

import com.localrag.common.config.OllamaProperties;
import com.localrag.common.config.WeaviateProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:local-rag-defaults.properties")
@EnableConfigurationProperties({RetrievalSettings.class, OllamaProperties.class, WeaviateProperties.class})
public class RetrievalPropertiesConfiguration {
}
