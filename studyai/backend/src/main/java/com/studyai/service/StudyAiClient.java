package com.studyai.service;

import com.studyai.exception.InvalidDocumentException;
import com.studyai.exception.StudyAiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.net.http.HttpClient;
import java.time.Duration;

@Service
@Slf4j
public class StudyAiClient {

    private final RestClient restClient;

    public StudyAiClient(@Value("${AI_SERVICE_URL:http://localhost:8000}") String aiServiceUrl) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(100));
        this.restClient = RestClient.builder()
            .baseUrl(aiServiceUrl)
            .requestFactory(requestFactory)
            .build();
    }

    public String extract(String filename, byte[] content) {
        try {
            ExtractResponse response = restClient.post()
                .uri("/extract")
                .body(Map.of(
                    "filename", filename,
                    "content_base64", Base64.getEncoder().encodeToString(content)
                ))
                .retrieve()
                .body(ExtractResponse.class);
            if (response == null || response.text() == null || response.text().isBlank()) {
                throw new StudyAiException(502, "Le service IA n'a extrait aucun texte exploitable.");
            }
            return response.text();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw new InvalidDocumentException("Le document ne peut pas être analysé : PDF endommagé ou texte non UTF-8.");
            }
            log.warn("Le service IA a échoué lors de l'extraction (HTTP {}).", ex.getStatusCode().value());
            throw new StudyAiException(503, "Le service d'extraction IA est momentanément indisponible.");
        } catch (ResourceAccessException ex) {
            log.warn("Le service IA est inaccessible lors de l'extraction.", ex);
            throw new StudyAiException(503, "Le service d'extraction IA est inaccessible.");
        }
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
