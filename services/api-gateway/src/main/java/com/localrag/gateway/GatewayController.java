package com.localrag.gateway;

import com.localrag.common.config.ServiceEndpointsProperties;
import com.localrag.common.dto.HealthResponse;
import com.localrag.common.dto.DocumentFetchRequest;
import com.localrag.common.dto.SearchRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GatewayController {
    private final ServiceEndpointsProperties settings;
    private final RestClient.Builder restClientBuilder;

    public GatewayController(ServiceEndpointsProperties settings, RestClient.Builder restClientBuilder) {
        this.settings = settings;
        this.restClientBuilder = restClientBuilder;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("sourceRegistry", serviceHealth(settings.registryUrl()));
        details.put("indexer", serviceHealth(settings.indexerUrl()));
        details.put("retrieval", serviceHealth(settings.retrievalUrl()));
        details.put("mcpBridge", serviceHealth(settings.mcpBridgeUrl()));
        return allServicesUp(details)
                ? HealthResponse.up("api-gateway", details)
                : HealthResponse.down("api-gateway", details);
    }

    @GetMapping("/registry/projects")
    public Object projects() {
        return get(settings.registryUrl(), "/api/registry/projects");
    }

    @GetMapping("/registry/sources")
    public Object sources() {
        return get(settings.registryUrl(), "/api/registry/sources");
    }

    @PostMapping("/registry/validate")
    public Object validateRegistry() {
        return post(settings.registryUrl(), "/api/registry/validate", null);
    }

    @PostMapping("/registry/reload")
    public Object reloadRegistry() {
        return post(settings.registryUrl(), "/api/registry/reload", null);
    }

    @GetMapping("/index/status")
    public Object indexStatus() {
        return get(settings.indexerUrl(), "/api/index/status");
    }

    @PostMapping({"/index/scan", "/index/force"})
    public Object scan(@RequestParam(name = "projectId", required = false) String projectId) {
        String suffix = projectId == null || projectId.isBlank()
                ? "/api/index/scan"
                : "/api/index/scan?projectId=" + projectId;
        return post(settings.indexerUrl(), suffix, null);
    }

    @PostMapping("/search")
    public Object search(@RequestBody SearchRequest request) {
        return post(settings.retrievalUrl(), "/api/search", request);
    }

    @PostMapping("/answer")
    public Object answer(@RequestBody SearchRequest request) {
        return post(settings.retrievalUrl(), "/api/answer", request);
    }

    @GetMapping("/mcp/rag_list_projects")
    public Object mcpListProjects() {
        return get(settings.mcpBridgeUrl(), "/api/mcp/rag_list_projects");
    }

    @GetMapping("/mcp/rag_list_sources")
    public Object mcpListSources() {
        return get(settings.mcpBridgeUrl(), "/api/mcp/rag_list_sources");
    }

    @PostMapping("/mcp/rag_search")
    public Object mcpSearch(@RequestBody SearchRequest request) {
        return post(settings.mcpBridgeUrl(), "/api/mcp/rag_search", request);
    }

    @PostMapping("/mcp/rag_answer")
    public Object mcpAnswer(@RequestBody SearchRequest request) {
        return post(settings.mcpBridgeUrl(), "/api/mcp/rag_answer", request);
    }

    @GetMapping("/mcp/rag_index_status")
    public Object mcpIndexStatus() {
        return get(settings.mcpBridgeUrl(), "/api/mcp/rag_index_status");
    }

    @PostMapping("/mcp/rag_get_document")
    public Object mcpGetDocument(@RequestBody DocumentFetchRequest request) {
        return post(settings.mcpBridgeUrl(), "/api/mcp/rag_get_document", request);
    }

    @PostMapping("/mcp/rag_force_scan")
    public Object mcpForceScan(@RequestParam(name = "projectId", required = false) String projectId) {
        String suffix = projectId == null || projectId.isBlank()
                ? "/api/mcp/rag_force_scan"
                : "/api/mcp/rag_force_scan?projectId=" + projectId;
        return post(settings.mcpBridgeUrl(), suffix, null);
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<Map<String, Object>> documentPlaceholder(@PathVariable("documentId") String documentId) {
        return ResponseEntity.status(501).body(Map.of(
                "documentId", documentId,
                "status", "not_implemented",
                "message", "Document fetch will be implemented after retrieval storage contracts are expanded."
        ));
    }

    @PostMapping("/documents/get")
    public Object getDocument(@RequestBody DocumentFetchRequest request) {
        return post(settings.indexerUrl(), "/api/documents/get", request);
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<String> downstreamError(RestClientResponseException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(exception.getResponseBodyAsString());
    }

    private Object serviceHealth(String baseUrl) {
        try {
            return get(baseUrl, "/api/health");
        } catch (RuntimeException exception) {
            return Map.of("status", "DOWN", "error", exception.getMessage());
        }
    }

    static boolean allServicesUp(Map<String, Object> details) {
        return details.values().stream().allMatch(GatewayController::serviceUp);
    }

    private static boolean serviceUp(Object serviceHealth) {
        if (serviceHealth instanceof HealthResponse response) {
            return "UP".equals(response.status());
        }
        if (serviceHealth instanceof Map<?, ?> map) {
            Object status = map.get("status");
            return "UP".equals(status);
        }
        return false;
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
