package com.localrag.indexer;

import com.localrag.common.config.SourceRegistryProperties;
import com.localrag.common.dto.DocumentFetchRequest;
import com.localrag.common.dto.DocumentFetchResponse;
import com.localrag.common.registry.SourceRegistryLoader;
import com.localrag.common.registry.SourceRegistryValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentFetchServiceTests {
    @TempDir
    Path tempDir;

    @Test
    void fetchesDocumentInsideRegisteredSource() throws Exception {
        Path source = Files.createDirectories(tempDir.resolve("docs"));
        Files.writeString(source.resolve("design.md"), "# Design\n\nLocal RAG document fetch.");
        DocumentFetchService service = serviceFor(source);

        DocumentFetchResponse response = service.fetch(new DocumentFetchRequest("project.docs", "design.md", 10_000));

        assertEquals("project.docs", response.sourceId());
        assertEquals("project", response.projectId());
        assertEquals("design.md", response.relativePath());
        assertTrue(response.content().contains("Local RAG document fetch"));
    }

    @Test
    void rejectsPathTraversal() throws Exception {
        Path source = Files.createDirectories(tempDir.resolve("docs"));
        Files.writeString(tempDir.resolve("outside.md"), "outside");
        DocumentFetchService service = serviceFor(source);

        assertThrows(ResponseStatusException.class,
                () -> service.fetch(new DocumentFetchRequest("project.docs", "../outside.md", 10_000)));
    }

    @Test
    void rejectsExcludedDocument() throws Exception {
        Path source = Files.createDirectories(tempDir.resolve("docs"));
        Files.createDirectories(source.resolve("build"));
        Files.writeString(source.resolve("build/generated.md"), "generated");
        DocumentFetchService service = serviceFor(source);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.fetch(new DocumentFetchRequest("project.docs", "build/generated.md", 10_000)));
        assertEquals(400, exception.getStatusCode().value());
    }

    private DocumentFetchService serviceFor(Path source) throws Exception {
        Path registry = tempDir.resolve("source-registry.yaml");
        Files.writeString(registry, """
                version: 1
                device_id: test-device
                default_project_id: project
                projects:
                  - project_id: project
                    display_name: Project
                    primary_source_id: project.docs
                    default_context:
                      - project.docs
                sources:
                  - source_id: project.docs
                    project_id: project
                    type: project-docs
                    ssot_role: project-current-truth
                    path: %s
                    priority: 100
                    active: true
                    sensitivity_default: private
                    include:
                      - "**/*.md"
                    exclude:
                      - "**/build/**"
                    read_policy: registered-default
                    write_policy: repo-docs
                """.formatted(source.toAbsolutePath()));
        SourceRegistryProperties settings = new SourceRegistryProperties(
                registry.toString(),
                true
        );
        return new DocumentFetchService(settings, new SourceRegistryLoader(), new SourceRegistryValidator());
    }
}
