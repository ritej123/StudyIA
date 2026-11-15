package com.studyai.exception;

/**
 * Exception levée lorsqu'une adresse email est déjà enregistrée.
 */
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("Cet email existe déjà : " + email);
    }
}
