package com.studyai.exception;

public class StudyAiException extends RuntimeException {
    private final int status;

    public StudyAiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
