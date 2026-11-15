package com.studyai.exception;

/**
 * Exception levée lorsqu'un compte désactivé tente de se connecter ou d'effectuer une action.
 */
public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException() {
        super("Votre compte a été désactivé");
    }
}
