package com.localrag.common.ollama;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

class OllamaChatClientTests {
    private HttpServer server;
    private String requestBody;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void disablesThinkingAndReadsMessageContent() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", this::handleChat);
        server.start();

        OllamaChatClient client = new OllamaChatClient(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "chat-model",
                100,
                1_000,
                512,
                0.1,
                false
        );

        String answer = client.chat("system", "user");

        assertEquals("answer", answer);
        assertTrue(requestBody.contains("\"think\":false"));
        assertTrue(requestBody.contains("\"num_predict\":512"));
    }

    @Test
    void triesNextEndpointWhenPrimaryEndpointFails() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", this::handleChat);
        server.start();

        String workingUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        OllamaChatClient client = new OllamaChatClient(
                String.join(",", List.of("http://127.0.0.1:" + closedLocalPort(), workingUrl)),
                "chat-model",
                100,
                1_000,
                512,
                0.1,
                false
        );

        assertEquals("answer", client.chat("system", "user"));
    }

    private void handleChat(HttpExchange exchange) throws IOException {
        requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        byte[] response = """
                {
                  "message": {
                    "role": "assistant",
                    "content": " answer "
                  },
                  "done": true
                }
                """.getBytes(StandardCharsets.UTF_8);
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
