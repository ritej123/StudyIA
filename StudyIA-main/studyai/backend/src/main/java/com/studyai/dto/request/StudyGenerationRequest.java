package com.studyai.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record StudyGenerationRequest(@Min(3) @Max(15) int count) {}
