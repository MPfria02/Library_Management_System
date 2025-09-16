package com.librarymanager.backend.controller;

import com.librarymanager.backend.dto.request.BookCreationRequest;
import com.librarymanager.backend.dto.response.BookResponse;
import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BookGenre;
import com.librarymanager.backend.mapper.BookMapper;
import com.librarymanager.backend.service.BookCatalogService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for book catalog operations.
 * 
 * Provides comprehensive CRUD operations and advanced search functionality for books.
 * Follows RESTful conventions with proper HTTP status codes and error handling.
 * 
 * Endpoints:
 * - GET /api/books - List books with pagination and filtering
 * - GET /api/books/{id} - Get book by ID
 * - GET /api/books/isbn/{isbn} - Get book by ISBN
 * - POST /api/books - Create new book
 * - PUT /api/books/{id} - Update existing book
 * - DELETE /api/books/{id} - Delete book
 * - GET /api/books/search - Advanced search with filters
 * - GET /api/books/search/full-text - Full-text search
 * - GET /api/books/genre/{genre} - Get books by genre
 * - GET /api/books/available - Get available books
 * - GET /api/books/title/{title} - Search by title
 * - GET /api/books/author/{author} - Search by author
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
@RestController
@RequestMapping("/api/books")
public class BookCatalogController {

    private static final Logger log = LoggerFactory.getLogger(BookCatalogController.class);
    
    private final BookCatalogService bookCatalogService;
    private final BookMapper bookMapper;

    /**
     * Constructor injection for dependencies.
     * 
     * @param bookCatalogService the book catalog service
     * @param bookMapper the book mapper for DTO conversion
     */
    public BookCatalogController(BookCatalogService bookCatalogService, BookMapper bookMapper) {
        this.bookCatalogService = bookCatalogService;
        this.bookMapper = bookMapper;
    }

    // ========== CRUD Operations ==========

