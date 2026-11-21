package com.studyai.dto.response;

import java.time.Instant;
import java.util.List;

public record CourseResponse(
    Long id,
    String title,
    String subject,
    String description,
    Instant createdAt,
    List<CourseFileResponse> files
) {
    public record CourseFileResponse(
        Long id,
        String filename,
        String contentType,
        Long sizeBytes,
        Instant uploadedAt
    ) {}
}
