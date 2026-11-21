package com.studyai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class StudyAiClientTest {

    @Test
    void generateSendsJsonRequestBody() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/generate", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            byte[] response = "{\"result\":\"Résumé de test\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var responseBody = exchange.getResponseBody()) {
                responseBody.write(response);
            } finally {
                exchange.close();
            }
        });
        server.start();

        try {
            StudyAiClient client = new StudyAiClient(
                "http://127.0.0.1:" + server.getAddress().getPort(), objectMapper);

            assertThat(client.generate("summary", "Le contenu du document", "Résume le document", 0))
                .isEqualTo("Résumé de test");

            JsonNode sentRequest = objectMapper.readTree(requestBody.get());
            assertThat(contentType.get()).startsWith("application/json");
            assertThat(sentRequest.path("operation").asText()).isEqualTo("summary");
            assertThat(sentRequest.path("context").asText()).isEqualTo("Le contenu du document");
            assertThat(sentRequest.path("prompt").asText()).isEqualTo("Résume le document");
            assertThat(sentRequest.path("count").asInt()).isZero();
        } finally {
            server.stop(0);
        }
    }
}
