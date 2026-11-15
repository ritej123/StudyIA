package com.studyai.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record StudyAttemptRequest(@NotNull List<Integer> answers) {}
