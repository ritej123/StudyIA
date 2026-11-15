package com.studyai.exception;

/**
 * Exception levée lorsque le token de rafraîchissement est invalide ou expiré.
 */
public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("Token de rafraîchissement invalide ou expiré");
    }
}
