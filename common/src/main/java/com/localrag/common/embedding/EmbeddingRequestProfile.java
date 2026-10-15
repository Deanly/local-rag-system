package com.localrag.common.embedding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public record EmbeddingRequestProfile(
        String requestModel,
        String provenanceModel,
        String bindingId,
        String lane,
        String bearerTokenFile,
        boolean authenticationRequired,
        int expectedDimensions,
        Integer keepAliveSeconds
) {
    private static final Set<PosixFilePermission> FORBIDDEN_TOKEN_PERMISSIONS = Set.of(
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_WRITE,
            PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_READ,
            PosixFilePermission.OTHERS_WRITE,
            PosixFilePermission.OTHERS_EXECUTE
    );

    public EmbeddingRequestProfile {
        requestModel = requireNonBlank(requestModel, "requestModel");
        provenanceModel = requireNonBlank(provenanceModel, "provenanceModel");
        bindingId = requireNonBlank(bindingId, "bindingId");
        lane = requireNonBlank(lane, "lane");
        bearerTokenFile = bearerTokenFile == null ? "" : bearerTokenFile.trim();
        if (expectedDimensions < 0) {
            throw new IllegalArgumentException("expectedDimensions must be zero or positive");
        }
        if (keepAliveSeconds != null && keepAliveSeconds < 0) {
            keepAliveSeconds = null;
        }
    }

    public static EmbeddingRequestProfile direct(String model, String lane) {
        return new EmbeddingRequestProfile(
                model,
                model,
                "direct-ollama-legacy",
                lane,
                "",
                false,
                0,
                null
        );
    }

    public static EmbeddingRequestProfile fromEnvironment(String fallbackModel, String defaultLane) {
        String requestModel = env("RAG_EMBEDDING_REQUEST_MODEL", fallbackModel);
        String provenanceModel = env("RAG_EMBEDDING_PROVENANCE_MODEL", fallbackModel);
        String bindingId = env("RAG_EMBEDDING_BINDING_ID", "direct-ollama-legacy");
        String lane = env("RAG_EMBEDDING_LANE", defaultLane);
        String tokenFile = env("RAG_EMBEDDING_BINDING_TOKEN_FILE", "");
        boolean authenticationRequired = truthy(env("RAG_EMBEDDING_AUTHENTICATION_REQUIRED", "false"));
        int expectedDimensions = parseNonNegativeInt(
                env("RAG_EMBEDDING_EXPECTED_DIMENSIONS", "0"),
                "RAG_EMBEDDING_EXPECTED_DIMENSIONS"
        );
        int keepAlive = Integer.parseInt(env("RAG_EMBEDDING_KEEP_ALIVE_SECONDS", "-1"));
        return new EmbeddingRequestProfile(
                requestModel,
                provenanceModel,
                bindingId,
                lane,
                tokenFile,
                authenticationRequired,
                expectedDimensions,
                keepAlive < 0 ? null : keepAlive
        );
    }

    public static String baseUrlsFromEnvironment(String fallbackBaseUrls) {
        return env("RAG_EMBEDDING_BASE_URLS", fallbackBaseUrls);
    }

    String authorizationHeader() {
        if (bearerTokenFile.isBlank()) {
            if (authenticationRequired) {
                throw new EmbeddingContractException("embedding binding token file is required");
            }
            return null;
        }
        Path path = Path.of(bearerTokenFile);
        if (Files.isSymbolicLink(path) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new EmbeddingContractException("embedding binding token must be a regular non-symlink file");
        }
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS);
            if (permissions.stream().anyMatch(FORBIDDEN_TOKEN_PERMISSIONS::contains)) {
                throw new EmbeddingContractException("embedding binding token must not be accessible by group or others");
            }
        } catch (UnsupportedOperationException ignored) {
            // Non-POSIX filesystems still retain the regular-file and non-symlink checks.
        } catch (IOException exception) {
            throw new EmbeddingContractException("embedding binding token permissions are unreadable", exception);
        }
        try {
            String token = Files.readString(path).trim();
            if (token.isEmpty()) {
                throw new EmbeddingContractException("embedding binding token is empty");
            }
            return "Bearer " + token;
        } catch (IOException exception) {
            throw new EmbeddingContractException("embedding binding token is unreadable", exception);
        }
    }

    void validateDimensions(int actualDimensions) {
        if (expectedDimensions > 0 && actualDimensions != expectedDimensions) {
            throw new EmbeddingContractException(
                    "embedding dimension mismatch for " + bindingId
                            + ": expected " + expectedDimensions + " but received " + actualDimensions
            );
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static int parseNonNegativeInt(String value, String name) {
        int parsed = Integer.parseInt(value);
        if (parsed < 0) {
            throw new IllegalArgumentException(name + " must be zero or positive");
        }
        return parsed;
    }

    private static boolean truthy(String value) {
        return switch (value.trim().toLowerCase()) {
            case "true", "yes", "1", "on", "enabled" -> true;
            default -> false;
        };
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
