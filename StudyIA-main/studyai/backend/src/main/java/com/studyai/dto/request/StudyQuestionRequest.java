package com.studyai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudyQuestionRequest(@NotBlank @Size(max = 2000) String question) {}
