package com.studyai.controller;

import com.studyai.dto.request.CourseRequest;
import com.studyai.dto.response.CourseResponse;
import com.studyai.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<List<CourseResponse>> list(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(courseService.list(user.getUsername()));
    }

    @PostMapping
    public ResponseEntity<CourseResponse> create(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(user.getUsername(), request));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long courseId) {
        courseService.delete(user.getUsername(), courseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{courseId}/files", consumes = "multipart/form-data")
    public ResponseEntity<CourseResponse.CourseFileResponse> upload(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(courseService.upload(user.getUsername(), courseId, file));
    }

    @DeleteMapping("/{courseId}/files/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long courseId,
            @PathVariable Long fileId) {
        courseService.deleteFile(user.getUsername(), courseId, fileId);
        return ResponseEntity.noContent().build();
    }
}
