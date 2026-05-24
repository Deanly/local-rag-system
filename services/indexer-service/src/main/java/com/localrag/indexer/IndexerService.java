package com.localrag.indexer;

import com.localrag.common.dto.IndexStatusResponse;
import com.localrag.common.dto.ScanResponse;
import com.localrag.common.embedding.EmbeddingClient;
import com.localrag.common.registry.RegistrySynchronizer;
import com.localrag.common.registry.SourceRegistry;
import com.localrag.common.registry.SourceRegistryLoader;
import com.localrag.common.registry.SourceRegistryValidator;
import com.localrag.common.registry.SourceRoot;
import com.localrag.common.weaviate.WeaviateClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class IndexerService {
    private final IndexerSettings settings;
    private final SourceRegistryLoader loader;
    private final SourceRegistryValidator validator;
    private final RegistrySynchronizer synchronizer;
    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingClient embeddingClient;
    private final WeaviateClient weaviateClient;
    private final MarkdownChunker chunker = new MarkdownChunker();

    public IndexerService(
            IndexerSettings settings,
            SourceRegistryLoader loader,
            SourceRegistryValidator validator,
            RegistrySynchronizer synchronizer,
            JdbcTemplate jdbcTemplate,
            EmbeddingClient embeddingClient,
            WeaviateClient weaviateClient
    ) {
        this.settings = settings;
        this.loader = loader;
        this.validator = validator;
        this.synchronizer = synchronizer;
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingClient = embeddingClient;
        this.weaviateClient = weaviateClient;
    }

    public synchronized ScanResponse scan(String projectId) {
        SourceRegistry registry = loader.load(Path.of(settings.registryPath()));
        List<String> errors = new ArrayList<>(validator.validate(registry, settings.registryRequirePaths()));
        if (!errors.isEmpty()) {
            return new ScanResponse(Instant.now(), 0, 0, 0, 0, 0, errors);
        }
        synchronizer.synchronize(registry);
        weaviateClient.ensureSchema();

        List<SourceRoot> sources = registry.sources().stream()
                .filter(SourceRoot::active)
                .filter(source -> projectId == null || projectId.isBlank() || source.projectId().equals(projectId))
                .toList();

        int detected = 0;
        int indexed = 0;
        int deleted = 0;
        int chunks = 0;
        for (SourceRoot source : sources) {
            try {
                ScanCounters counters = scanSource(source);
                detected += counters.detected();
                indexed += counters.indexed();
                deleted += counters.deleted();
                chunks += counters.chunks();
            } catch (RuntimeException exception) {
                errors.add(source.sourceId() + ": " + exception.getMessage());
            }
        }
        return new ScanResponse(Instant.now(), sources.size(), detected, indexed, deleted, chunks, errors);
    }

    public IndexStatusResponse status() {
        long documents = count("SELECT count(*) FROM document_state");
        long chunks = count("SELECT count(*) FROM chunk_state");
        return new IndexStatusResponse(
                Instant.now(),
                documents,
                chunks,
                groupedCounts("SELECT status, count(*) FROM document_state GROUP BY status"),
                groupedCounts("SELECT status, count(*) FROM index_job GROUP BY status")
        );
    }

    private ScanCounters scanSource(SourceRoot source) {
        if (!Files.exists(source.path())) {
            throw new IllegalArgumentException("source path does not exist: " + source.path());
        }
        Map<String, Path> seen = new LinkedHashMap<>();
        SourcePathFilter pathFilter = new SourcePathFilter(source);
        try {
            Files.walkFileTree(source.path(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!pathFilter.shouldVisitDirectory(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (isSupported(file) && pathFilter.shouldIndexFile(file)) {
                        seen.put(source.path().relativize(file).toString(), file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            throw new IllegalArgumentException("failed to walk source: " + source.path(), exception);
        }

        int indexed = 0;
        int chunks = 0;
        for (Map.Entry<String, Path> entry : seen.entrySet()) {
            IndexFileResult result = indexFile(source, entry.getKey(), entry.getValue());
            if (result.indexed()) {
                indexed++;
            }
            chunks += result.chunks();
        }
        int deleted = removeStale(source, seen.keySet().stream().toList());
        return new ScanCounters(seen.size(), indexed, deleted, chunks);
    }

    private IndexFileResult indexFile(SourceRoot source, String relativePath, Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            String sha256 = sha256(bytes);
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            ExistingDocument existing = jdbcTemplate.query("""
                            SELECT d.sha256, d.status, count(c.chunk_id) AS chunk_count
                            FROM document_state d
                            LEFT JOIN chunk_state c ON c.document_id = d.document_id
                            WHERE d.source_id = ? AND d.relative_path = ?
                            GROUP BY d.document_id, d.sha256, d.status
                            """,
                    rs -> rs.next()
                            ? new ExistingDocument(rs.getString("sha256"), rs.getString("status"), rs.getInt("chunk_count"))
                            : null,
                    source.sourceId(), relativePath);
            if (existing != null
                    && sha256.equals(existing.sha256())
                    && "indexed".equals(existing.status())
                    && existing.chunkCount() > 0) {
                return new IndexFileResult(false, 0);
            }

            UUID documentId = upsertDocument(source, relativePath, path, attrs, sha256);
            deleteChunks(documentId);
            String text = new String(bytes, StandardCharsets.UTF_8);
            List<MarkdownChunker.ChunkCandidate> candidates = chunker.chunk(text, 1600);
            List<List<Double>> vectors = embeddingClient.embedAll(candidates.stream()
                    .map(MarkdownChunker.ChunkCandidate::content)
                    .toList());
            for (int i = 0; i < candidates.size(); i++) {
                upsertChunk(source, documentId, relativePath, path, candidates.get(i), vectors.get(i));
            }
            jdbcTemplate.update("UPDATE document_state SET status = 'indexed', last_indexed_at = now(), updated_at = now() WHERE document_id = ?", documentId);
            return new IndexFileResult(true, candidates.size());
        } catch (IOException exception) {
            throw new IllegalArgumentException("failed to index file " + path, exception);
        }
    }

    private UUID upsertDocument(SourceRoot source, String relativePath, Path path, BasicFileAttributes attrs, String sha256) {
        String absoluteHash = EmbeddingClient.sha256Hex(path.toAbsolutePath().toString());
        UUID existing = jdbcTemplate.query("""
                        SELECT document_id FROM document_state WHERE source_id = ? AND relative_path = ?
                        """,
                rs -> rs.next() ? rs.getObject("document_id", UUID.class) : null,
                source.sourceId(), relativePath);
        if (existing != null) {
            jdbcTemplate.update("""
                            UPDATE document_state
                            SET absolute_path_hash = ?, file_name = ?, extension = ?, file_size = ?, mtime_ns = ?,
                                sha256 = ?, status = 'changed', last_detected_at = now(), updated_at = now()
                            WHERE document_id = ?
                            """,
                    absoluteHash, path.getFileName().toString(), extension(path), attrs.size(),
                    attrs.lastModifiedTime().toInstant().toEpochMilli() * 1_000_000L, sha256, existing);
            return existing;
        }
        return jdbcTemplate.query("""
                        INSERT INTO document_state(source_id, relative_path, absolute_path_hash, file_name, extension,
                                                   file_size, mtime_ns, sha256, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'queued')
                        RETURNING document_id
                        """,
                rs -> {
                    rs.next();
                    return rs.getObject("document_id", UUID.class);
                },
                source.sourceId(), relativePath, absoluteHash, path.getFileName().toString(), extension(path),
                attrs.size(), attrs.lastModifiedTime().toInstant().toEpochMilli() * 1_000_000L, sha256);
    }

    private void upsertChunk(SourceRoot source, UUID documentId, String relativePath, Path path, MarkdownChunker.ChunkCandidate candidate, List<Double> vector) {
        String chunkId = documentId + ":" + candidate.index();
        UUID weaviateId = UUID.nameUUIDFromBytes(chunkId.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("chunkId", chunkId);
        properties.put("documentId", documentId.toString());
        properties.put("projectId", source.projectId());
        properties.put("sourceId", source.sourceId());
        properties.put("sourceType", source.type());
        properties.put("ssotRole", source.ssotRole());
        properties.put("relativePath", relativePath);
        properties.put("fileName", path.getFileName().toString());
        properties.put("extension", extension(path));
        properties.put("headingPath", candidate.headingPath());
        properties.put("chunkIndex", candidate.index());
        properties.put("content", candidate.content());
        properties.put("contentHash", candidate.contentHash());
        properties.put("sensitivity", source.sensitivityDefault());
        properties.put("indexedAt", Instant.now().toString());
        weaviateClient.upsert(weaviateId.toString(), vector, properties);
        jdbcTemplate.update("""
                        INSERT INTO chunk_state(chunk_id, document_id, source_id, chunk_index, heading_path, content_hash,
                                                token_estimate, weaviate_uuid)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (chunk_id)
                        DO UPDATE SET heading_path = EXCLUDED.heading_path,
                                      content_hash = EXCLUDED.content_hash,
                                      token_estimate = EXCLUDED.token_estimate,
                                      weaviate_uuid = EXCLUDED.weaviate_uuid,
                                      updated_at = now()
                        """,
                chunkId, documentId, source.sourceId(), candidate.index(), candidate.headingPath(),
                candidate.contentHash(), Math.max(1, candidate.content().length() / 4), weaviateId);
    }

    private void deleteChunks(UUID documentId) {
        List<UUID> ids = jdbcTemplate.query("""
                        SELECT weaviate_uuid FROM chunk_state WHERE document_id = ? AND weaviate_uuid IS NOT NULL
                        """,
                (rs, rowNum) -> rs.getObject("weaviate_uuid", UUID.class),
                documentId);
        ids.forEach(weaviateClient::deleteQuietly);
        jdbcTemplate.update("DELETE FROM chunk_state WHERE document_id = ?", documentId);
    }

    private int removeStale(SourceRoot source, List<String> seen) {
        List<UUID> staleDocumentIds;
        if (seen.isEmpty()) {
            staleDocumentIds = jdbcTemplate.query("""
                            SELECT document_id FROM document_state WHERE source_id = ? AND status <> 'removed'
                            """,
                    (rs, rowNum) -> rs.getObject("document_id", UUID.class),
                    source.sourceId());
        } else {
            staleDocumentIds = jdbcTemplate.query("""
                            SELECT document_id FROM document_state
                            WHERE source_id = ? AND status <> 'removed' AND NOT (relative_path = ANY (?))
                            """,
                    ps -> {
                        ps.setString(1, source.sourceId());
                        ps.setArray(2, ps.getConnection().createArrayOf("text", seen.toArray(String[]::new)));
                    },
                    (rs, rowNum) -> rs.getObject("document_id", UUID.class));
        }
        for (UUID documentId : staleDocumentIds) {
            deleteChunks(documentId);
            jdbcTemplate.update("UPDATE document_state SET status = 'removed', updated_at = now() WHERE document_id = ?", documentId);
        }
        return staleDocumentIds.size();
    }

    private long count(String sql) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0 : count;
    }

    private Map<String, Long> groupedCounts(String sql) {
        Map<String, Long> result = new LinkedHashMap<>();
        jdbcTemplate.query(sql, (RowCallbackHandler) rs -> result.put(rs.getString(1), rs.getLong(2)));
        return result;
    }

    private static boolean isSupported(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".md") || name.endsWith(".markdown") || name.endsWith(".txt");
    }

    private static String extension(Path path) {
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase() : "";
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    private record ScanCounters(int detected, int indexed, int deleted, int chunks) {
    }

    private record IndexFileResult(boolean indexed, int chunks) {
    }

    private record ExistingDocument(String sha256, String status, int chunkCount) {
    }
}
