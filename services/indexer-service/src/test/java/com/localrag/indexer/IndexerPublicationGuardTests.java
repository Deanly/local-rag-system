package com.localrag.indexer;

import com.localrag.common.embedding.EmbeddingClient;
import com.localrag.common.embedding.EmbeddingContractException;
import com.localrag.common.registry.RegistrySynchronizer;
import com.localrag.common.registry.SourceRegistryLoader;
import com.localrag.common.registry.SourceRegistryValidator;
import com.localrag.common.weaviate.WeaviateClient;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IndexerPublicationGuardTests {
    @Test
    void embeddingContractFailureStopsBeforeExistingChunkDeletionOrPublication() {
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        WeaviateClient weaviateClient = mock(WeaviateClient.class);
        when(embeddingClient.embedAll(anyList())).thenThrow(
                new EmbeddingContractException("fixed dimension mismatch")
        );
        IndexerService service = new IndexerService(
                null,
                mock(SourceRegistryLoader.class),
                mock(SourceRegistryValidator.class),
                mock(RegistrySynchronizer.class),
                jdbcTemplate,
                embeddingClient,
                weaviateClient
        );
        MarkdownChunker.ChunkCandidate candidate = new MarkdownChunker.ChunkCandidate(
                0,
                "document",
                "fixed content",
                0,
                13,
                "fixed-hash",
                new MarkdownChunker.DocumentMetadata(
                        "document", "guide", "current", "canonical", "2026-09-04T00:00:00Z",
                        List.of(), List.of(), List.of(), List.of()
                ),
                List.of("document"),
                1,
                "document",
                "fixed context",
                2
        );

        assertThrows(
                EmbeddingContractException.class,
                () -> service.replaceChunksAfterEmbeddingValidation(
                        null, UUID.randomUUID(), "document.md", null, null, List.of(candidate)
                )
        );

        verifyNoInteractions(jdbcTemplate, weaviateClient);
    }
}
