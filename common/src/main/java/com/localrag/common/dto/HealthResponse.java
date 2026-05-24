package com.localrag.common.dto;

import java.time.Instant;
import java.util.Map;

public record HealthResponse(
        String service,
        String status,
        Instant checkedAt,
        Map<String, Object> details
) {
    public static HealthResponse up(String service) {
        return new HealthResponse(service, "UP", Instant.now(), Map.of());
    }

    public static HealthResponse up(String service, Map<String, Object> details) {
        return new HealthResponse(service, "UP", Instant.now(), details);
    }
}
