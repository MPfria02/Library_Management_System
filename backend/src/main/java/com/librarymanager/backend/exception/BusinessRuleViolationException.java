package com.librarymanager.backend.exception;

/**
 * Exception thrown when a business rule is violated.
 * 
 * This exception is used when an operation violates a business rule
 * or constraint in the library management system.
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
public class BusinessRuleViolationException extends BaseLibraryException {

    private static final String ERROR_CODE = "BUSINESS_RULE_VIOLATION";

    /**
     * Constructs a new BusinessRuleViolationException with the specified detail message.
     * 
     * @param message the detail message
     */
    public BusinessRuleViolationException(String message) {
        super(message, ERROR_CODE);
    }

    /**
     * Constructs a new BusinessRuleViolationException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public BusinessRuleViolationException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }

    /**
     * Constructs a new BusinessRuleViolationException with the specified detail message and details.
     * 
     * @param message the detail message
     * @param details additional details about the error
     */
    public BusinessRuleViolationException(String message, String details) {
        super(message, ERROR_CODE, details);
    }

    /**
     * Constructs a new BusinessRuleViolationException with the specified detail message, details, and cause.
     * 
     * @param message the detail message
     * @param details additional details about the error
     * @param cause the cause of this exception
     */
    public BusinessRuleViolationException(String message, String details, Throwable cause) {
        super(message, ERROR_CODE, details, cause);
    }

    /**
     * Convenience method to create a BusinessRuleViolationException for book availability.
     * 
     * @param bookTitle the title of the book that is not available
     * @return BusinessRuleViolationException with formatted message
     */
    public static BusinessRuleViolationException bookNotAvailable(String bookTitle) {
        return new BusinessRuleViolationException("Book '" + bookTitle + "' is not available for borrowing");
    }

    /**
     * Convenience method to create a BusinessRuleViolationException for book deletion with borrowed copies.
     * 
     * @param bookTitle the title of the book that cannot be deleted
     * @return BusinessRuleViolationException with formatted message
     */
    public static BusinessRuleViolationException cannotDeleteBookWithBorrowedCopies(String bookTitle) {
        return new BusinessRuleViolationException(
            "Cannot delete book '" + bookTitle + "' with borrowed copies. Please ensure all copies are returned first.");
    }

    /**
     * Convenience method to create a BusinessRuleViolationException for invalid copy counts.
     * 
     * @param availableCopies the available copies count
     * @param totalCopies the total copies count
     * @return BusinessRuleViolationException with formatted message
     */
    public static BusinessRuleViolationException invalidCopyCounts(int availableCopies, int totalCopies) {
        return new BusinessRuleViolationException(
            "Available copies (" + availableCopies + ") cannot exceed total copies (" + totalCopies + ")");
    }

    /**
     * Convenience method to create a BusinessRuleViolationException for minimum copy requirement.
     * 
     * @return BusinessRuleViolationException with formatted message
     */
    public static BusinessRuleViolationException minimumCopiesRequired() {
        return new BusinessRuleViolationException("Total copies must be at least 1");
    }

    /**
     * Convenience method to create a BusinessRuleViolationException for book return when all copies are available.
     * 
     * @param bookTitle the title of the book
     * @return BusinessRuleViolationException with formatted message
     */
    public static BusinessRuleViolationException cannotReturnAllCopiesAvailable(String bookTitle) {
        return new BusinessRuleViolationException(
            "Cannot return book '" + bookTitle + "': all copies are already available");
    }

    /**
     * Convenience method to create a BusinessRuleViolationException for attempting to reduce total copies below borrowed copies.
     * 
     * @param borrowedCopies the number of currently borrowed copies
     * @param newTotalCopies the new total copies being set
     * @return BusinessRuleViolationException with formatted message
     */
    public static BusinessRuleViolationException cannotReduceCopiesBelowBorrowed(int borrowedCopies, int newTotalCopies) {
        return new BusinessRuleViolationException(
            "Cannot set total copies to " + newTotalCopies + " when " + borrowedCopies + " copies are currently borrowed");
    }
}
