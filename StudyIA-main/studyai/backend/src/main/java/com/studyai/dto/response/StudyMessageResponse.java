package com.studyai.dto.response;

import java.time.Instant;

public record StudyMessageResponse(Long id, String role, String content, Instant createdAt) {}
