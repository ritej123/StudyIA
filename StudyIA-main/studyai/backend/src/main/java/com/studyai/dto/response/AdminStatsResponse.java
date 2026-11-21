package com.studyai.dto.response;

public record AdminStatsResponse(
    long totalStudents,
    long activeStudents,
    long pendingStudents,
    long disabledStudents,
    long courses,
    long documents,
    long learningResources,
    long summaries,
    long quizzes,
    long flashcardSets,
    long completedActivities
) {}
