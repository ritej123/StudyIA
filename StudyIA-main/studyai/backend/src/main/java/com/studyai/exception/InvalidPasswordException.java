package com.studyai.exception;

/**
 * Exception levée lors d'un échec de vérification de l'ancien mot de passe.
 */
public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException() {
        super("L'ancien mot de passe est incorrect");
    }
}
