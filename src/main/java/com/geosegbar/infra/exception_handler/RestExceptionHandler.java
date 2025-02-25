package com.geosegbar.infra.exception_handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.geosegbar.common.WebResponseEntity;
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<WebResponseEntity<String>> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(WebResponseEntity.error("Erro inesperado. Tente novamente mais tarde."));
    }
}
