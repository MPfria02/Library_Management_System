package com.librarymanager.backend.service;

import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BookGenre;
import com.librarymanager.backend.exception.BusinessRuleViolationException;
import com.librarymanager.backend.exception.DuplicateResourceException;
import com.librarymanager.backend.exception.ResourceNotFoundException;
import com.librarymanager.backend.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for Book CRUD operations.
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
@Service
@Transactional
public class BookCatalogService {

    private static final Logger log = LoggerFactory.getLogger(BookCatalogService.class);
    
    private final BookRepository bookRepository;

    /**
     * Constructor injection for BookRepository.
     * 
     * @param bookRepository the book repository dependency
     */
    public BookCatalogService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // ========== CRUD Operations ==========

    /**
     * Creates a new book in the system.
     * 
     * @param book the book to create
     * @return the created book with generated ID
     * @throws DuplicateResourceException if book with same ISBN already exists
     * @throws BusinessRuleViolationException if business rules are violated
     */
    public Book createBook(Book book) {
        log.info("Creating new book with ISBN: {}", book.getIsbn());
        
        // Business rule: ISBN must be unique
        if (bookRepository.findByIsbn(book.getIsbn()).isPresent()) {
            log.warn("Attempted to create book with duplicate ISBN: {}", book.getIsbn());
            throw DuplicateResourceException.forBookIsbn(book.getIsbn());
        }
        
        // Business rule: Available copies cannot exceed total copies
        if (book.getTotalCopies() < 1) {
            log.warn("Total copies must be at least 1");
            throw BusinessRuleViolationException.minimumCopiesRequired();
        }
        
        Book savedBook = bookRepository.save(book);
        log.info("Book created successfully with ID: {} and title: '{}'", 
            savedBook.getId(), savedBook.getTitle());
        return savedBook;
    }

    /**
     * Retrieves a book by its ID.
     * 
     * @param id the book ID
     * @return Optional containing the book if found
     */
    @Transactional(readOnly = true)
    public Optional<Book> findById(Long id) {
        log.debug("Finding book by ID: {}", id);
        return bookRepository.findById(id);
    }

    /**
     * Retrieves a book by its ISBN.
     * 
     * @param isbn the book ISBN
     * @return Optional containing the book if found
     */
    @Transactional(readOnly = true)
    public Optional<Book> findByIsbn(String isbn) {
        log.debug("Finding book by ISBN: {}", isbn);
        return bookRepository.findByIsbn(isbn);
    }

    /**
     * Updates an existing book.
     * 
     * @param book the book with updated information
     * @return the updated book
     * @throws ResourceNotFoundException if book doesn't exist
     * @throws BusinessRuleViolationException if business rules are violated
     */
    public Book updateBook(Book book) {
        log.info("Updating book with ID: {}", book.getId());
        
        // Verify book exists
        Optional<Book> existingBook = bookRepository.findById(book.getId());
        if (existingBook.isEmpty()) {
            log.warn("Attempted to update non-existent book with ID: {}", book.getId());
            throw ResourceNotFoundException.forBook(book.getId());
        }
            
        // Business rule: Available copies validation
        if (book.getAvailableCopies() > book.getTotalCopies()) {
            throw BusinessRuleViolationException.invalidCopyCounts(book.getAvailableCopies(), book.getTotalCopies());
        }
        
        Book updatedBook = bookRepository.save(book);
        log.info("Book updated successfully: {}", updatedBook.getTitle());
        return updatedBook;
    }

    /**
     * Deletes a book by ID.
     * 
     * @param id the book ID to delete
     * @throws ResourceNotFoundException if book doesn't exist
     * @throws BusinessRuleViolationException if book has borrowed copies
     */
    public void deleteBook(Long id) {
        log.info("Attempting to delete book with ID: {}", id);
        
        Optional<Book> book = bookRepository.findById(id);
        if (book.isEmpty()) {
            log.warn("Attempted to delete non-existent book with ID: {}", id);
            throw ResourceNotFoundException.forBook(id);
        }
        
        // Business rule: Cannot delete book with borrowed copies
        Book bookEntity = book.get();
        if (bookEntity.getAvailableCopies() < bookEntity.getTotalCopies()) {
            log.warn("Cannot delete book with borrowed copies. Book: {}, Available: {}, Total: {}",
                bookEntity.getTitle(), bookEntity.getAvailableCopies(), bookEntity.getTotalCopies());
            throw BusinessRuleViolationException.cannotDeleteBookWithBorrowedCopies(bookEntity.getTitle());
        }
        
        bookRepository.deleteById(id);
        log.info("Book deleted successfully: {}", bookEntity.getTitle());
    }

    // ========== Search and Filtering Operations ==========

    /**
     * Searches books with advanced filtering and pagination.
     * 
     * @param searchTerm optional search term for title/author
     * @param genre optional genre filter
     * @param availableOnly if true, only return available books
     * @param pageable pagination and sorting information
     * @return paginated list of books matching criteria
     */
    @Transactional(readOnly = true)
    public Page<Book> searchBooks(String searchTerm, BookGenre genre, boolean availableOnly, Pageable pageable) {
        log.debug("Searching books - term: '{}', genre: {}, availableOnly: {}, page: {}", 
            searchTerm, genre, availableOnly, pageable.getPageNumber());
        
        return bookRepository.findBooksWithFilters(searchTerm, genre, availableOnly, pageable);
    }

    /**
     * Performs full-text search on books using PostgreSQL capabilities.
     * 
     * @param searchTerm the search term
     * @return list of books ordered by relevance
     */
    @Transactional(readOnly = true)
    public List<Book> fullTextSearch(String searchTerm) {
        log.debug("Performing full-text search with term: '{}'", searchTerm);
        return bookRepository.searchBooksFullText(searchTerm);
    }

    /**
     * Retrieves all books by genre.
     * 
     * @param genre the book genre
     * @return list of books in the specified genre
     */
    @Transactional(readOnly = true)
    public List<Book> findBooksByGenre(BookGenre genre) {
        log.debug("Finding books by genre: {}", genre);
        return bookRepository.findByGenre(genre);
    }

    /**
     * Retrieves all available books (with at least one available copy).
     * 
     * @return list of available books
     */
    @Transactional(readOnly = true)
    public List<Book> findAvailableBooks() {
        log.debug("Finding all available books");
        return bookRepository.findByAvailableCopiesGreaterThan(0);
    }

    /**
     * Searches books by title (case-insensitive, partial match).
     * 
     * @param title the title or part of the title to search for
     * @return list of books matching the title
     */
    @Transactional(readOnly = true)
    public List<Book> findBooksByTitle(String title) {
        log.debug("Finding books by title: {}", title);
        return bookRepository.findByTitleContainingIgnoreCase(title);
    }

    /**
     * Searches books by author (case-insensitive, partial match).
     * 
     * @param author the author or part of the author name to search for
     * @return list of books matching the author
     */
    @Transactional(readOnly = true)
    public List<Book> findBooksByAuthor(String author) {
        log.debug("Finding books by author: {}", author);
        return bookRepository.findByAuthorContainingIgnoreCase(author);
    }
}