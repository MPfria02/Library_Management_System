package com.librarymanager.backend.integration;

import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BookGenre;
import com.librarymanager.backend.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for BookStatisticsController using Testcontainers.
 * 
 * These tests verify end-to-end book statistics operations:
 * - Total book count retrieval
 * - Available book count retrieval  
 * - Books with borrowed copies retrieval
 * - Genre-specific available book counts
 * - Overall availability percentage calculation
 * - Edge cases with empty catalog and various book states
 * 
 * Tests use real PostgreSQL database via Testcontainers and MockMvc
 * for realistic integration testing without authentication requirements.
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
@AutoConfigureMockMvc
class BookStatisticsControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    private Long fullyAvailableBookId;
    private Long partiallyBorrowedBookId;
    private Long fullyBorrowedBookId;
    private Long anotherFictionBookId;
    private Long nonFictionBookId;

    /**
     * Set up test data before each test.
     * Creates books with different availability states and genres for comprehensive testing.
     */
    @BeforeEach
    void setUp() {
        // Clean up database
        bookRepository.deleteAll();
        
        createTestBooks();
    }

    private void createTestBooks() {
        // Book 1: Fully available (3/3 copies available) - FICTION
        Book fullyAvailableBook = Book.builder()
                .isbn("978-0-123456-78-9")
                .title("The Great Adventure")
                .author("John Smith")
                .description("An exciting adventure story")
                .genre(BookGenre.FICTION)
                .totalCopies(3)
                .availableCopies(3) // All copies available
                .publicationDate(LocalDate.of(2023, 1, 15))
                .build();
        fullyAvailableBookId = bookRepository.save(fullyAvailableBook).getId();

        // Book 2: Partially borrowed (2/5 copies available) - FICTION
        Book partiallyBorrowedBook = Book.builder()
                .isbn("978-0-234567-89-0")
                .title("Mystery of the Lost Key")
                .author("Jane Doe")
                .description("A thrilling mystery")
                .genre(BookGenre.FICTION)
                .totalCopies(5)
                .availableCopies(2) // 3 copies borrowed
                .publicationDate(LocalDate.of(2022, 6, 10))
                .build();
        partiallyBorrowedBookId = bookRepository.save(partiallyBorrowedBook).getId();

        // Book 3: Fully borrowed (0/2 copies available) - SCIENCE
        Book fullyBorrowedBook = Book.builder()
                .isbn("978-0-345678-90-1")
                .title("Advanced Physics")
                .author("Dr. Einstein")
                .description("Complex physics concepts")
                .genre(BookGenre.SCIENCE)
                .totalCopies(2)
                .availableCopies(0) // All copies borrowed
                .publicationDate(LocalDate.of(2021, 3, 20))
                .build();
        fullyBorrowedBookId = bookRepository.save(fullyBorrowedBook).getId();

        // Book 4: Another fully available book - FICTION
        Book anotherFictionBook = Book.builder()
                .isbn("978-0-456789-01-2")
                .title("Epic Fantasy Tale")
                .author("Fantasy Writer")
                .description("Dragons and magic")
                .genre(BookGenre.FICTION)
                .totalCopies(1)
                .availableCopies(1) // All copies available
                .publicationDate(LocalDate.of(2023, 8, 5))
                .build();
        anotherFictionBookId = bookRepository.save(anotherFictionBook).getId();

        // Book 5: Available non-fiction book - NON_FICTION
        Book nonFictionBook = Book.builder()
                .isbn("978-0-567890-12-3")
                .title("History of Ancient Rome")
                .author("Historian")
                .description("Complete history of Rome")
                .genre(BookGenre.NON_FICTION)
                .totalCopies(4)
                .availableCopies(4) // All copies available
                .publicationDate(LocalDate.of(2020, 12, 1))
                .build();
        nonFictionBookId = bookRepository.save(nonFictionBook).getId();
    }

    // ---------- Helper Methods ----------

    private ResultActions getTotalBookCount() throws Exception {
        return mockMvc.perform(get("/api/statistics/books/count")
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions getAvailableBookCount() throws Exception {
        return mockMvc.perform(get("/api/statistics/books/available/count")
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions getBooksWithBorrowedCopies() throws Exception {
        return mockMvc.perform(get("/api/statistics/books/borrowed")
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions getAvailableBooksCountByGenre(BookGenre genre) throws Exception {
        return mockMvc.perform(get("/api/statistics/books/genre/{genre}/count", genre)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions getAvailabilityPercentage() throws Exception {
        return mockMvc.perform(get("/api/statistics/books/availability/percentage")
                .contentType(MediaType.APPLICATION_JSON));
    }

    @Nested
    @DisplayName("Total Book Count Tests")
    class TotalBookCountTests {

        @Test
        @DisplayName("Should return correct total book count")
        void shouldReturnCorrectTotalBookCount() throws Exception {
            // When & Then
            getTotalBookCount()
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(content().string("5")); // 5 books total
        }

        @Test
        @DisplayName("Should return 0 when no books exist")
        void shouldReturn0WhenNoBooksExist() throws Exception {
            // Given - Clean database
            bookRepository.deleteAll();

            // When & Then
            getTotalBookCount()
                    .andExpect(status().isOk())
                    .andExpect(content().string("0"));
        }
    }

    @Nested
    @DisplayName("Available Book Count Tests")
    class AvailableBookCountTests {

        @Test
        @DisplayName("Should return correct available book count")
        void shouldReturnCorrectAvailableBookCount() throws Exception {
            // When & Then - 4 books have available copies (excludes fully borrowed book)
            getAvailableBookCount()
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(content().string("4"));
        }

        @Test
        @DisplayName("Should return 0 when all books are fully borrowed")
        void shouldReturn0WhenAllBooksAreFullyBorrowed() throws Exception {
            // Given - Make all books fully borrowed
            bookRepository.findAll().forEach(book -> {
                book.setAvailableCopies(0);
                bookRepository.save(book);
            });

            // When & Then
            getAvailableBookCount()
                    .andExpect(status().isOk())
                    .andExpect(content().string("0"));
        }

        @Test
        @DisplayName("Should return 0 when no books exist")
        void shouldReturn0WhenNoBooksExist() throws Exception {
            // Given - Clean database
            bookRepository.deleteAll();

            // When & Then
            getAvailableBookCount()
                    .andExpect(status().isOk())
                    .andExpect(content().string("0"));
        }
    }

    @Nested
    @DisplayName("Books With Borrowed Copies Tests")
    class BooksWithBorrowedCopiesTests {

        @Test
        @DisplayName("Should return books with borrowed copies")
        void shouldReturnBooksWithBorrowedCopies() throws Exception {
            // When & Then - Should return 2 books (partially and fully borrowed)
            getBooksWithBorrowedCopies()
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].id", hasItems(
                            partiallyBorrowedBookId.intValue(),
                            fullyBorrowedBookId.intValue())))
                    .andExpect(jsonPath("$[*].title", hasItems(
                            "Mystery of the Lost Key",
                            "Advanced Physics")));
        }

        @Test
        @DisplayName("Should return empty list when no books are borrowed")
        void shouldReturnEmptyListWhenNoBooksAreBorrowed() throws Exception {
            // Given - Make all books fully available
            bookRepository.findAll().forEach(book -> {
                book.setAvailableCopies(book.getTotalCopies());
                bookRepository.save(book);
            });

            // When & Then
            getBooksWithBorrowedCopies()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should return empty list when no books exist")
        void shouldReturnEmptyListWhenNoBooksExist() throws Exception {
            // Given - Clean database
            bookRepository.deleteAll();

            // When & Then
            getBooksWithBorrowedCopies()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("Available Books Count By Genre Tests")
    class AvailableBooksByGenreTests {

        @Test
        @DisplayName("Should return correct count for FICTION genre")
        void shouldReturnCorrectCountForFictionGenre() throws Exception {
            // When & Then - 2 fiction books have available copies
            getAvailableBooksCountByGenre(BookGenre.FICTION)
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(content().string("3"));
        }

        @Test
        @DisplayName("Should return correct count for NON_FICTION genre")
        void shouldReturnCorrectCountForNonFictionGenre() throws Exception {
            // When & Then - 1 non-fiction book has available copies
            getAvailableBooksCountByGenre(BookGenre.NON_FICTION)
                    .andExpect(status().isOk())
                    .andExpect(content().string("1"));
        }

        @Test
        @DisplayName("Should return correct count for SCIENCE genre")
        void shouldReturnCorrectCountForScienceGenre() throws Exception {
            // When & Then - 0 science books have available copies (fully borrowed)
            getAvailableBooksCountByGenre(BookGenre.SCIENCE)
                    .andExpect(status().isOk())
                    .andExpect(content().string("0"));
        }

        @Test
        @DisplayName("Should return 0 for genre with no books")
        void shouldReturnZeroForGenreWithNoBooks() throws Exception {
            // When & Then - No HISTORY books in test data
            getAvailableBooksCountByGenre(BookGenre.HISTORY)
                    .andExpect(status().isOk())
                    .andExpect(content().string("0"));
        }
    }

    @Nested
    @DisplayName("Availability Percentage Tests")
    class AvailabilityPercentageTests {

        @Test
        @DisplayName("Should return correct availability percentage")
        void shouldReturnCorrectAvailabilityPercentage() throws Exception {
            // When & Then - 4 available books out of 5 total = 80%
            getAvailabilityPercentage()
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(content().string("80.0"));
        }

        @Test
        @DisplayName("Should return 100% when all books are available")
        void shouldReturn100PercentWhenAllBooksAreAvailable() throws Exception {
            // Given - Make all books fully available
            bookRepository.findAll().forEach(book -> {
                book.setAvailableCopies(book.getTotalCopies());
                bookRepository.save(book);
            });

            // When & Then
            getAvailabilityPercentage()
                    .andExpect(status().isOk())
                    .andExpect(content().string("100.0"));
        }

        @Test
        @DisplayName("Should return 0% when no books are available")
        void shouldReturn0PercentWhenNoBooksAreAvailable() throws Exception {
            // Given - Make all books fully borrowed
            bookRepository.findAll().forEach(book -> {
                book.setAvailableCopies(0);
                bookRepository.save(book);
            });

            // When & Then
            getAvailabilityPercentage()
                    .andExpect(status().isOk())
                    .andExpect(content().string("0.0"));
        }

        @Test
        @DisplayName("Should return 0% when no books exist")
        void shouldReturn0PercentWhenNoBooksExist() throws Exception {
            // Given - Clean database
            bookRepository.deleteAll();

            // When & Then
            getAvailabilityPercentage()
                    .andExpect(status().isOk())
                    .andExpect(content().string("0.0"));
        }

        @Test
        @DisplayName("Should return correctly rounded percentage")
        void shouldReturnCorrectlyRoundedPercentage() throws Exception {
            // Given - Create scenario with 1 available out of 3 books = 33.33%
            bookRepository.deleteAll();
            
            Book book1 = Book.builder()
                    .isbn("978-1-111111-11-1")
                    .title("Book 1")
                    .author("Author 1")
                    .description("Description 1")
                    .genre(BookGenre.FICTION)
                    .totalCopies(1)
                    .availableCopies(1) // Available
                    .publicationDate(LocalDate.of(2023, 1, 1))
                    .build();
            bookRepository.save(book1);

            Book book2 = Book.builder()
                    .isbn("978-2-222222-22-2")
                    .title("Book 2")
                    .author("Author 2")
                    .description("Description 2")
                    .genre(BookGenre.FICTION)
                    .totalCopies(1)
                    .availableCopies(0) // Not available
                    .publicationDate(LocalDate.of(2023, 1, 2))
                    .build();
            bookRepository.save(book2);

            Book book3 = Book.builder()
                    .isbn("978-3-333333-33-3")
                    .title("Book 3")
                    .author("Author 3")
                    .description("Description 3")
                    .genre(BookGenre.FICTION)
                    .totalCopies(1)
                    .availableCopies(0) // Not available
                    .publicationDate(LocalDate.of(2023, 1, 3))
                    .build();
            bookRepository.save(book3);

            // When & Then - Should round 33.333... to 33.33
            getAvailabilityPercentage()
                    .andExpect(status().isOk())
                    .andExpect(content().string("33.33"));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle invalid genre gracefully")
        void shouldHandleInvalidGenreGracefully() throws Exception {
            // When & Then - Invalid genre should return 400 Bad Request
            mockMvc.perform(get("/api/statistics/books/genre/{genre}/count", "INVALID_GENRE")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }
}