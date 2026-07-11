package com.localrag.mcp;

import com.localrag.common.config.ServiceEndpointsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:local-rag-defaults.properties")
@EnableConfigurationProperties(ServiceEndpointsProperties.class)
public class McpPropertiesConfiguration {
}
