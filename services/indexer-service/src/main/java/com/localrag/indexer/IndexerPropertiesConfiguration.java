package com.localrag.indexer;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(IndexerSettings.class)
public class IndexerPropertiesConfiguration {
}
