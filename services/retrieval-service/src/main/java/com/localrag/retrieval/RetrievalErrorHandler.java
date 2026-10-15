package com.localrag.retrieval;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public final class RetrievalErrorHandler {
    @ExceptionHandler(AnswerGenerationDisabledException.class)
    public ResponseEntity<Map<String, Object>> answerGenerationDisabled() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "code", "ANSWER_GENERATION_DISABLED",
                "message", "Use /api/search or rag_search and synthesize from citations in the authorized caller",
                "recommendedTool", "rag_search",
                "retryable", false
        ));
    }
}
