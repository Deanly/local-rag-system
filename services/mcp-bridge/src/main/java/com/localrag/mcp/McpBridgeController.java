package com.localrag.mcp;

import com.localrag.common.dto.HealthResponse;
import com.localrag.common.dto.SearchRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class McpBridgeController {
    private final McpSettings settings;
    private final RestClient.Builder restClientBuilder;

    public McpBridgeController(McpSettings settings, RestClient.Builder restClientBuilder) {
        this.settings = settings;
        this.restClientBuilder = restClientBuilder;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return HealthResponse.up("mcp-bridge", Map.of(
                "tools", "rag_list_projects,rag_list_sources,rag_search,rag_get_document,rag_index_status,rag_force_scan"
        ));
    }

    @GetMapping("/mcp/rag_list_projects")
    public Object listProjects() {
        return get(settings.registryUrl(), "/api/registry/projects");
    }

    @GetMapping("/mcp/rag_list_sources")
    public Object listSources() {
        return get(settings.registryUrl(), "/api/registry/sources");
    }

    @PostMapping("/mcp/rag_search")
    public Object search(@RequestBody SearchRequest request) {
        return post(settings.retrievalUrl(), "/api/search", request);
    }

    @GetMapping("/mcp/rag_index_status")
    public Object indexStatus() {
        return get(settings.indexerUrl(), "/api/index/status");
    }

    @PostMapping("/mcp/rag_force_scan")
    public Object forceScan(@RequestParam(name = "projectId", required = false) String projectId) {
        String suffix = projectId == null || projectId.isBlank()
                ? "/api/index/force"
                : "/api/index/force?projectId=" + projectId;
        return post(settings.indexerUrl(), suffix, null);
    }

    private Object get(String baseUrl, String path) {
        return restClientBuilder.baseUrl(baseUrl).build().get().uri(path).retrieve().body(Object.class);
    }

    private Object post(String baseUrl, String path, Object body) {
        RestClient.RequestBodySpec spec = restClientBuilder.baseUrl(baseUrl).build().post().uri(path);
        if (body == null) {
            return spec.retrieve().body(Object.class);
        }
        return spec.body(body).retrieve().body(Object.class);
    }
}
