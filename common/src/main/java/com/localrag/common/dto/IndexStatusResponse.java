package com.localrag.common.dto;

import java.time.Instant;
import java.util.Map;

public record IndexStatusResponse(
        Instant checkedAt,
        long documents,
        long chunks,
        Map<String, Long> documentsByStatus,
        Map<String, Long> jobsByStatus
) {
}
