package com.localrag.indexer;

import com.localrag.common.dto.DocumentFetchRequest;
import com.localrag.common.dto.DocumentFetchResponse;
import com.localrag.common.config.SourceRegistryProperties;
import com.localrag.common.registry.SourceRegistry;
import com.localrag.common.registry.SourceRegistryLoader;
import com.localrag.common.registry.SourceRegistryValidator;
import com.localrag.common.registry.SourceRoot;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class DocumentFetchService {
    private final SourceRegistryProperties registrySettings;
    private final SourceRegistryLoader loader;
    private final SourceRegistryValidator validator;

    public DocumentFetchService(
            SourceRegistryProperties registrySettings,
            SourceRegistryLoader loader,
            SourceRegistryValidator validator
    ) {
        this.registrySettings = registrySettings;
        this.loader = loader;
        this.validator = validator;
    }

    public DocumentFetchResponse fetch(DocumentFetchRequest request) {
        if (request.sourceId() == null || request.sourceId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sourceId is required");
        }
        if (request.relativePath() == null || request.relativePath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "relativePath is required");
        }

        SourceRegistry registry = loader.load(Path.of(registrySettings.path()));
        List<String> errors = validator.validate(registry, registrySettings.requirePaths());
        if (!errors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "source registry is invalid: " + String.join("; ", errors));
        }

        SourceRoot source = registry.sources().stream()
                .filter(SourceRoot::active)
                .filter(candidate -> candidate.sourceId().equals(request.sourceId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "unknown sourceId: " + request.sourceId()));

        Path relative = Path.of(request.relativePath()).normalize();
        if (relative.isAbsolute() || relative.toString().isBlank() || relative.startsWith("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "relativePath must stay inside the registered source root");
        }

        String normalizedRelativePath = relative.toString().replace('\\', '/');
        Path root = source.path().toAbsolutePath().normalize();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "relativePath must stay inside the registered source root");
        }
        if (!Files.isRegularFile(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "document not found: " + request.relativePath());
        }

        SourceRoot normalizedSource = new SourceRoot(
                source.sourceId(),
                source.projectId(),
                source.type(),
                source.ssotRole(),
                root,
                source.priority(),
                source.active(),
                source.sensitivityDefault(),
                source.include(),
                source.exclude(),
                source.readPolicy(),
                source.writePolicy()
        );
        if (!new SourcePathFilter(normalizedSource).shouldIndexFile(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "document is excluded by source filters");
        }

        int maxBytes = request.effectiveMaxBytes();
        try (InputStream inputStream = Files.newInputStream(target)) {
            byte[] bytes = inputStream.readNBytes(maxBytes + 1);
            boolean truncated = bytes.length > maxBytes;
            int bytesRead = truncated ? maxBytes : bytes.length;
            String content = new String(bytes, 0, bytesRead, StandardCharsets.UTF_8);
            return new DocumentFetchResponse(
                    source.sourceId(),
                    source.projectId(),
                    normalizedRelativePath,
                    source.sourceId() + ":" + normalizedRelativePath,
                    bytesRead,
                    truncated,
                    content
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to read document", exception);
        }
    }
}
