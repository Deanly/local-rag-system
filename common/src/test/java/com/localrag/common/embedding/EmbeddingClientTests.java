package com.localrag.common.embedding;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmbeddingClientTests {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void triesNextEndpointWhenSingleEmbeddingEndpointFails() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/embeddings", this::handleEmbedding);
        server.start();

        String workingUrl = "http://127.0.0.1:" + server.getAddress().getPort();
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
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/embed", this::handleBatchEmbedding);
        server.start();

        String workingUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        EmbeddingClient client = new EmbeddingClient(
                List.of("http://127.0.0.1:" + closedLocalPort(), workingUrl),
                "embedding-model",
                false,
                100,
                1_000
        );

        assertEquals(List.of(List.of(0.1, 0.2), List.of(0.3, 0.4)), client.embedAll(List.of("one", "two")));
    }

    private void handleEmbedding(HttpExchange exchange) throws IOException {
        exchange.getRequestBody().readAllBytes();
        writeJson(exchange, """
                {
                  "embedding": [0.25, 0.5]
                }
                """);
    }

    private void handleBatchEmbedding(HttpExchange exchange) throws IOException {
        exchange.getRequestBody().readAllBytes();
        writeJson(exchange, """
                {
                  "embeddings": [[0.1, 0.2], [0.3, 0.4]]
                }
                """);
    }

    private static void writeJson(HttpExchange exchange, String json) throws IOException {
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private static int closedLocalPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
