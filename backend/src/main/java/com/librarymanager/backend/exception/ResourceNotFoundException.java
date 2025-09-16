package com.librarymanager.backend.exception;

/**
 * Exception thrown when a requested resource is not found.
 * 
 * This exception is used when attempting to access a resource (user, book, etc.)
 * that doesn't exist in the system.
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
public class ResourceNotFoundException extends BaseLibraryException {

    private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message.
     * 
     * @param message the detail message
     */
    public ResourceNotFoundException(String message) {
        super(message, ERROR_CODE);
    }

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message and details.
     * 
     * @param message the detail message
     * @param details additional details about the error
     */
    public ResourceNotFoundException(String message, String details) {
        super(message, ERROR_CODE, details);
    }

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message, details, and cause.
     * 
     * @param message the detail message
     * @param details additional details about the error
     * @param cause the cause of this exception
     */
    public ResourceNotFoundException(String message, String details, Throwable cause) {
        super(message, ERROR_CODE, details, cause);
    }

    /**
     * Convenience method to create a ResourceNotFoundException for a user.
     * 
     * @param userId the user ID that was not found
     * @return ResourceNotFoundException with formatted message
     */
    public static ResourceNotFoundException forUser(Long userId) {
        return new ResourceNotFoundException("User with ID " + userId + " not found");
    }

    /**
     * Convenience method to create a ResourceNotFoundException for a user by email.
     * 
     * @param email the email that was not found
     * @return ResourceNotFoundException with formatted message
     */
    public static ResourceNotFoundException forUserByEmail(String email) {
        return new ResourceNotFoundException("User with email " + email + " not found");
    }

    /**
     * Convenience method to create a ResourceNotFoundException for a book.
     * 
     * @param bookId the book ID that was not found
     * @return ResourceNotFoundException with formatted message
     */
    public static ResourceNotFoundException forBook(Long bookId) {
        return new ResourceNotFoundException("Book with ID " + bookId + " not found");
    }

    /**
     * Convenience method to create a ResourceNotFoundException for a book by ISBN.
     * 
     * @param isbn the ISBN that was not found
     * @return ResourceNotFoundException with formatted message
     */
    public static ResourceNotFoundException forBookByIsbn(String isbn) {
        return new ResourceNotFoundException("Book with ISBN " + isbn + " not found");
    }
}
