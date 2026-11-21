package com.studyai.exception;

/**
 * Exception levée lorsqu'un compte est encore en attente de validation.
 */
public class AccountPendingException extends RuntimeException {
    public AccountPendingException() {
        super("Votre compte est en attente de validation");
    }
}
