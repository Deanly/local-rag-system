package com.localrag.registryservice;

import com.localrag.common.config.SourceRegistryProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:local-rag-defaults.properties")
@EnableConfigurationProperties(SourceRegistryProperties.class)
public class RegistryPropertiesConfiguration {
}
