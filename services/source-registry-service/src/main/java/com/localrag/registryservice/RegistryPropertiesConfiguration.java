package com.localrag.registryservice;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RegistryConfig.class)
public class RegistryPropertiesConfiguration {
}
