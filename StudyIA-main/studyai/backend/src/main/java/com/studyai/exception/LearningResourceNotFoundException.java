package com.studyai.exception;

public class LearningResourceNotFoundException extends RuntimeException {
    public LearningResourceNotFoundException(Long id) {
        super("Ressource d'apprentissage introuvable : " + id);
    }
}
