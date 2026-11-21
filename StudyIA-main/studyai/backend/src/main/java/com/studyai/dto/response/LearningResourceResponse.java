package com.studyai.dto.response;

import java.time.Instant;

public record LearningResourceResponse(
    Long id,
    Long courseId,
    String kind,
    String title,
    String content,
    Instant createdAt
) {}
