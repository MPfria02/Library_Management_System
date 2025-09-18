package com.librarymanager.backend.controller;

import com.librarymanager.backend.dto.response.BookResponse;
import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.mapper.BookMapper;
import com.librarymanager.backend.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * REST controller for book inventory operations.
 * 
 * Provides simple inventory management functionality for borrowing and returning books.
 * Designed for circulation management and book availability tracking.
 * 
 * Endpoints:
 * - POST /api/inventory/books/{id}/borrow - Borrow a book
 * - POST /api/inventory/books/{id}/return - Return a book
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
@RestController
@RequestMapping("/api/inventory/books")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);
    
    private final InventoryService inventoryService;
    private final BookMapper bookMapper;

    /**
     * Constructor injection for dependencies.
     * 
     * @param inventoryService the inventory service
     * @param bookMapper the book mapper for DTO conversion
     */
    public InventoryController(InventoryService inventoryService, BookMapper bookMapper) {
        this.inventoryService = inventoryService;
        this.bookMapper = bookMapper;
    }

    /**
     * Borrows a book by its ID.
     * 
     * @param id the book ID to borrow
     * @return ResponseEntity with updated book information or error status
     */
    @PostMapping("/{id}/borrow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookResponse> borrowBook(@PathVariable Long id) {
        log.info("Processing borrow request for book ID: {}", id);
        
        Book borrowedBook = inventoryService.borrowBook(id);
        BookResponse response = bookMapper.toResponse(borrowedBook);
        
        log.info("Book borrowed successfully: '{}' (ID: {}). Available copies: {}", 
            borrowedBook.getTitle(), id, borrowedBook.getAvailableCopies());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Returns a book by its ID.
     * 
     * @param id the book ID to return
     * @return ResponseEntity with updated book information or error status
     */
    @PostMapping("/{id}/return")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookResponse> returnBook(@PathVariable Long id) {
        log.info("Processing return request for book ID: {}", id);
        
        Book returnedBook = inventoryService.returnBook(id);
        BookResponse response = bookMapper.toResponse(returnedBook);
        
        log.info("Book returned successfully: '{}' (ID: {}). Available copies: {}", 
            returnedBook.getTitle(), id, returnedBook.getAvailableCopies());
        
        return ResponseEntity.ok(response);
    }
}
