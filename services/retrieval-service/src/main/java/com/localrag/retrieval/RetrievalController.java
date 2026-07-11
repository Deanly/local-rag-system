package com.localrag.retrieval;

import com.localrag.common.dto.HealthResponse;
import com.localrag.common.dto.AnswerResponse;
import com.localrag.common.dto.SearchRequest;
import com.localrag.common.dto.SearchResponse;
import com.localrag.common.ollama.OllamaHealthClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class RetrievalController {
    private final RetrievalService retrievalService;
    private final OllamaHealthClient ollamaHealthClient;

    public RetrievalController(RetrievalService retrievalService, OllamaHealthClient ollamaHealthClient) {
        this.retrievalService = retrievalService;
        this.ollamaHealthClient = ollamaHealthClient;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        Map<String, Object> ollama = ollamaHealthClient.health();
        Map<String, Object> details = Map.of("ollama", ollama);
        return "UP".equals(ollama.get("status"))
                ? HealthResponse.up("retrieval-service", details)
                : HealthResponse.down("retrieval-service", details);
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