    /**
     * Creates a new book in the system.
     * 
     * @param request the book creation request with validation
     * @return ResponseEntity with created book or error status
     */
    @PostMapping
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody BookCreationRequest request) {
        log.info("Creating new book with ISBN: {}", request.getIsbn());
        
        try {
            Book book = bookMapper.toEntity(request);
            Book createdBook = bookCatalogService.createBook(book);
            BookResponse response = bookMapper.toResponse(createdBook);
            
            log.info("Book created successfully with ID: {} and title: '{}'", 
                createdBook.getId(), createdBook.getTitle());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create book: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Retrieves a book by its ID.
     * 
     * @param id the book ID
     * @return ResponseEntity with book data or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable Long id) {
        log.debug("Retrieving book by ID: {}", id);
        
        Optional<Book> bookOpt = bookCatalogService.findById(id);
        return bookOpt
            .map(bookMapper::toResponse)
            .map(ResponseEntity::ok)
            .orElseGet(() -> {
                log.debug("Book not found with ID: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            });
    }

    /**
     * Retrieves a book by its ISBN.
     * 
     * @param isbn the book ISBN
     * @return ResponseEntity with book data or 404 if not found
     */
    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BookResponse> getBookByIsbn(@PathVariable String isbn) {
        log.debug("Retrieving book by ISBN: {}", isbn);
        
        Optional<Book> bookOpt = bookCatalogService.findByIsbn(isbn);
        return bookOpt
            .map(bookMapper::toResponse)
            .map(ResponseEntity::ok)
            .orElseGet(() -> {
                log.debug("Book not found with ISBN: {}", isbn);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            });
    }

    /**
     * Updates an existing book.
     * 
     * @param id the book ID
     * @param request the updated book data
     * @return ResponseEntity with updated book or error status
     */
    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> updateBook(@PathVariable Long id, 
                                                  @Valid @RequestBody BookCreationRequest request) {
        log.info("Updating book with ID: {}", id);
        
        try {
            // Create book entity from request and set the ID
            Book book = bookMapper.toEntity(request);
            book.setId(id);
            
            Book updatedBook = bookCatalogService.updateBook(book);
            BookResponse response = bookMapper.toResponse(updatedBook);
            
            log.info("Book updated successfully: {}", updatedBook.getTitle());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update book with ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Deletes a book by ID.
     * 
     * @param id the book ID to delete
     * @return ResponseEntity with appropriate status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        log.info("Deleting book with ID: {}", id);
        
        try {
            bookCatalogService.deleteBook(id);
            log.info("Book deleted successfully with ID: {}", id);
            return ResponseEntity.noContent().build();
            
        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete book with ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            
        } catch (IllegalStateException e) {
            log.warn("Cannot delete book with ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    // ========== Search and Filtering Operations ==========

    /**
     * Retrieves books with pagination and optional filtering.
     * 
     * @param page page number (0-based, default: 0)
     * @param size page size (default: 20)
     * @param sortBy field to sort by (default: title)
     * @param sortDir sort direction (asc/desc, default: asc)
     * @param searchTerm optional search term for title/author
     * @param genre optional genre filter
     * @param availableOnly if true, only return available books
     * @return ResponseEntity with paginated book list
     */
    @GetMapping
    public ResponseEntity<Page<BookResponse>> getBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) BookGenre genre,
            @RequestParam(defaultValue = "false") boolean availableOnly) {
        
        log.debug("Retrieving books - page: {}, size: {}, sortBy: {}, sortDir: {}, " +
                 "searchTerm: '{}', genre: {}, availableOnly: {}", 
                 page, size, sortBy, sortDir, searchTerm, genre, availableOnly);
        
        // Create sort object
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Search books with filters
        Page<Book> books = bookCatalogService.searchBooks(searchTerm, genre, availableOnly, pageable);
        
        // Convert to response DTOs
        Page<BookResponse> response = books.map(bookMapper::toResponse);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Performs advanced search with multiple filters and pagination.
     * 
     * @param searchTerm optional search term for title/author
     * @param genre optional genre filter
     * @param availableOnly if true, only return available books
     * @param page page number (0-based, default: 0)
     * @param size page size (default: 20)
     * @param sortBy field to sort by (default: title)
     * @param sortDir sort direction (asc/desc, default: asc)
     * @return ResponseEntity with paginated search results
     */
    @GetMapping("/search")
    public ResponseEntity<Page<BookResponse>> searchBooks(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) BookGenre genre,
            @RequestParam(defaultValue = "false") boolean availableOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        log.debug("Advanced search - term: '{}', genre: {}, availableOnly: {}, " +
                 "page: {}, size: {}, sortBy: {}, sortDir: {}", 
                 searchTerm, genre, availableOnly, page, size, sortBy, sortDir);
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Book> books = bookCatalogService.searchBooks(searchTerm, genre, availableOnly, pageable);
        Page<BookResponse> response = books.map(bookMapper::toResponse);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Performs full-text search using PostgreSQL capabilities.
     * 
     * @param searchTerm the search term for full-text search
     * @return ResponseEntity with list of books ordered by relevance
     */
    @GetMapping("/search/full-text")
    public ResponseEntity<List<BookResponse>> fullTextSearch(@RequestParam String searchTerm) {
        log.debug("Full-text search with term: '{}'", searchTerm);
        
        List<Book> books = bookCatalogService.fullTextSearch(searchTerm);
        List<BookResponse> response = books.stream()
            .map(bookMapper::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all books by genre.
     * 
     * @param genre the book genre
     * @return ResponseEntity with list of books in the specified genre
     */
    @GetMapping("/genre/{genre}")
    public ResponseEntity<List<BookResponse>> getBooksByGenre(@PathVariable BookGenre genre) {
        log.debug("Retrieving books by genre: {}", genre);
        
        List<Book> books = bookCatalogService.findBooksByGenre(genre);
        List<BookResponse> response = books.stream()
            .map(bookMapper::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all available books (with at least one available copy).
     * 
     * @return ResponseEntity with list of available books
     */
    @GetMapping("/available")
    public ResponseEntity<List<BookResponse>> getAvailableBooks() {
        log.debug("Retrieving all available books");
        
        List<Book> books = bookCatalogService.findAvailableBooks();
        List<BookResponse> response = books.stream()
            .map(bookMapper::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Searches books by title (case-insensitive, partial match).
     * 
     * @param title the title or part of the title to search for
     * @return ResponseEntity with list of books matching the title
     */
    @GetMapping("/title/{title}")
    public ResponseEntity<List<BookResponse>> getBooksByTitle(@PathVariable String title) {
        log.debug("Searching books by title: {}", title);
        
        List<Book> books = bookCatalogService.findBooksByTitle(title);
        List<BookResponse> response = books.stream()
            .map(bookMapper::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Searches books by author (case-insensitive, partial match).
     * 
     * @param author the author or part of the author name to search for
     * @return ResponseEntity with list of books matching the author
     */
    @GetMapping("/author/{author}")
    public ResponseEntity<List<BookResponse>> getBooksByAuthor(@PathVariable String author) {
        log.debug("Searching books by author: {}", author);
        
        List<Book> books = bookCatalogService.findBooksByAuthor(author);
        List<BookResponse> response = books.stream()
            .map(bookMapper::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all available book genres.
     * 
     * @return ResponseEntity with list of available genres
     */
    @GetMapping("/genres")
    public ResponseEntity<BookGenre[]> getAvailableGenres() {
        log.debug("Retrieving available book genres");
        return ResponseEntity.ok(BookGenre.values());
    }
}
