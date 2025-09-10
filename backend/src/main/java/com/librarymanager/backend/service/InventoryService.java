package com.librarymanager.backend.service;

import com.librarymanager.backend.entity.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for Book inventory operations.
 * 
 * This service provides business logic for book inventory management including:
 * - Borrowing books
 * - Returning books
 * - Checking availability
 * 
 * @author Marcel Pulido
 * @version 1.0
 */

@Service
@Transactional
public class InventoryService {
    private final BookCatalogService bookCatalogService;
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    public InventoryService(BookCatalogService bookCatalogService) {
        this.bookCatalogService = bookCatalogService;
    }

     /* Handles book borrowing logic.
     * 
     * @param bookId the ID of the book to borrow
     * @return the updated book after borrowing
     * @throws IllegalArgumentException if book not found
     * @throws IllegalStateException if book not available
     */
    public Book borrowBook(Long bookId) {
        log.info("Processing book borrow request for book ID: {}", bookId);

        Book book = bookCatalogService.findById(bookId)
            .orElseThrow(() -> new IllegalArgumentException("Book with ID " + bookId + " not found"));
        
        try {
            // Use domain method from Book entity
            book.borrowCopy();
            Book updatedBook = bookCatalogService.updateBook(book);
            
            log.info("Book borrowed successfully: '{}' (ID: {}). Remaining copies: {}", 
                book.getTitle(), bookId, updatedBook.getAvailableCopies());
            
            return updatedBook;
            
        } catch (IllegalStateException e) {
            log.warn("Failed to borrow book '{}' (ID: {}): {}", book.getTitle(), bookId, e.getMessage());
            throw new IllegalStateException("Book '" + book.getTitle() + "' is not available for borrowing");
        }
    }

    /**
     * Handles book return logic.
     * 
     * @param bookId the ID of the book to return
     * @return the updated book after returning
     * @throws IllegalArgumentException if book not found
     * @throws IllegalStateException if cannot return more copies
     */
    public Book returnBook(Long bookId) {
        log.info("Processing book return request for book ID: {}", bookId);
        
        Book book = bookCatalogService.findById(bookId)
            .orElseThrow(() -> new IllegalArgumentException("Book with ID " + bookId + " not found"));
        
        try {
            // Use domain method from Book entity
            book.returnCopy();
            Book updatedBook = bookCatalogService.updateBook(book);

            log.info("Book returned successfully: '{}' (ID: {}). Available copies: {}", 
                book.getTitle(), bookId, updatedBook.getAvailableCopies());
            
            return updatedBook;
            
        } catch (IllegalStateException e) {
            log.warn("Failed to return book '{}' (ID: {}): {}", book.getTitle(), bookId, e.getMessage());
            throw new IllegalStateException("Cannot return book '" + book.getTitle() + "': " + e.getMessage());
        }
    }
}