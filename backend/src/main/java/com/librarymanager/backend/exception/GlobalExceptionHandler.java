package com.librarymanager.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.librarymanager.backend.dto.response.ErrorResponse;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for the library management system.
 * 
 * This class provides centralized exception handling using Spring's @ControllerAdvice
 * annotation, following Spring Boot best practices for error handling.
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ========== Custom Library Exceptions ==========

    /**
     * Handles ResourceNotFoundException.
     * 
     * @param ex the ResourceNotFoundException
     * @param request the HTTP request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        
        log.warn("Resource not found: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.NOT_FOUND.value())
            .error(HttpStatus.NOT_FOUND.getReasonPhrase())
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .errorCode(ex.getErrorCode())
            .details(ex.getDetails())
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles DuplicateResourceException.
     * 
     * @param ex the DuplicateResourceException
     * @param request the HTTP request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {
        
        log.warn("Duplicate resource: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.CONFLICT.value())
            .error(HttpStatus.CONFLICT.getReasonPhrase())
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .errorCode(ex.getErrorCode())
            .details(ex.getDetails())
            .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles BusinessRuleViolationException.
     * 
     * @param ex the BusinessRuleViolationException
     * @param request the HTTP request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolationException(
            BusinessRuleViolationException ex, HttpServletRequest request) {
        
        log.warn("Business rule violation: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
            .error(HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase())
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .errorCode(ex.getErrorCode())
            .details(ex.getDetails())
            .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }

    /**
     * Handles ValidationException.
     * 
     * @param ex the ValidationException
     * @param request the HTTP request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, HttpServletRequest request) {
        
        log.warn("Validation error: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .errorCode(ex.getErrorCode())
            .details(ex.getDetails())
            .validationErrors(ex.getValidationErrors())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles AuthenticationException.
     * 
     * @param ex the AuthenticationException
     * @param request the HTTP request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        
        log.warn("Authentication failed: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.UNAUTHORIZED.value())
            .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .errorCode(ex.getErrorCode())
            .details(ex.getDetails())
            .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    // ========== Spring Framework Exceptions ==========

    /**
     * Handles MethodArgumentNotValidException (Bean Validation errors).
     * 
     * @param ex the MethodArgumentNotValidException
     * @param request the HTTP request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        log.warn("Validation failed for request: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        List<String> validationErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::formatFieldError)
            .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message("Validation failed")
            .path(request.getRequestURI())
            .errorCode("VALIDATION_ERROR")
            .validationErrors(validationErrors)
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles ConstraintViolationException (Bean Validation errors).
     * 
     * @param ex the ConstraintViolationException
     * @param request the HTTP request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        log.warn("Constraint violation: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        List<String> validationErrors = ex.getConstraintViolations()
            .stream()
            .map(this::formatConstraintViolation)
            .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message("Constraint validation failed")
            .path(request.getRequestURI())
            .errorCode("CONSTRAINT_VIOLATION")
            .validationErrors(validationErrors)
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles HttpMessageNotReadableException (JSON parsing errors).
     * 
     * @param ex the HttpMessageNotReadableException
     * @param request the HTTP request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
        log.warn("Invalid JSON format: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message("Invalid JSON format")
            .path(request.getRequestURI())
            .errorCode("INVALID_JSON")
            .details("Request body contains invalid JSON")
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles MethodArgumentTypeMismatchException (type conversion errors).
     * 
     * @param ex the MethodArgumentTypeMismatchException
     * @param request the HTTP request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        log.warn("Type mismatch for parameter '{}': {} - Path: {}", 
            ex.getName(), ex.getMessage(), request.getRequestURI());
        
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
            ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message(message)
            .path(request.getRequestURI())
            .errorCode("TYPE_MISMATCH")
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles MissingServletRequestParameterException.
     * 
     * @param ex the MissingServletRequestParameterException
     * @param request the HTTP request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
        log.warn("Missing required parameter '{}': {} - Path: {}", 
            ex.getParameterName(), ex.getMessage(), request.getRequestURI());
        
        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());

        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message(message)
            .path(request.getRequestURI())
            .errorCode("MISSING_PARAMETER")
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // ========== Generic Exception Handler ==========

    /**
     * Handles all other unhandled exceptions.
     * 
     * @param ex the exception
     * @param request the HTTP request
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        log.error("Unexpected error occurred: {} - Path: {}", ex.getMessage(), request.getRequestURI(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
            .message("An unexpected error occurred")
            .path(request.getRequestURI())
            .errorCode("INTERNAL_SERVER_ERROR")
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // ========== Helper Methods ==========

    /**
     * Formats a FieldError into a readable string.
     * 
     * @param fieldError the field error
     * @return formatted error message
     */
    private String formatFieldError(FieldError fieldError) {
        return String.format("Field '%s': %s", fieldError.getField(), fieldError.getDefaultMessage());
    }

    /**
     * Formats a ConstraintViolation into a readable string.
     * 
     * @param violation the constraint violation
     * @return formatted error message
     */
    private String formatConstraintViolation(ConstraintViolation<?> violation) {
        return String.format("Field '%s': %s", violation.getPropertyPath(), violation.getMessage());
    }
}
