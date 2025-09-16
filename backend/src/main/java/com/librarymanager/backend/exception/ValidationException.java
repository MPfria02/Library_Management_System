package com.librarymanager.backend.exception;

import java.util.List;

/**
 * Exception thrown when input validation fails.
 * 
 * This exception is used when request data fails validation rules
 * and provides detailed information about validation errors.
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
public class ValidationException extends BaseLibraryException {

    private static final String ERROR_CODE = "VALIDATION_ERROR";
    private final List<String> validationErrors;

    /**
     * Constructs a new ValidationException with the specified detail message.
     * 
     * @param message the detail message
     */
    public ValidationException(String message) {
        super(message, ERROR_CODE);
        this.validationErrors = null;
    }

    /**
     * Constructs a new ValidationException with the specified detail message and validation errors.
     * 
     * @param message the detail message
     * @param validationErrors list of specific validation errors
     */
    public ValidationException(String message, List<String> validationErrors) {
        super(message, ERROR_CODE);
        this.validationErrors = validationErrors;
    }

    /**
     * Constructs a new ValidationException with the specified detail message, validation errors, and cause.
     * 
     * @param message the detail message
     * @param validationErrors list of specific validation errors
     * @param cause the cause of this exception
     */
    public ValidationException(String message, List<String> validationErrors, Throwable cause) {
        super(message, ERROR_CODE, cause);
        this.validationErrors = validationErrors;
    }

    /**
     * Returns the list of validation errors.
     * 
     * @return the validation errors, or null if no specific errors were provided
     */
    public List<String> getValidationErrors() {
        return validationErrors;
    }

    /**
     * Convenience method to create a ValidationException for a single validation error.
     * 
     * @param field the field that failed validation
     * @param error the validation error message
     * @return ValidationException with formatted message
     */
    public static ValidationException forField(String field, String error) {
        return new ValidationException("Validation failed for field '" + field + "': " + error);
    }

    /**
     * Convenience method to create a ValidationException for multiple validation errors.
     * 
     * @param validationErrors list of validation error messages
     * @return ValidationException with formatted message
     */
    public static ValidationException forMultipleErrors(List<String> validationErrors) {
        return new ValidationException("Multiple validation errors occurred", validationErrors);
    }

    /**
     * Convenience method to create a ValidationException for required field.
     * 
     * @param field the required field name
     * @return ValidationException with formatted message
     */
    public static ValidationException requiredField(String field) {
        return new ValidationException("Field '" + field + "' is required");
    }

    /**
     * Convenience method to create a ValidationException for invalid format.
     * 
     * @param field the field with invalid format
     * @param expectedFormat the expected format
     * @return ValidationException with formatted message
     */
    public static ValidationException invalidFormat(String field, String expectedFormat) {
        return new ValidationException("Field '" + field + "' has invalid format. Expected: " + expectedFormat);
    }
}
