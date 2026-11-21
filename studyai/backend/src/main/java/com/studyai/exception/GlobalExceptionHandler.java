package com.studyai.exception;

import com.studyai.dto.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions pour renvoyer des réponses HTTP standardisées et en français.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            errors.put(fieldName, error.getDefaultMessage());
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse.builder()
                .status(400).code("VALIDATION_ERROR")
                .message("Les données saisies sont invalides")
                .errors(errors).timestamp(Instant.now()).build()
        );
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ErrorResponse.builder().status(409).code("EMAIL_EXISTS")
                .message(ex.getMessage()).timestamp(Instant.now()).build()
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCreds(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ErrorResponse.builder().status(401).code("INVALID_CREDENTIALS")
                .message(ex.getMessage()).timestamp(Instant.now()).build()
        );
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(AccountDisabledException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ErrorResponse.builder().status(403).code("ACCOUNT_DISABLED")
                .message(ex.getMessage()).timestamp(Instant.now()).build()
        );
    }

    @ExceptionHandler(AccountPendingException.class)
    public ResponseEntity<ErrorResponse> handlePending(AccountPendingException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ErrorResponse.builder().status(403).code("ACCOUNT_PENDING")
                .message(ex.getMessage()).timestamp(Instant.now()).build()
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse.builder().status(404).code("USER_NOT_FOUND")
                .message(ex.getMessage()).timestamp(Instant.now()).build()
        );
    }

    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCourseNotFound(CourseNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse.builder().status(404).code("RESOURCE_NOT_FOUND")
                .message(ex.getMessage()).timestamp(Instant.now()).build()
        );
    }

    @ExceptionHandler(LearningResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLearningResourceNotFound(LearningResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ErrorResponse.builder().status(404).code("RESOURCE_NOT_FOUND")
                .message(ex.getMessage()).timestamp(Instant.now()).build()
        );
    }

    @ExceptionHandler(InvalidDocumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDocument(InvalidDocumentException ex) {
        log.warn("Document invalide : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse.builder().status(400).code("INVALID_LEARNING_REQUEST")
                .message(ex.getMessage()).timestamp(Instant.now()).build()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Requête invalide : {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse.builder().status(400).code("INVALID_LEARNING_REQUEST")
                .message(ex.getMessage()).timestamp(Instant.now()).build()
        );
    }

    @ExceptionHandler(StudyAiException.class)
    public ResponseEntity<ErrorResponse> handleStudyAiFailure(StudyAiException ex) {
        log.error("Échec du service IA : {}", ex.getMessage(), ex);
        return ResponseEntity.status(ex.getStatus()).body(
            ErrorResponse.builder().status(ex.getStatus()).code("STUDY_AI_UNAVAILABLE")
                .message(ex.getMessage()).timestamp(Instant.now()).build()
        );
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefresh(InvalidRefreshTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ErrorResponse.builder().status(401).code("INVALID_REFRESH_TOKEN")
                .message(ex.getMessage()).timestamp(Instant.now()).build()
        );
    }

    @ExceptionHandler(FileTooLargeException.class)
    public ResponseEntity<ErrorResponse> handleFileTooLarge(FileTooLargeException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
            ErrorResponse.builder().status(413).code("FILE_TOO_LARGE")
                .message(ex.getMessage())
                .timestamp(Instant.now()).build()
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMultipartTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
            ErrorResponse.builder().status(413).code("FILE_TOO_LARGE")
                .message("Le document dépasse la taille maximale autorisée de 20 Mo.")
                .timestamp(Instant.now()).build()
        );
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedFile(UnsupportedFileTypeException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(
            ErrorResponse.builder().status(415).code("UNSUPPORTED_FILE_TYPE")
                .message(ex.getMessage()).timestamp(Instant.now()).build()
        );
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPassword(InvalidPasswordException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse.builder().status(400).code("INVALID_PASSWORD")
                .message(ex.getMessage()).timestamp(Instant.now()).build()
        );
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
            ErrorResponse.builder().status(429).code("RATE_LIMIT_EXCEEDED")
                .message(ex.getMessage()).timestamp(Instant.now()).build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Erreur non gérée", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse.builder().status(500).code("INTERNAL_ERROR")
                .message("Échec de l'opération, veuillez réessayer plus tard")
                .timestamp(Instant.now()).build()
        );
    }
}