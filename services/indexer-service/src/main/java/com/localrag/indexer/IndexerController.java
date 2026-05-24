package com.localrag.indexer;

import com.localrag.common.dto.HealthResponse;
import com.localrag.common.dto.IndexStatusResponse;
import com.localrag.common.dto.ScanResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class IndexerController {
    private final IndexerService indexerService;

    public IndexerController(IndexerService indexerService) {
        this.indexerService = indexerService;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return HealthResponse.up("indexer-service");
    }

    @GetMapping("/index/status")
    public IndexStatusResponse status() {
        return indexerService.status();
    }

    @PostMapping({"/index/scan", "/index/force"})
    public ScanResponse scan(@RequestParam(name = "projectId", required = false) String projectId) {
        return indexerService.scan(projectId);
    }
}
