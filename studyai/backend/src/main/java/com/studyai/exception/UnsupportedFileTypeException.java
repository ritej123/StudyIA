package com.studyai.exception;

/**
 * Exception levée lorsque le type de fichier n'est pas autorisé.
 */
public class UnsupportedFileTypeException extends RuntimeException {
    public UnsupportedFileTypeException() {
        super("Type de fichier non supporté. Seuls JPG, PNG et WebP sont acceptés");
    }
}
