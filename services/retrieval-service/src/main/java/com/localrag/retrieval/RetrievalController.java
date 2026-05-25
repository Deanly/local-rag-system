package com.localrag.retrieval;

import com.localrag.common.dto.HealthResponse;
import com.localrag.common.dto.AnswerResponse;
import com.localrag.common.dto.SearchRequest;
import com.localrag.common.dto.SearchResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RetrievalController {
    private final RetrievalService retrievalService;

    public RetrievalController(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return HealthResponse.up("retrieval-service");
    }

    @PostMapping("/search")
    public SearchResponse search(@RequestBody SearchRequest request) {
        return retrievalService.search(request);
    }

    @PostMapping("/answer")
    public AnswerResponse answer(@RequestBody SearchRequest request) {
        return retrievalService.answer(request);
    }
}
