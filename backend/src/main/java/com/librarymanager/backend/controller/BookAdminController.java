package com.librarymanager.backend.controller;

import com.librarymanager.backend.dto.request.BookCreationRequest;
import com.librarymanager.backend.dto.response.BookAdminResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin REST controller for book management with full field visibility.
 * <p>
 * Endpoints (all require ADMIN role):
 * <ul>
 * <li>POST /api/admin/books</li>
 * <li>GET /api/admin/books</li>
 * <li>GET /api/admin/books/{id}</li>
 * <li>PUT /api/admin/books/{id}</li>
 * <li>DELETE /api/admin/books/{id}</li>
 * </ul>
 * Returns full book data, including isbn and totalCopies.
 *
 * @author Marcel Pulido
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin/books")
public class BookAdminController {

    private static final Logger log = LoggerFactory.getLogger(BookAdminController.class);

    private final BookCatalogService bookCatalogService;
    private final BookMapper bookMapper;

    public BookAdminController(BookCatalogService bookCatalogService, BookMapper bookMapper) {
        this.bookCatalogService = bookCatalogService;
        this.bookMapper = bookMapper;
    }

    /**
     * Creates a new book (admin).
     * @param request validated book creation request
     * @return created BookAdminResponse
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookAdminResponse> createBook(@Valid @RequestBody BookCreationRequest request) {
        log.info("[ADMIN] Creating new book with ISBN: {}", request.getIsbn());
        Book book = bookMapper.toEntity(request);
        Book createdBook = bookCatalogService.createBook(book);
        BookAdminResponse response = bookMapper.toAdminResponse(createdBook);
        log.info("[ADMIN] Book created with ID: {} and title: '{}'", createdBook.getId(), createdBook.getTitle());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Gets paginated list of all books (admin, default page size 30).
     * @param page page number (default 0)
     * @param size page size (default 30)
     * @param sortBy sort field (default title)
     * @param sortDir direction (default asc)
     * @param searchTerm filter for title/author
     * @param genre filter genre
     * @param availableOnly filter for books in stock
     * @return paginated admin book data
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<BookAdminResponse>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) BookGenre genre,
            @RequestParam(defaultValue = "false") boolean availableOnly) {
        log.debug("[ADMIN] Retrieving books - page: {}, size: {}, sortBy: {}, sortDir: {}, searchTerm: '{}', genre: {}, availableOnly: {}", page, size, sortBy, sortDir, searchTerm, genre, availableOnly);
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Book> books = bookCatalogService.searchBooks(searchTerm, genre, availableOnly, pageable);
        Page<BookAdminResponse> response = books.map(bookMapper::toAdminResponse);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets full book data by ID (admin).
     * @param id book id
     * @return BookAdminResponse if found
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookAdminResponse> getBookById(@PathVariable Long id) {
        log.debug("[ADMIN] Retrieving book by ID: {}", id);
        Book book = bookCatalogService.findById(id);
        BookAdminResponse response = bookMapper.toAdminResponse(book);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates existing book (admin).
     * @param id book id
     * @param request new book data
     * @return updated BookAdminResponse
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookAdminResponse> updateBook(@PathVariable Long id, @Valid @RequestBody BookCreationRequest request) {
        log.info("[ADMIN] Updating book with ID: {}", id);
        Book book = bookMapper.toEntity(request);
        book.setId(id);
        Book updatedBook = bookCatalogService.updateBook(book);
        BookAdminResponse response = bookMapper.toAdminResponse(updatedBook);
        log.info("[ADMIN] Book updated: {}", updatedBook.getTitle());
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a book by ID (admin).
     * @param id book id
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        log.info("[ADMIN] Deleting book with ID: {}", id);
        bookCatalogService.deleteBook(id);
        log.info("[ADMIN] Book deleted with ID: {}", id);
        return ResponseEntity.noContent().build();
    }
}
