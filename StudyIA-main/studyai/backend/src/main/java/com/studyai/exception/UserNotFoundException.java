package com.studyai.exception;

/**
 * Exception levée lorsqu'un utilisateur n'est pas trouvé.
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("Compte introuvable avec l'identifiant : " + id);
    }

    public UserNotFoundException(String email) {
        super("Compte introuvable avec l'email : " + email);
    }
}
