package com.studyai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyai.exception.InvalidDocumentException;
import com.studyai.exception.StudyAiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class StudyAiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String aiServiceUrl;

    public StudyAiClient(@Value("${AI_SERVICE_URL:http://localhost:8000}") String aiServiceUrl,
                         ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.aiServiceUrl = aiServiceUrl;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(100000);
        this.restClient = RestClient.builder()
            .baseUrl(aiServiceUrl)
            .requestFactory(requestFactory)
            .build();
    }

    public String extract(String filename, byte[] content) {
        java.nio.file.Path tempFile = null;
        try {
            String suffix = filename.toLowerCase().endsWith(".pdf") ? ".pdf" : ".txt";
            tempFile = java.nio.file.Files.createTempFile("studyai-", suffix);
            java.nio.file.Files.write(tempFile, content);
            Process process = new ProcessBuilder(
                "curl", "-sS", "--fail-with-body",
                "-F", "file=@" + tempFile.toAbsolutePath(),
                aiServiceUrl + "/extract"
            ).redirectErrorStream(true).start();
            byte[] responseBytes = process.getInputStream().readAllBytes();
            int exitCode = process.waitFor();
            java.nio.file.Files.deleteIfExists(tempFile);
            tempFile = null;
            String responseBody = new String(responseBytes, StandardCharsets.UTF_8);
            if (exitCode != 0) {
                if (responseBody.contains("\"detail\"")) {
                    cleanupTempFile(tempFile);
                    throw new InvalidDocumentException(extractErrorDetail(responseBody));
                }
                cleanupTempFile(tempFile);
                throw new StudyAiException(503, "Le service d'extraction IA est momentanément indisponible.");
            }
            ExtractResponse extractResponse = objectMapper.readValue(responseBody, ExtractResponse.class);
            if (extractResponse == null || extractResponse.text() == null || extractResponse.text().isBlank()) {
                throw new StudyAiException(502, "Le service IA n'a extrait aucun texte exploitable.");
            }
            return extractResponse.text();
        } catch (JsonProcessingException ex) {
            cleanupTempFile(tempFile);
            log.warn("Réponse invalide du service d'extraction IA.", ex);
            throw new StudyAiException(502, "Le service IA a retourné une réponse invalide.");
        } catch (IOException ex) {
            cleanupTempFile(tempFile);
            log.warn("Le service IA est inaccessible lors de l'extraction.", ex);
            throw new StudyAiException(503, "Le service d'extraction IA est inaccessible.");
        } catch (InterruptedException ex) {
            cleanupTempFile(tempFile);
            Thread.currentThread().interrupt();
            log.warn("L'appel au service IA a été interrompu.");
            throw new StudyAiException(503, "Le service d'extraction IA est inaccessible.");
        }
    }

    private void cleanupTempFile(java.nio.file.Path tempFile) {
        if (tempFile == null) {
            return;
        }
        try {
            java.nio.file.Files.deleteIfExists(tempFile);
        } catch (IOException cleanupException) {
            log.debug("Impossible de supprimer le fichier temporaire d'extraction.", cleanupException);
        }
    }

    private String extractErrorDetail(String responseBody) {
        try {
            JsonNode detail = objectMapper.readTree(responseBody).path("detail");
            if (detail.isTextual() && !detail.asText().isBlank()) {
                return detail.asText();
            }
        } catch (JsonProcessingException parseException) {
            log.debug("La réponse d'erreur du service IA n'est pas au format JSON.", parseException);
        }
        return "Le PDF est illisible ou l'OCR n'a détecté aucun texte.";
    }

    private String extractErrorDetail(RestClientResponseException ex) {
        try {
            JsonNode detail = objectMapper.readTree(ex.getResponseBodyAsString()).path("detail");
            if (detail.isTextual() && !detail.asText().isBlank()) {
                return detail.asText();
            }
        } catch (JsonProcessingException parseException) {
            log.debug("La réponse d'erreur du service IA n'est pas au format JSON.", parseException);
        }
        return "Le PDF est illisible ou l'OCR n'a détecté aucun texte.";
    }

    public String generate(String operation, String context, String prompt, int count) {
        try {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("operation", operation);
            request.put("context", context);
            request.put("prompt", prompt);
            request.put("count", count);
            GenerateResponse response = restClient.post()
                .uri("/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GenerateResponse.class);
            if (response == null || response.result() == null || response.result().isBlank()) {
                throw new StudyAiException(502, "Le service IA a retourné une réponse vide.");
            }
            return response.result();
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value() == 503 ? 503 : 502;
            log.warn("Le service IA a échoué pour l'opération {} (HTTP {}).", operation, ex.getStatusCode().value());
            throw new StudyAiException(status, status == 503
                ? "Le service IA ou sa clé Gemini n'est pas disponible."
                : "Le fournisseur IA n'a pas pu traiter cette demande.");
        } catch (ResourceAccessException ex) {
            log.warn("Le service IA est inaccessible pour l'opération {}.", operation, ex);
            throw new StudyAiException(503, "Le service IA est inaccessible.");
        }
    }

    private record ExtractResponse(String text) {}
    private record GenerateResponse(String result) {}
}
