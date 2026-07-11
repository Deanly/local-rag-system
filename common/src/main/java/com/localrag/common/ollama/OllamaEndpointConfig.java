package com.localrag.common.ollama;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class OllamaEndpointConfig {
    private OllamaEndpointConfig() {
    }

    public static List<String> parseBaseUrls(String baseUrls) {
        if (baseUrls == null || baseUrls.isBlank()) {
            throw new IllegalArgumentException("At least one Ollama base URL must be configured");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : baseUrls.split(",")) {
            String baseUrl = normalizeBaseUrl(value);
            if (!baseUrl.isBlank()) {
                normalized.add(baseUrl);
            }
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("At least one Ollama base URL must be configured");
        }
        return List.copyOf(normalized);
    }

    public static List<String> parseBaseUrls(List<String> baseUrls) {
        if (baseUrls == null || baseUrls.isEmpty()) {
            throw new IllegalArgumentException("At least one Ollama base URL must be configured");
        }
        List<String> expanded = new ArrayList<>();
        for (String baseUrl : baseUrls) {
            expanded.addAll(parseBaseUrls(baseUrl));
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>(expanded);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("At least one Ollama base URL must be configured");
        }
        return List.copyOf(normalized);
    }

    public static RestClient restClient(String baseUrl, long connectTimeoutMillis, long readTimeoutMillis) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(toTimeoutMillis(connectTimeoutMillis));
        requestFactory.setReadTimeout(toTimeoutMillis(readTimeoutMillis));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    private static int toTimeoutMillis(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static String normalizeBaseUrl(String value) {
        if (value == null) {
            return "";
        }
        String baseUrl = value.trim();
        while (baseUrl.endsWith("/") && baseUrl.length() > "http://x".length()) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
