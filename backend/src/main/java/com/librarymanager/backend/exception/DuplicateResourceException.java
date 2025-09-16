package com.librarymanager.backend.exception;

/**
 * Exception thrown when attempting to create a resource that already exists.
 * 
 * This exception is used when trying to create a resource (user, book, etc.)
 * with unique constraints that already exist in the system.
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
public class DuplicateResourceException extends BaseLibraryException {

    private static final String ERROR_CODE = "DUPLICATE_RESOURCE";

    /**
     * Constructs a new DuplicateResourceException with the specified detail message.
     * 
     * @param message the detail message
     */
    public DuplicateResourceException(String message) {
        super(message, ERROR_CODE);
    }

    /**
     * Constructs a new DuplicateResourceException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public DuplicateResourceException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }

    /**
     * Constructs a new DuplicateResourceException with the specified detail message and details.
     * 
     * @param message the detail message
     * @param details additional details about the error
     */
    public DuplicateResourceException(String message, String details) {
        super(message, ERROR_CODE, details);
    }

    /**
     * Constructs a new DuplicateResourceException with the specified detail message, details, and cause.
     * 
     * @param message the detail message
     * @param details additional details about the error
     * @param cause the cause of this exception
     */
    public DuplicateResourceException(String message, String details, Throwable cause) {
        super(message, ERROR_CODE, details, cause);
    }

    /**
     * Convenience method to create a DuplicateResourceException for a user email.
     * 
     * @param email the email that already exists
     * @return DuplicateResourceException with formatted message
     */
    public static DuplicateResourceException forUserEmail(String email) {
        return new DuplicateResourceException("User with email " + email + " already exists");
    }

    /**
     * Convenience method to create a DuplicateResourceException for a book ISBN.
     * 
     * @param isbn the ISBN that already exists
     * @return DuplicateResourceException with formatted message
     */
    public static DuplicateResourceException forBookIsbn(String isbn) {
        return new DuplicateResourceException("Book with ISBN " + isbn + " already exists");
    }
}
