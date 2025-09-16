package com.librarymanager.backend.controller;

import com.librarymanager.backend.dto.response.BookResponse;
import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BookGenre;
import com.librarymanager.backend.mapper.BookMapper;
import com.librarymanager.backend.service.BookStatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for book statistics and analytics operations.
 * 
 * Provides simple statistics and reporting functionality for the book catalog.
 * Designed for dashboard analytics and basic reporting features.
 * 
 * Endpoints:
 * - GET /api/statistics/books/count - Total book count
 * - GET /api/statistics/books/available/count - Available book count
 * - GET /api/statistics/books/borrowed - Books with borrowed copies
 * - GET /api/statistics/books/genre/{genre}/count - Available books count by genre
 * - GET /api/statistics/books/availability/percentage - Overall availability percentage
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
@RestController
@RequestMapping("/api/statistics/books")
public class BookStatisticsController {

    private static final Logger log = LoggerFactory.getLogger(BookStatisticsController.class);
    
    private final BookStatisticsService bookStatisticsService;
    private final BookMapper bookMapper;

    /**
     * Constructor injection for dependencies.
     * 
     * @param bookStatisticsService the book statistics service
     * @param bookMapper the book mapper for DTO conversion
     */
    public BookStatisticsController(BookStatisticsService bookStatisticsService, BookMapper bookMapper) {
        this.bookStatisticsService = bookStatisticsService;
        this.bookMapper = bookMapper;
    }


    /**
     * Retrieves the total count of books in the system.
     * 
     * @return ResponseEntity with total book count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getTotalBookCount() {
        log.debug("Retrieving total book count");
        
        try {
            long totalBooks = bookStatisticsService.countAllBooks();
            log.debug("Total books count: {}", totalBooks);
            return ResponseEntity.ok(totalBooks);
            
        } catch (Exception e) {
            log.error("Error retrieving total book count", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retrieves the count of available books (with at least one available copy).
     * 
     * @return ResponseEntity with available book count
     */
    @GetMapping("/available/count")
    public ResponseEntity<Long> getAvailableBookCount() {
        log.debug("Retrieving available book count");
        
        try {
            long availableBooks = bookStatisticsService.countAvailableBooks();
            log.debug("Available books count: {}", availableBooks);
            return ResponseEntity.ok(availableBooks);
            
        } catch (Exception e) {
            log.error("Error retrieving available book count", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retrieves all books that currently have borrowed copies.
     * 
     * @return ResponseEntity with list of books that have borrowed copies
     */
    @GetMapping("/borrowed")
    public ResponseEntity<List<BookResponse>> getBooksWithBorrowedCopies() {
        log.debug("Retrieving books with borrowed copies");
        
        try {
            List<Book> booksWithBorrowedCopies = bookStatisticsService.getBooksWithBorrowedCopies();
            List<BookResponse> response = booksWithBorrowedCopies.stream()
                .map(bookMapper::toResponse)
                .collect(Collectors.toList());
            
            log.debug("Found {} books with borrowed copies", response.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving books with borrowed copies", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retrieves the count of available books for a specific genre.
     * 
     * @param genre the book genre to get statistics for
     * @return ResponseEntity with available book count for the genre
     */
    @GetMapping("/genre/{genre}/count")
    public ResponseEntity<Long> getAvailableBooksCountByGenre(@PathVariable BookGenre genre) {
        log.debug("Retrieving available books count for genre: {}", genre);
        
        try {
            long count = bookStatisticsService.countAvailableBooksByGenre(genre);
            log.debug("Available books count for genre {}: {}", genre, count);
            return ResponseEntity.ok(count);
            
        } catch (Exception e) {
            log.error("Error retrieving available books count for genre: {}", genre, e);
            return ResponseEntity.internalServerError().build();
        }
    }


    /**
     * Retrieves availability percentage for the entire book catalog.
     * 
     * @return ResponseEntity with availability percentage
     */
    @GetMapping("/availability/percentage")
    public ResponseEntity<Double> getAvailabilityPercentage() {
        log.debug("Retrieving book availability percentage");
        
        try {
            long totalBooks = bookStatisticsService.countAllBooks();
            long availableBooks = bookStatisticsService.countAvailableBooks();
            
            double availabilityPercentage = totalBooks > 0 ? 
                (double) availableBooks / totalBooks * 100 : 0.0;
            
            // Round to 2 decimal places
            double roundedPercentage = Math.round(availabilityPercentage * 100.0) / 100.0;
            
            log.debug("Book availability percentage: {}%", roundedPercentage);
            return ResponseEntity.ok(roundedPercentage);
            
        } catch (Exception e) {
            log.error("Error retrieving availability percentage", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
