package com.studyai.exception;

/**
 * Exception levée lors d'un échec d'authentification (email ou mot de passe invalide).
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Email ou mot de passe incorrect");
    }
}
