package com.studyai.exception;

/**
 * Exception levée lorsque le quota de requêtes est dépassé.
 */
public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException() {
        super("Trop de tentatives. Veuillez réessayer dans 15 minutes");
    }
}
