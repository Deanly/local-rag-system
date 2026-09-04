package com.localrag.common.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmbeddingClientTests {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private HttpServer server;
    private final List<JsonNode> requests = new ArrayList<>();
    private final List<String> authorizationHeaders = new ArrayList<>();
    private String requiredAuthorization;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void triesNextEndpointWhenSingleEmbeddingEndpointFails() throws IOException {
        startServer();

        String workingUrl = serverUrl();
        EmbeddingClient client = new EmbeddingClient(
                List.of("http://127.0.0.1:" + closedLocalPort(), workingUrl),
                "embedding-model",
                false,
                100,
                1_000
        );

        assertEquals(List.of(0.25, 0.5), client.embed("hello"));
    }

    @Test
    void triesNextEndpointWhenBatchEmbeddingEndpointFails() throws IOException {
        startServer();

        String workingUrl = serverUrl();
        EmbeddingClient client = new EmbeddingClient(
                List.of("http://127.0.0.1:" + closedLocalPort(), workingUrl),
                "embedding-model",
                false,
                100,
                1_000
        );

        assertEquals(List.of(List.of(0.1, 0.2), List.of(0.3, 0.4)), client.embedAll(List.of("one", "two")));
    }

    @Test
    void sendsAuthenticatedQueryAliasKeepAliveAndValidatesDimensions() throws IOException {
        startServer();
        Path tokenFile = privateTokenFile("query-token");
        EmbeddingRequestProfile profile = new EmbeddingRequestProfile(
                "silverstone/rag-query:qwen3-4b-v2",
                "qwen3-embedding:4b",
                "rag-query-qwen3-v2",
                "rag-query",
                tokenFile.toString(),
                true,
                2,
                120
        );
        EmbeddingClient client = new EmbeddingClient(serverUrl(), profile, false, 100, 1_000);

        assertEquals(List.of(0.25, 0.5), client.embed("hello"));
        assertEquals("Bearer query-token", authorizationHeaders.get(0));
        assertEquals("silverstone/rag-query:qwen3-4b-v2", requests.get(0).path("model").asText());
        assertEquals(120, requests.get(0).path("keep_alive").asInt());
        assertEquals("qwen3-embedding:4b", client.profile().provenanceModel());
        assertEquals("rag-query", client.profile().lane());
    }

    @Test
    void sendsAuthenticatedBulkAliasWithZeroKeepAlive() throws IOException {
        startServer();
        Path tokenFile = privateTokenFile("bulk-token");
        EmbeddingRequestProfile profile = new EmbeddingRequestProfile(
                "silverstone/rag-bulk:qwen3-4b-v2",
                "qwen3-embedding:4b",
                "rag-bulk-qwen3-v2",
                "rag-bulk",
                tokenFile.toString(),
                true,
                2,
                0
        );
        EmbeddingClient client = new EmbeddingClient(serverUrl(), profile, false, 100, 1_000);

        assertEquals(List.of(List.of(0.1, 0.2), List.of(0.3, 0.4)), client.embedAll(List.of("one", "two")));
        assertEquals("Bearer bulk-token", authorizationHeaders.get(0));
        assertEquals("silverstone/rag-bulk:qwen3-4b-v2", requests.get(0).path("model").asText());
        assertEquals(0, requests.get(0).path("keep_alive").asInt());
    }

    @Test
    void missingRequiredTokenFailsBeforeNetworkOrFallback() throws IOException {
        startServer();
        EmbeddingRequestProfile profile = new EmbeddingRequestProfile(
                "silverstone/rag-query:qwen3-4b-v2",
                "qwen3-embedding:4b",
                "rag-query-qwen3-v2",
                "rag-query",
                Path.of("missing-token-file").toAbsolutePath().toString(),
                true,
                2,
                120
        );
        EmbeddingClient client = new EmbeddingClient(serverUrl(), profile, false, 100, 1_000);

        assertThrows(EmbeddingContractException.class, () -> client.embed("hello"));
        assertEquals(0, requests.size());
    }

    @Test
    void authenticatedProfileCannotEnableFallbackVectors() throws IOException {
        Path tokenFile = privateTokenFile("query-token");
        EmbeddingRequestProfile profile = new EmbeddingRequestProfile(
                "silverstone/rag-query:qwen3-4b-v2",
                "qwen3-embedding:4b",
                "rag-query-qwen3-v2",
                "rag-query",
                tokenFile.toString(),
                true,
                2,
                120
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new EmbeddingClient("http://127.0.0.1:11434", profile, true, 100, 1_000)
        );
    }

    @Test
    void wrongTokenIsRejectedWithoutFallback() throws IOException {
        startServer();
        requiredAuthorization = "Bearer expected-token";
        Path tokenFile = privateTokenFile("wrong-token");
        EmbeddingRequestProfile profile = new EmbeddingRequestProfile(
                "silverstone/rag-query:qwen3-4b-v2",
                "qwen3-embedding:4b",
                "rag-query-qwen3-v2",
                "rag-query",
                tokenFile.toString(),
                true,
                2,
                120
        );
        EmbeddingClient client = new EmbeddingClient(serverUrl(), profile, false, 100, 1_000);

        assertThrows(IllegalStateException.class, () -> client.embed("hello"));
        assertEquals(1, requests.size());
        assertEquals("Bearer wrong-token", authorizationHeaders.get(0));
    }

    @Test
    void dimensionMismatchFailsClosedEvenWhenDevelopmentFallbackIsEnabled() throws IOException {
        startServer();
        EmbeddingRequestProfile profile = new EmbeddingRequestProfile(
                "qwen3-embedding:4b",
                "qwen3-embedding:4b",
                "dimension-guard-test",
                "rag-query",
                "",
                false,
                3,
                null
        );
        EmbeddingClient client = new EmbeddingClient(serverUrl(), profile, true, 100, 1_000);

        EmbeddingContractException exception = assertThrows(
                EmbeddingContractException.class,
                () -> client.embed("hello")
        );
        assertEquals(true, exception.getMessage().contains("expected 3 but received 2"));
    }

    private void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/embeddings", this::handleEmbedding);
        server.createContext("/api/embed", this::handleBatchEmbedding);
        server.start();
    }

    private String serverUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private Path privateTokenFile(String token) throws IOException {
        Path path = Files.createTempFile("local-rag-binding-", ".token");
        Files.writeString(path, token + "\n");
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"));
        path.toFile().deleteOnExit();
        return path;
    }

    private void handleEmbedding(HttpExchange exchange) throws IOException {
        recordRequest(exchange);
        if (!authorized(exchange)) {
            writeJson(exchange, 401, "{\"error\":\"unauthorized\"}");
            return;
        }
        writeJson(exchange, 200, "{\"embedding\":[0.25,0.5]}");
    }

    private void handleBatchEmbedding(HttpExchange exchange) throws IOException {
        recordRequest(exchange);
        if (!authorized(exchange)) {
            writeJson(exchange, 401, "{\"error\":\"unauthorized\"}");
            return;
        }
        writeJson(exchange, 200, "{\"embeddings\":[[0.1,0.2],[0.3,0.4]]}");
    }

    private void recordRequest(HttpExchange exchange) throws IOException {
        requests.add(OBJECT_MAPPER.readTree(exchange.getRequestBody()));
        authorizationHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
    }

    private boolean authorized(HttpExchange exchange) {
        return requiredAuthorization == null
                || requiredAuthorization.equals(exchange.getRequestHeaders().getFirst("Authorization"));
    }

    private static void writeJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private static int closedLocalPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
