package com.librarymanager.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized error response DTO for consistent API error format.
 * 
 * This class provides a consistent structure for all error responses
 * returned by the library management system API.
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime timestamp;
    
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final String errorCode;
    private final String details;
    private final List<String> validationErrors;

    /**
     * Private constructor to enforce use of builder pattern.
     */
    private ErrorResponse(Builder builder) {
        this.timestamp = builder.timestamp;
        this.status = builder.status;
        this.error = builder.error;
        this.message = builder.message;
        this.path = builder.path;
        this.errorCode = builder.errorCode;
        this.details = builder.details;
        this.validationErrors = builder.validationErrors;
    }

    /**
     * Returns the timestamp when the error occurred.
     * 
     * @return the timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the HTTP status code.
     * 
     * @return the status code
     */
    public int getStatus() {
        return status;
    }

    /**
     * Returns the HTTP error reason phrase.
     * 
     * @return the error reason phrase
     */
    public String getError() {
        return error;
    }

    /**
     * Returns the error message.
     * 
     * @return the error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the request path where the error occurred.
     * 
     * @return the request path
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the application-specific error code.
     * 
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Returns additional details about the error.
     * 
     * @return the details, or null if no details were provided
     */
    public String getDetails() {
        return details;
    }

    /**
     * Returns the list of validation errors.
     * 
     * @return the validation errors, or null if no validation errors were provided
     */
    public List<String> getValidationErrors() {
        return validationErrors;
    }

    /**
     * Builder class for creating ErrorResponse instances.
     */
    public static class Builder {
        private LocalDateTime timestamp = LocalDateTime.now();
        private int status;
        private String error;
        private String message;
        private String path;
        private String errorCode;
        private String details;
        private List<String> validationErrors;

        /**
         * Sets the timestamp.
         * 
         * @param timestamp the timestamp
         * @return this builder instance
         */
        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Sets the HTTP status code.
         * 
         * @param status the status code
         * @return this builder instance
         */
        public Builder status(int status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the HTTP error reason phrase.
         * 
         * @param error the error reason phrase
         * @return this builder instance
         */
        public Builder error(String error) {
            this.error = error;
            return this;
        }

        /**
         * Sets the error message.
         * 
         * @param message the error message
         * @return this builder instance
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the request path.
         * 
         * @param path the request path
         * @return this builder instance
         */
        public Builder path(String path) {
            this.path = path;
            return this;
        }

        /**
         * Sets the application-specific error code.
         * 
         * @param errorCode the error code
         * @return this builder instance
         */
        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        /**
         * Sets additional details about the error.
         * 
         * @param details the details
         * @return this builder instance
         */
        public Builder details(String details) {
            this.details = details;
            return this;
        }

        /**
         * Sets the list of validation errors.
         * 
         * @param validationErrors the validation errors
         * @return this builder instance
         */
        public Builder validationErrors(List<String> validationErrors) {
            this.validationErrors = validationErrors;
            return this;
        }

        /**
         * Builds the ErrorResponse instance.
         * 
         * @return the ErrorResponse instance
         */
        public ErrorResponse build() {
            return new ErrorResponse(this);
        }
    }

    /**
     * Creates a new builder instance.
     * 
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
