package com.localrag.indexer;

import com.localrag.common.dto.HealthResponse;
import com.localrag.common.dto.IndexStatusResponse;
import com.localrag.common.dto.ScanResponse;
import com.localrag.common.dto.DocumentFetchRequest;
import com.localrag.common.dto.DocumentFetchResponse;
import com.localrag.common.ollama.OllamaHealthClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class IndexerController {
    private final IndexerService indexerService;
    private final DocumentFetchService documentFetchService;
    private final OllamaHealthClient ollamaHealthClient;

    public IndexerController(IndexerService indexerService, DocumentFetchService documentFetchService, OllamaHealthClient ollamaHealthClient) {
        this.indexerService = indexerService;
        this.documentFetchService = documentFetchService;
        this.ollamaHealthClient = ollamaHealthClient;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        Map<String, Object> ollama = ollamaHealthClient.health();
        Map<String, Object> details = Map.of("ollama", ollama);
        return "UP".equals(ollama.get("status"))
                ? HealthResponse.up("indexer-service", details)
                : HealthResponse.down("indexer-service", details);
    }

    @GetMapping("/index/status")
    public IndexStatusResponse status() {
        return indexerService.status();
    }

    @PostMapping({"/index/scan", "/index/force"})
    public ScanResponse scan(@RequestParam(name = "projectId", required = false) String projectId) {
        return indexerService.scan(projectId);
    }

    @PostMapping("/documents/get")
    public DocumentFetchResponse getDocument(@RequestBody DocumentFetchRequest request) {
        return documentFetchService.fetch(request);
    }
}
