package com.geosegbar.exceptions.exception_handler;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.exceptions.BusinessRuleException;
import com.geosegbar.exceptions.DatabaseException;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.FileStorageException;
import com.geosegbar.exceptions.ForbiddenException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.InvalidTokenException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.exceptions.TokenExpiredException;
import com.geosegbar.exceptions.UnauthorizedException;
import com.geosegbar.exceptions.UnsupportedFileTypeException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<WebResponseEntity<String>> handleNotFoundException(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(WebResponseEntity.error(ex.getMessage()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<WebResponseEntity<String>> handleDuplicateResourceException(DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(WebResponseEntity.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<WebResponseEntity<String>> handleInvalidInputException(InvalidInputException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(WebResponseEntity.error(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<WebResponseEntity<String>> handleUnauthorizedException(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(WebResponseEntity.error(ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<WebResponseEntity<String>> handleForbiddenException(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(WebResponseEntity.error(ex.getMessage()));
    }

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<WebResponseEntity<String>> handleDatabaseException(DatabaseException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(WebResponseEntity.error("Erro no banco de dados: " + ex.getMessage()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<WebResponseEntity<String>> handleBusinessRuleException(BusinessRuleException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(WebResponseEntity.error(ex.getMessage()));
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<WebResponseEntity<String>> handleFileStorageException(FileStorageException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(WebResponseEntity.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<WebResponseEntity<String>> handleInvalidTokenException(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(WebResponseEntity.error(ex.getMessage()));
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<WebResponseEntity<String>> handleTokenException(TokenExpiredException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(WebResponseEntity.error(ex.getMessage()));
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<WebResponseEntity<String>> handleUnsupportedFileTypeException(UnsupportedFileTypeException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(WebResponseEntity.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<WebResponseEntity<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(WebResponseEntity.errorValidation("Erro de validação", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<WebResponseEntity<Map<String, String>>> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();

        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            String field = propertyPath.contains(".")
                    ? propertyPath.substring(propertyPath.lastIndexOf('.') + 1) : propertyPath;
            errors.put(field, violation.getMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(WebResponseEntity.errorValidation("Inválido!", errors));
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<WebResponseEntity<?>> handleTransactionSystemException(TransactionSystemException ex) {
        Throwable cause = ex.getRootCause();

        if (cause instanceof jakarta.validation.ConstraintViolationException) {
            jakarta.validation.ConstraintViolationException violationException
                    = (jakarta.validation.ConstraintViolationException) cause;

            Map<String, String> errors = new HashMap<>();

            for (ConstraintViolation<?> violation : violationException.getConstraintViolations()) {
                String propertyPath = violation.getPropertyPath().toString();
                String field = propertyPath.contains(".")
                        ? propertyPath.substring(propertyPath.lastIndexOf('.') + 1) : propertyPath;
                errors.put(field, violation.getMessage());
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(WebResponseEntity.errorValidation("Inválido!", errors));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(WebResponseEntity.error("Erro na transação: "
                        + (cause != null ? cause.getMessage() : ex.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<WebResponseEntity<String>> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(WebResponseEntity.error("Erro inesperado. Tente novamente mais tarde."));
    }
}
