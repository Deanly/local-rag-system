package com.localrag.retrieval;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

interface LocalRerankerClient {
    RerankOutcome rerank(String query, List<CandidateText> candidates);

    static LocalRerankerClient disabled() {
        return (query, candidates) -> RerankOutcome.disabled("reranker-client-disabled");
    }

    record CandidateText(String id, String text) {
        public CandidateText {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("id must not be blank");
            }
            text = text == null ? "" : text;
        }
    }

    record RerankOutcome(
            boolean attempted,
            boolean successful,
            String model,
            Map<String, Double> scores,
            int latencyMs,
            String fallbackReason
    ) {
        public RerankOutcome {
            scores = scores == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(scores));
        }

        static RerankOutcome success(String model, Map<String, Double> scores, int latencyMs) {
            return new RerankOutcome(true, true, model, new LinkedHashMap<>(scores), latencyMs, null);
        }

        static RerankOutcome failure(String reason, int latencyMs) {
            return new RerankOutcome(true, false, null, Map.of(), latencyMs, reason);
        }

        static RerankOutcome disabled(String reason) {
            return new RerankOutcome(false, false, null, Map.of(), 0, reason);
        }
    }
}
