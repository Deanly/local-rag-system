package com.localrag.common.dto;

import java.time.Instant;
import java.util.List;

public record ScanResponse(
        Instant finishedAt,
        int sourcesScanned,
        int documentsDetected,
        int documentsIndexed,
        int documentsDeleted,
        int chunksIndexed,
        List<String> errors
) {
}
