package com.librarymanager.backend.exception;

/**
 * Exception thrown when authentication fails.
 * 
 * This exception is used when user authentication fails due to
 * invalid credentials or authentication-related issues.
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
public class AuthenticationException extends BaseLibraryException {

    private static final String ERROR_CODE = "AUTHENTICATION_FAILED";

    /**
     * Constructs a new AuthenticationException with the specified detail message.
     * 
     * @param message the detail message
     */
    public AuthenticationException(String message) {
        super(message, ERROR_CODE);
    }

    /**
     * Constructs a new AuthenticationException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }

    /**
     * Constructs a new AuthenticationException with the specified detail message and details.
     * 
     * @param message the detail message
     * @param details additional details about the error
     */
    public AuthenticationException(String message, String details) {
        super(message, ERROR_CODE, details);
    }

    /**
     * Constructs a new AuthenticationException with the specified detail message, details, and cause.
     * 
     * @param message the detail message
     * @param details additional details about the error
     * @param cause the cause of this exception
     */
    public AuthenticationException(String message, String details, Throwable cause) {
        super(message, ERROR_CODE, details, cause);
    }

    /**
     * Convenience method to create an AuthenticationException for invalid email.
     * 
     * @param email the email that failed authentication
     * @return AuthenticationException with formatted message
     */
    public static AuthenticationException invalidEmail(String email) {
        return new AuthenticationException("Invalid credentials for email: " + email);
    }

     /**
     * Convenience method to create an AuthenticationException for invalid password.
     * 
     * @param password the password that failed authentication
     * @return AuthenticationException with formatted message
     */
    public static AuthenticationException invalidPassword(String password) {
        return new AuthenticationException("Invalid credentials for password: " + password);
    }

    /**
     * Convenience method to create an AuthenticationException for user not found during authentication.
     * 
     * @param email the email that was not found
     * @return AuthenticationException with formatted message
     */
    public static AuthenticationException userNotFound(String email) {
        return new AuthenticationException("User not found for email: " + email);
    }
}
