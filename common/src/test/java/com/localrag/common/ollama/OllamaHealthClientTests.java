package com.localrag.common.ollama;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OllamaHealthClientTests {
    private HttpServer server;
    private String tagsResponse;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void reportsUpWhenRequiredModelsAreAvailable() throws IOException {
        startServer("""
                {
                  "models": [
                    {"name": "qwen3-embedding:4b"},
                    {"name": "qwen3.5:9b"}
                  ]
                }
                """);
        OllamaHealthClient client = client("qwen3-embedding:4b", "qwen3.5:9b");

        Map<String, Object> health = client.health();

        assertEquals("UP", health.get("status"));
    }

    @Test
    void reportsDownWhenRequiredChatModelIsMissing() throws IOException {
        startServer("""
                {
                  "models": [
                    {"name": "qwen3-embedding:4b"}
                  ]
                }
                """);
        OllamaHealthClient client = client("qwen3-embedding:4b", "qwen3.5:9b");

        Map<String, Object> health = client.health();

        assertEquals("DOWN", health.get("status"));
    }

    @Test
    void reportsDownWhenEndpointIsUnavailable() throws IOException {
        OllamaHealthClient client = new OllamaHealthClient(
                "http://127.0.0.1:" + closedLocalPort(),
                "qwen3-embedding:4b",
                "",
                100,
                100
        );

        Map<String, Object> health = client.health();

        assertEquals("DOWN", health.get("status"));
    }

    private OllamaHealthClient client(String embeddingModel, String chatModel) {
        return new OllamaHealthClient(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                embeddingModel,
                chatModel,
                100,
                1_000
        );
    }

    private void startServer(String response) throws IOException {
        tagsResponse = response;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/tags", this::handleTags);
        server.start();
    }

    private void handleTags(HttpExchange exchange) throws IOException {
        byte[] response = tagsResponse.getBytes(StandardCharsets.UTF_8);
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
