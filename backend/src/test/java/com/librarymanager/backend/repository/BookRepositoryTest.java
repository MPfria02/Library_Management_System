package com.librarymanager.backend.repository;

import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BookGenre;
import com.librarymanager.backend.testutil.TestDataFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for BookRepository using @DataJpaTest.
 * 
 * Testing Strategy:
 * - Tests run against H2 in-memory database
 * - Real JPA queries and entity relationships
 * - Custom repository methods and JPQL queries
 * - Database constraints and validations
 * 
 * @author Marcel Pulido
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("BookRepository Integration Tests")
class BookRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookRepository bookRepository;

    private Book javaBook;
    private Book pythonBook;
    private Book fictionBook;
    private Book unavailableBook;

    @BeforeEach
    void setUp() {
        // Clear any existing data
        entityManager.clear();

        // Create test data using TestDataFactory
        javaBook = TestDataFactory.createDefaultTechBook();

        pythonBook = TestDataFactory.createCustomBook(
            "978-0596009258", "Learning Python", "Mark Lutz", "Comprehensive guide to Python programming", LocalDate.of(2013, 6, 12), BookGenre.TECHNOLOGY, 4, 4);

        fictionBook = TestDataFactory.createCustomBook(
            "978-0142000670", "Of Mice and Men", "John Steinbeck", "Classic American novel", LocalDate.of(1937, 4, 6), BookGenre.FICTION, 2, 2);
        
        unavailableBook = TestDataFactory.createCustomBook(
            "978-0321356680", "Clean Code", "Robert Martin", "Writing maintainable code", LocalDate.of(2008, 8, 1), BookGenre.TECHNOLOGY, 2, 0);
 
        // Persist test data
        entityManager.persist(javaBook);
        entityManager.persist(pythonBook);
        entityManager.persist(fictionBook);
        entityManager.persist(unavailableBook);
        entityManager.flush();
    }

    // ========== Basic Spring Data JPA Methods ==========

    @Test
    @DisplayName("Should find book by ISBN")
    void findByIsbn_ExistingISBN_ReturnsBook() {
        // When
        Optional<Book> result = bookRepository.findByIsbn("978-0134685991");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Effective Java");
        assertThat(result.get().getAuthor()).isEqualTo("Joshua Bloch");
    }

    @Test
    @DisplayName("Should return empty when ISBN not found")
    void findByIsbn_NonExistentISBN_ReturnsEmpty() {
        // When
        Optional<Book> result = bookRepository.findByIsbn("978-9999999999");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find available books")
    void findByAvailableCopiesGreaterThan_ReturnsAvailableBooks() {
        // When
        List<Book> result = bookRepository.findByAvailableCopiesGreaterThan(0);

        // Then
        assertThat(result).hasSize(3); // javaBook, pythonBook, fictionBook
        assertThat(result).extracting(Book::getTitle)
            .containsExactlyInAnyOrder("Effective Java", "Learning Python", "Of Mice and Men");
        assertThat(result).allMatch(book -> book.getAvailableCopies() > 0);
    }

    @Test
    @DisplayName("Should find books by genre")
    void findByGenre_ExistingGenre_ReturnsGenreBooks() {
        // When
        List<Book> result = bookRepository.findByGenre(BookGenre.TECHNOLOGY);

        // Then
        assertThat(result).hasSize(3); // javaBook, pythonBook, unavailableBook
        assertThat(result).extracting(Book::getGenre)
            .containsOnly(BookGenre.TECHNOLOGY);
    }

    // ========== Custom Query Methods ==========

    @Test
    @DisplayName("Should find books with filters and pagination")
    void findBooksWithFilters_ComplexQuery_ReturnsFilteredResults() {
        // Given
        String searchTerm = "Java";
        BookGenre genre = BookGenre.TECHNOLOGY;
        boolean availableOnly = true;
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findBooksWithFilters(searchTerm, genre, availableOnly, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Effective Java");
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getAvailableCopies()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle null filters in complex query")
    void findBooksWithFilters_NullFilters_ReturnsAllBooks() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findBooksWithFilters(null, null, false, pageable);

        // Then
        assertThat(result.getContent()).hasSize(4); // All books
        assertThat(result.getTotalElements()).isEqualTo(4);
    }

    @Test
    @DisplayName("Should filter available books only")
    void findBooksWithFilters_AvailableOnly_ExcludesUnavailable() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Book> result = bookRepository.findBooksWithFilters(null, null, true, pageable);

        // Then
        assertThat(result.getContent()).hasSize(3); // Excludes unavailableBook
        assertThat(result.getContent()).allMatch(book -> book.getAvailableCopies() > 0);
        assertThat(result.getContent()).extracting(Book::getTitle)
            .doesNotContain("Clean Code");
    }

    @Test
    @DisplayName("Should find books by title containing (case-insensitive)")
    void findByTitleContainingIgnoreCase_PartialMatch_ReturnsMatchingBooks() {
        // When
        List<Book> result = bookRepository.findByTitleContainingIgnoreCase("java");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Effective Java");
    }

    @Test
    @DisplayName("Should find books by author containing (case-insensitive)")
    void findByAuthorContainingIgnoreCase_PartialMatch_ReturnsMatchingBooks() {
        // When
        List<Book> result = bookRepository.findByAuthorContainingIgnoreCase("bloch");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAuthor()).isEqualTo("Joshua Bloch");
    }

    // ========== Statistical Queries ==========

    @Test
    @DisplayName("Should count available books by genre")
    void countAvailableBooksByGenre_ValidGenre_ReturnsCorrectCount() {
        // When
        long result = bookRepository.countAvailableBooksByGenre(BookGenre.TECHNOLOGY);

        // Then
        assertThat(result).isEqualTo(2L); // javaBook and pythonBook (not unavailableBook)
    }

    @Test
    @DisplayName("Should return zero for genre with no available books")
    void countAvailableBooksByGenre_NoAvailableBooks_ReturnsZero() {
        // Given - Create a genre with only unavailable books
        Book unavailableHistoryBook = TestDataFactory.createCustomBook(
            "978-1234567890", "History Book", "Historian", "Exploration of history", 
            LocalDate.of(2020, 1, 1), BookGenre.HISTORY, 1, 0);
        
        entityManager.persist(unavailableHistoryBook);
        entityManager.flush();

        // When
        long result = bookRepository.countAvailableBooksByGenre(BookGenre.HISTORY);

        // Then
        assertThat(result).isEqualTo(0L);
    }

    // ========== Entity Relationship and Constraint Tests ==========

    @Test
    @DisplayName("Should enforce ISBN uniqueness constraint")
    void save_DuplicateISBN_ThrowsException() {
        // Given
        // Same ISBN as javaBook
        Book duplicateBook = TestDataFactory.createCustomBook(
            "978-0134685991", "Another Java Book", "Another Author", "Another description",
            LocalDate.of(2017, 12, 27), BookGenre.TECHNOLOGY, 1, 1);

        // When & Then
        assertThatThrownBy(() -> {
            entityManager.persist(duplicateBook);
            entityManager.flush();
        }).isInstanceOf(Exception.class); // H2 will throw constraint violation
    }

    @Test
    @DisplayName("Should handle timestamp fields correctly")
    void save_NewBook_AutoGeneratesTimestamps() {
        // Given
        Book newBook = TestDataFactory.createCustomBook(
            "978-9876543210", "New Test Book", "Test Author", "Test description", LocalDate.of(2024, 1, 1), BookGenre.SCIENCE, 2, 2);

        // When
        Book savedBook = bookRepository.save(newBook);
        entityManager.flush();

        // Then
        assertThat(savedBook.getId()).isNotNull();
        assertThat(savedBook.getCreatedAt()).isNotNull();
        assertThat(savedBook.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update timestamps on modification")
    void save_UpdateExistingBook_UpdatesTimestamp() throws InterruptedException {
        // Given
        Book originalBook = bookRepository.save(TestDataFactory.createCustomBook(
            "978-1111111111", "Original Title", "Original Author", "Original description", LocalDate.of(2020, 1, 1), BookGenre.FICTION, 1, 1));
        entityManager.flush();
        
        // Small delay to ensure timestamp difference
        Thread.sleep(10);

        // When
        originalBook.setTitle("Updated Title");
        Book updatedBook = bookRepository.save(originalBook);
        entityManager.flush();

        // Then
        assertThat(updatedBook.getTitle()).isEqualTo("Updated Title");
        assertThat(updatedBook.getUpdatedAt()).isAfter(updatedBook.getCreatedAt());
    }

    // ========== Pagination Tests ==========

    @Test
    @DisplayName("Should handle pagination correctly")
    void findBooksWithFilters_Pagination_ReturnsCorrectPage() {
        // Given
        Pageable firstPage = PageRequest.of(0, 2); // First page, 2 items
        Pageable secondPage = PageRequest.of(1, 2); // Second page, 2 items

        // When
        Page<Book> firstResult = bookRepository.findBooksWithFilters(null, null, false, firstPage);
        Page<Book> secondResult = bookRepository.findBooksWithFilters(null, null, false, secondPage);

        // Then
        assertThat(firstResult.getContent()).hasSize(2);
        assertThat(secondResult.getContent()).hasSize(2);
        assertThat(firstResult.getTotalElements()).isEqualTo(4);
        assertThat(firstResult.getTotalPages()).isEqualTo(2);
        assertThat(firstResult.hasNext()).isTrue();
        assertThat(secondResult.hasNext()).isFalse();
    }
}