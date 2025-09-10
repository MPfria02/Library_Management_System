package com.librarymanager.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BookGenre;
import com.librarymanager.backend.repository.BookRepository;


/**
 * Service layer for Book statistics and reporting operations.
 * 
 * This service provides business logic for generating statistics about books including:
 * - Total book count
 * - Available book count
 * - Books with borrowed copies
 * - Available books by genre
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
@Service
@Transactional
public class BookStatisticsService {

    private static final Logger log = LoggerFactory.getLogger(BookStatisticsService.class);
    private final BookRepository bookRepository;

    public BookStatisticsService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * Counts total number of books in the system.
     * 
     * @return total book count
     */
    @Transactional(readOnly = true)
    public long countAllBooks() {
        log.debug("Counting all books");
        return bookRepository.count();
    }

    /**
     * Counts total number of available books.
     * 
     * @return available book count
     */
    @Transactional(readOnly = true)
    public long countAvailableBooks() {
        log.debug("Counting available books");
        return bookRepository.findByAvailableCopiesGreaterThan(0).size();
    }

    /**
     * Gets books that are currently borrowed (available < total).
     * 
     * @return list of books with borrowed copies
     */
    @Transactional(readOnly = true)
    public List<Book> getBooksWithBorrowedCopies() {
        log.debug("Finding books with borrowed copies");
        return bookRepository.findAll().stream()
            .filter(book -> book.getAvailableCopies() < book.getTotalCopies())
            .collect(Collectors.toList());
    }

    /**
     * Counts available books by genre.
     * 
     * @param genre the genre to filter by
     * @return count of available books in the specified genre
     */    
    @Transactional(readOnly = true) 
    public long countAvailableBooksByGenre(BookGenre genre) {
        log.debug("Counting available books by genre: {}", genre);
        return bookRepository.countAvailableBooksByGenre(genre);
    }

}
