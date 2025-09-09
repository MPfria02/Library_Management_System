package com.librarymanager.backend.repository;

import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BookGenre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    
    // Basic Spring Data JPA methods
    
    /**
     * Find book by ISBN (unique identifier)
     * @param isbn the book's ISBN
     * @return Optional containing book if found
     */
    Optional<Book> findByIsbn(String isbn);
    
    /**
     * Find available books (copies > 0)
     * @param copies minimum available copies (usually 0)
     * @return list of available books
     */
    List<Book> findByAvailableCopiesGreaterThan(Integer copies);
    
    /**
     * Find books by genre
     * @param genre the book genre
     * @return list of books in the specified genre
     */
    List<Book> findByGenre(BookGenre genre);
    
    // Advanced search queries
    
    /**
     * PostgreSQL full-text search using GIN indexes
     * This is production-ready search functionality
     * @param searchTerm the term to search for
     * @return list of books ranked by relevance
     */
    @Query(value = """
            SELECT * FROM books
            WHERE to_tsvector('english', title) @@ plainto_tsquery('english', :searchTerm)
               OR to_tsvector('english', author) @@ plainto_tsquery('english', :searchTerm)
            ORDER BY ts_rank(to_tsvector('english', title || ' ' || author), 
                            plainto_tsquery('english', :searchTerm)) DESC
            """, nativeQuery = true)
    List<Book> searchBooksFullText(@Param("searchTerm") String searchTerm);
    
    /**
     * Combined search with filters and pagination
     * This is the main search method used by the frontend
     * @param searchTerm optional search term for title/author
     * @param genre optional genre filter
     * @param availableOnly whether to show only available books
     * @param pageable pagination and sorting parameters
     * @return page of books matching criteria
     */
    @Query("""
            SELECT b FROM Book b
            WHERE (:searchTerm IS NULL OR 
                   LOWER(b.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
                   LOWER(b.author) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
              AND (:genre IS NULL OR b.genre = :genre)
              AND (:availableOnly = false OR b.availableCopies > 0)
            ORDER BY b.title
            """)
    Page<Book> findBooksWithFilters(
            @Param("searchTerm") String searchTerm,
            @Param("genre") BookGenre genre,
            @Param("availableOnly") boolean availableOnly,
            Pageable pageable
    );
    
    /**
     * Find books by partial title match (case-insensitive)
     * Simple search for autocomplete functionality
     * @param title partial title
     * @return list of books with matching titles
     */
    List<Book> findByTitleContainingIgnoreCase(String title);
    
    /**
     * Find books by author (case-insensitive)
     * @param author partial or full author name
     * @return list of books by the author
     */
    List<Book> findByAuthorContainingIgnoreCase(String author);
    
    /**
     * Count available books by genre
     * Useful for dashboard statistics
     * @param genre the genre to count
     * @return count of available books in the genre
     */
    @Query("SELECT COUNT(b) FROM Book b WHERE b.genre = :genre AND b.availableCopies > 0")
    long countAvailableBooksByGenre(@Param("genre") BookGenre genre);
}