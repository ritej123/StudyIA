package com.studyai.controller;

import com.studyai.dto.request.StudyAttemptRequest;
import com.studyai.dto.request.StudyGenerationRequest;
import com.studyai.dto.request.StudyQuestionRequest;
import com.studyai.dto.response.LearningResourceResponse;
import com.studyai.dto.response.StudyAttemptResponse;
import com.studyai.dto.response.StudyMessageResponse;
import com.studyai.service.StudyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;

    @GetMapping("/resources")
    public ResponseEntity<List<LearningResourceResponse>> listResources(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(studyService.listResources(user.getUsername()));
    }

    @PostMapping("/courses/{courseId}/summary")
    public ResponseEntity<LearningResourceResponse> summarize(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long courseId) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(studyService.summarize(user.getUsername(), courseId));
    }

    @PostMapping("/courses/{courseId}/files/{fileId}/summary")
    public ResponseEntity<LearningResourceResponse> summarizeFile(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long courseId,
            @PathVariable Long fileId) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(studyService.summarizeFile(user.getUsername(), courseId, fileId));
    }

    @GetMapping("/resources/{resourceId}/export/pdf")
    public ResponseEntity<byte[]> exportSummaryPdf(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long resourceId) {
        byte[] pdf = studyService.exportSummaryPdf(user.getUsername(), resourceId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename("synthese-studyai.pdf")
            .build());
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @PostMapping("/courses/{courseId}/quiz")
    public ResponseEntity<LearningResourceResponse> generateQuiz(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long courseId,
            @Valid @RequestBody StudyGenerationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(studyService.generateQuiz(user.getUsername(), courseId, request));
    }

    @PostMapping("/courses/{courseId}/flashcards")
    public ResponseEntity<LearningResourceResponse> generateFlashcards(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long courseId,
            @Valid @RequestBody StudyGenerationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(studyService.generateFlashcards(user.getUsername(), courseId, request));
    }

    @GetMapping("/courses/{courseId}/messages")
    public ResponseEntity<List<StudyMessageResponse>> getMessages(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long courseId) {
        return ResponseEntity.ok(studyService.getMessages(user.getUsername(), courseId));
    }

    @PostMapping("/courses/{courseId}/messages")
    public ResponseEntity<StudyMessageResponse> ask(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long courseId,
            @Valid @RequestBody StudyQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(studyService.ask(user.getUsername(), courseId, request));
    }

    @PostMapping("/resources/{resourceId}/attempts")
    public ResponseEntity<StudyAttemptResponse> submitAttempt(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long resourceId,
            @Valid @RequestBody StudyAttemptRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(studyService.submitAttempt(user.getUsername(), resourceId, request));
    }
}
