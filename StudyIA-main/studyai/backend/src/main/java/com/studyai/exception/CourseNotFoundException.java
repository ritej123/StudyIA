package com.studyai.exception;

public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException(Long id) {
        super("Cours introuvable : " + id);
    }
}
