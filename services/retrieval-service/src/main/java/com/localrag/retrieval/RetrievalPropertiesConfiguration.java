package com.localrag.retrieval;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RetrievalSettings.class)
public class RetrievalPropertiesConfiguration {
}
