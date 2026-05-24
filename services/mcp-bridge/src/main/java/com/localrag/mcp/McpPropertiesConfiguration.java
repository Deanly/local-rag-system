package com.localrag.mcp;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(McpSettings.class)
public class McpPropertiesConfiguration {
}
