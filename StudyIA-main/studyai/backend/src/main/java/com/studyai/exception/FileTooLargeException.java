package com.studyai.exception;

/**
 * Exception levée lorsque le fichier téléversé dépasse la taille autorisée.
 */
public class FileTooLargeException extends RuntimeException {
    public FileTooLargeException() {
        super("Le fichier dépasse la taille maximale autorisée de 2 Mo");
    }
}
