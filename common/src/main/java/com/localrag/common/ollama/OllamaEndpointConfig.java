package com.localrag.common.ollama;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class OllamaEndpointConfig {
    public static final long DEFAULT_CONNECT_TIMEOUT_MILLIS = 1_500L;
    public static final long DEFAULT_READ_TIMEOUT_MILLIS = 120_000L;

    private OllamaEndpointConfig() {
    }

    public static List<String> parseBaseUrls(String baseUrls) {
        if (baseUrls == null || baseUrls.isBlank()) {
            return List.of("http://localhost:11434");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : baseUrls.split(",")) {
            String baseUrl = normalizeBaseUrl(value);
            if (!baseUrl.isBlank()) {
                normalized.add(baseUrl);
            }
        }
        if (normalized.isEmpty()) {
            return List.of("http://localhost:11434");
        }
        return List.copyOf(normalized);
    }

    public static List<String> parseBaseUrls(List<String> baseUrls) {
        if (baseUrls == null || baseUrls.isEmpty()) {
            return List.of("http://localhost:11434");
        }
        List<String> expanded = new ArrayList<>();
        for (String baseUrl : baseUrls) {
            expanded.addAll(parseBaseUrls(baseUrl));
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>(expanded);
        return normalized.isEmpty() ? List.of("http://localhost:11434") : List.copyOf(normalized);
    }

    public static RestClient restClient(String baseUrl, long connectTimeoutMillis, long readTimeoutMillis) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(toTimeoutMillis(connectTimeoutMillis, DEFAULT_CONNECT_TIMEOUT_MILLIS));
        requestFactory.setReadTimeout(toTimeoutMillis(readTimeoutMillis, DEFAULT_READ_TIMEOUT_MILLIS));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public static long positiveOrDefault(long value, long defaultValue) {
        return value > 0 ? value : defaultValue;
    }

    private static int toTimeoutMillis(long value, long defaultValue) {
        long timeout = positiveOrDefault(value, defaultValue);
        return timeout > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) timeout;
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
