package com.localrag.indexer;

import com.localrag.common.dto.ScanResponse;
import com.localrag.common.embedding.EmbeddingClient;
import com.localrag.common.registry.ProjectRegistration;
import com.localrag.common.registry.RegistrySynchronizer;
import com.localrag.common.registry.SourceRegistry;
import com.localrag.common.registry.SourceRegistryLoader;
import com.localrag.common.registry.SourceRegistryValidator;
import com.localrag.common.registry.SourceRoot;
import com.localrag.common.weaviate.WeaviateClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndexerServiceTests {
    @TempDir
    Path tempDir;

    @Test
    void recordsNonFatalFileIndexFailuresAndContinuesScan() throws Exception {
        Path sourceDir = Files.createDirectories(tempDir.resolve("docs"));
        Files.writeString(sourceDir.resolve("broken.md"), "# Broken\n\nBody text.");
        SourceRoot source = new SourceRoot(
                "project.docs",
                "project",
                "project-docs",
                "project-current-truth",
                sourceDir,
                100,
                true,
                "private",
                List.of("**/*.md"),
                List.of(),
                "registered-default",
                "repo-docs"
        );
        SourceRegistry registry = new SourceRegistry(
                1,
                "test-device",
                "project",
                List.of(new ProjectRegistration("project", "Project", null, "project.docs", List.of("project.docs"), true)),
                List.of(source)
        );
        UUID documentId = UUID.randomUUID();

        SourceRegistryLoader loader = mock(SourceRegistryLoader.class);
        SourceRegistryValidator validator = mock(SourceRegistryValidator.class);
        RegistrySynchronizer synchronizer = mock(RegistrySynchronizer.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        WeaviateClient weaviateClient = mock(WeaviateClient.class);

        when(loader.load(any(Path.class))).thenReturn(registry);
        when(validator.validate(eq(registry), eq(true))).thenReturn(List.of());
        when(jdbcTemplate.query(
                contains("SELECT d.sha256"),
                any(ResultSetExtractor.class),
                eq("project.docs"),
                eq("broken.md")
        )).thenReturn(null);
        when(jdbcTemplate.query(any(PreparedStatementCreator.class), any(ResultSetExtractor.class)))
                .thenReturn(documentId);
        when(jdbcTemplate.query(contains("SELECT weaviate_uuid FROM chunk_state"), any(RowMapper.class), eq(documentId)))
                .thenReturn(List.of());
        when(jdbcTemplate.query(
                contains("SELECT document_id FROM document_state"),
                any(PreparedStatementSetter.class),
                any(RowMapper.class)
        )).thenReturn(List.of());
        when(embeddingClient.embedAll(anyList())).thenThrow(new IllegalStateException("embedding failed"));

        IndexerService service = new IndexerService(
                settings(),
                loader,
                validator,
                synchronizer,
                jdbcTemplate,
                embeddingClient,
                weaviateClient
        );

        ScanResponse response = service.scan("project");

        assertThat(response.documentsDetected()).isEqualTo(1);
        assertThat(response.documentsIndexed()).isZero();
        assertThat(response.chunksIndexed()).isZero();
        assertThat(response.errors()).containsExactly(
                "project.docs:broken.md failed during index-file (index_file_failed)"
        );
        verify(jdbcTemplate).update(
                contains("INSERT INTO document_state("),
                eq("project.docs"),
                eq("broken.md"),
                anyString(),
                eq("broken.md"),
                eq("md"),
                anyLong(),
                anyLong(),
                eq("broken.md"),
                eq("Failed to index the source file")
        );
        verify(jdbcTemplate).update(
                contains("INSERT INTO failure_record"),
                eq("project.docs"),
                eq("project.docs"),
                eq("broken.md"),
                eq("broken.md"),
                eq("index-file"),
                eq("index_file_failed"),
                eq("Failed to index the source file"),
                eq(true)
        );
    }

    private IndexerSettings settings() {
        return new IndexerSettings(
                tempDir.resolve("source-registry.yaml").toString(),
                true,
                "http://localhost:11434",
                "http://localhost:11434",
                "qwen3-embedding:4b",
                "http://weaviate:8080",
                false,
                false,
                300_000,
                10_000,
                1_500,
                120_000
        );
    }
}
