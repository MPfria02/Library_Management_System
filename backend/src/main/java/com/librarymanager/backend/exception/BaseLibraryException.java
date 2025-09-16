package com.librarymanager.backend.exception;

import java.time.LocalDateTime;

/**
 * Base exception class for all library management system exceptions.
 * 
 * This abstract class provides common properties and behavior for all custom exceptions
 * in the library management system, following Spring Boot best practices for exception handling.
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
public abstract class BaseLibraryException extends RuntimeException {

    private final String errorCode;
    private final LocalDateTime timestamp;
    private final String details;

    /**
     * Constructs a new base library exception with the specified detail message.
     * 
     * @param message the detail message
     * @param errorCode the error code for this exception
     */
    protected BaseLibraryException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
        this.details = null;
    }

    /**
     * Constructs a new base library exception with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param errorCode the error code for this exception
     * @param cause the cause of this exception
     */
    protected BaseLibraryException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
        this.details = null;
    }

    /**
     * Constructs a new base library exception with the specified detail message, error code, and details.
     * 
     * @param message the detail message
     * @param errorCode the error code for this exception
     * @param details additional details about the error
     */
    protected BaseLibraryException(String message, String errorCode, String details) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
        this.details = details;
    }

    /**
     * Constructs a new base library exception with the specified detail message, error code, details, and cause.
     * 
     * @param message the detail message
     * @param errorCode the error code for this exception
     * @param details additional details about the error
     * @param cause the cause of this exception
     */
    protected BaseLibraryException(String message, String errorCode, String details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
        this.details = details;
    }

    /**
     * Returns the error code associated with this exception.
     * 
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the timestamp when this exception was created.
     * 
     * @return the timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Returns additional details about this exception.
     * 
     * @return the details, or null if no details were provided
     */
    public String getDetails() {
        return details;
    }

    /**
     * Returns a string representation of this exception including error code and timestamp.
     * 
     * @return string representation
     */
    @Override
    public String toString() {
        return String.format("%s [errorCode=%s, timestamp=%s, details=%s]", 
            getClass().getSimpleName(), errorCode, timestamp, details);
    }
}
