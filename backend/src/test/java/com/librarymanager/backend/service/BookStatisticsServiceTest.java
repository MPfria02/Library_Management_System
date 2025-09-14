package com.librarymanager.backend.service;

import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BookGenre;
import com.librarymanager.backend.repository.BookRepository;
import com.librarymanager.backend.testutil.TestDataFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookStatisticsService focusing on statistics and reporting operations.
 * 
 * Key Testing Areas:
 * - Count operations for dashboard metrics
 * - Available book calculations
 * - Borrowed book identification
 * - Genre-based statistics
 * - Business intelligence queries
 * 
 * @author Marcel Pulido
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookStatisticsService Unit Tests")
class BookStatisticsServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookStatisticsService bookStatisticsService;

    private List<Book> sampleBooks;
    private List<Book> availableBooks;
    private Book fullyBorrowedBook;
    private Book partiallyBorrowedBook;
    private Book fullyAvailableBook;

    @BeforeEach
    void setUp() {
        // Fully borrowed book (0 available)
        fullyBorrowedBook = TestDataFactory.createCustomBook(
            "978-0134685991", 
            "Effective Java", 
            "Joshua Bloch",
            "Best practices for Java programming",
            LocalDate.of(2017, 12, 27),
            BookGenre.TECHNOLOGY,
            3, 
            0); // All borrowed
        fullyBorrowedBook.setId(1L);

        // Partially borrowed book (some available)
        partiallyBorrowedBook = TestDataFactory.createCustomBook(
            "978-0321356680",
            "Clean Code",
            "Robert Martin",
            "A Handbook of Agile Software Craftsmanship",
            LocalDate.of(2008, 8, 1),
            BookGenre.TECHNOLOGY,
            5,
            2);
        partiallyBorrowedBook.setId(2L);

        // Fully available book (none borrowed)
        fullyAvailableBook = TestDataFactory.createCustomBook(
            "978-0201633610",
            "Design Patterns",
            "Gang of Four",
            "Elements of Reusable Object-Oriented Software",
            LocalDate.of(1994, 10, 31),
            BookGenre.TECHNOLOGY,
            4,
            4);
        fullyAvailableBook.setId(3L);

        sampleBooks = Arrays.asList(fullyBorrowedBook, partiallyBorrowedBook, fullyAvailableBook);
        availableBooks = Arrays.asList(partiallyBorrowedBook, fullyAvailableBook);
    }

    // ========== Count All Books Tests ==========

    @Test
    @DisplayName("Should count all books in the system")
    void countAllBooks_ReturnsCorrectCount() {
        // Given
        when(bookRepository.count()).thenReturn(15L);

        // When
        long result = bookStatisticsService.countAllBooks();

        // Then
        assertThat(result).isEqualTo(15L);
        verify(bookRepository).count();
    }

    @Test
    @DisplayName("Should return zero when no books exist")
    void countAllBooks_EmptyLibrary_ReturnsZero() {
        // Given
        when(bookRepository.count()).thenReturn(0L);

        // When
        long result = bookStatisticsService.countAllBooks();

        // Then
        assertThat(result).isEqualTo(0L);
        verify(bookRepository).count();
    }

    // ========== Count Available Books Tests ==========

    @Test
    @DisplayName("Should count available books correctly")
    void countAvailableBooks_ReturnsCorrectCount() {
        // Given
        when(bookRepository.findByAvailableCopiesGreaterThan(0)).thenReturn(availableBooks);

        // When
        long result = bookStatisticsService.countAvailableBooks();

        // Then
        assertThat(result).isEqualTo(2L);
        verify(bookRepository).findByAvailableCopiesGreaterThan(0);
    }

    @Test
    @DisplayName("Should return zero when no books are available")
    void countAvailableBooks_AllBorrowed_ReturnsZero() {
        // Given
        when(bookRepository.findByAvailableCopiesGreaterThan(0)).thenReturn(Arrays.asList());

        // When
        long result = bookStatisticsService.countAvailableBooks();

        // Then
        assertThat(result).isEqualTo(0L);
        verify(bookRepository).findByAvailableCopiesGreaterThan(0);
    }

    // ========== Books with Borrowed Copies Tests ==========

    @Test
    @DisplayName("Should identify books with borrowed copies")
    void getBooksWithBorrowedCopies_ReturnsBooksWithBorrowedCopies() {
        // Given
        when(bookRepository.findAll()).thenReturn(sampleBooks);

        // When
        List<Book> result = bookStatisticsService.getBooksWithBorrowedCopies();

        // Then
        assertThat(result).hasSize(2); // fullyBorrowedBook and partiallyBorrowedBook
        assertThat(result).contains(fullyBorrowedBook, partiallyBorrowedBook);
        assertThat(result).doesNotContain(fullyAvailableBook);
        
        // Verify business logic: available < total
        assertThat(result).allMatch(book -> 
            book.getAvailableCopies() < book.getTotalCopies());
        
        verify(bookRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no books are borrowed")
    void getBooksWithBorrowedCopies_NoBorrowedBooks_ReturnsEmptyList() {
        // Given
        List<Book> allAvailableBooks = Arrays.asList(fullyAvailableBook);
        when(bookRepository.findAll()).thenReturn(allAvailableBooks);

        // When
        List<Book> result = bookStatisticsService.getBooksWithBorrowedCopies();

        // Then
        assertThat(result).isEmpty();
        verify(bookRepository).findAll();
    }

    @Test
    @DisplayName("Should handle books with zero total copies edge case")
    void getBooksWithBorrowedCopies_EdgeCases_HandlesCorrectly() {
        // Given
        Book edgeCaseBook = TestDataFactory.createCustomBook(
            "978-1234567890", "Edge Case Book", "Test Author", 
            "Test description", LocalDate.of(2024, 1, 1), BookGenre.FICTION, 1, 1); // Same as total, no borrowed copies

        List<Book> edgeCaseBooks = Arrays.asList(edgeCaseBook, partiallyBorrowedBook);
        when(bookRepository.findAll()).thenReturn(edgeCaseBooks);

        // When
        List<Book> result = bookStatisticsService.getBooksWithBorrowedCopies();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).contains(partiallyBorrowedBook);
        assertThat(result).doesNotContain(edgeCaseBook);
        verify(bookRepository).findAll();
    }

    // ========== Count Available Books by Genre Tests ==========

    @Test
    @DisplayName("Should count available books by genre")
    void countAvailableBooksByGenre_ValidGenre_ReturnsCorrectCount() {
        // Given
        BookGenre genre = BookGenre.TECHNOLOGY;
        when(bookRepository.countAvailableBooksByGenre(genre)).thenReturn(8L);

        // When
        long result = bookStatisticsService.countAvailableBooksByGenre(genre);

        // Then
        assertThat(result).isEqualTo(8L);
        verify(bookRepository).countAvailableBooksByGenre(genre);
    }

    @Test
    @DisplayName("Should return zero for genre with no available books")
    void countAvailableBooksByGenre_NoAvailableBooks_ReturnsZero() {
        // Given
        BookGenre genre = BookGenre.ROMANCE;
        when(bookRepository.countAvailableBooksByGenre(genre)).thenReturn(0L);

        // When
        long result = bookStatisticsService.countAvailableBooksByGenre(genre);

        // Then
        assertThat(result).isEqualTo(0L);
        verify(bookRepository).countAvailableBooksByGenre(genre);
    }

    @Test
    @DisplayName("Should handle all genre types")
    void countAvailableBooksByGenre_AllGenres_HandlesCorrectly() {
        // Given & When & Then - Test each genre
        for (BookGenre genre : BookGenre.values()) {
            when(bookRepository.countAvailableBooksByGenre(genre)).thenReturn(3L);
            
            long result = bookStatisticsService.countAvailableBooksByGenre(genre);
            
            assertThat(result).isEqualTo(3L);
            verify(bookRepository).countAvailableBooksByGenre(genre);
        }
    }

    // ========== Integration and Complex Scenarios Tests ==========

    @Test
    @DisplayName("Should handle mixed library scenario correctly")
    void mixedLibraryScenario_ValidatesComplexStatistics() {
        // Given - Complex library scenario
        Book fictionBook = TestDataFactory.createCustomBook(
            "978-0061120084", "To Kill a Mockingbird", "Harper Lee",
            "A novel about the serious issues of rape and racial inequality", LocalDate.of(1960, 7, 11),
            BookGenre.FICTION, 6, 4); // 2 borrowed

        Book scienceBook = TestDataFactory.createCustomBook(
            "978-0307389732", "A Brief History of Time", "Stephen Hawking",
            "An overview of cosmology from the Big Bang to black holes", LocalDate.of(1988, 4, 1),
            BookGenre.SCIENCE, 3, 0); // All borrowed
   
        List<Book> complexLibrary = Arrays.asList(
            fullyBorrowedBook, partiallyBorrowedBook, fullyAvailableBook, 
            fictionBook, scienceBook
        );

        when(bookRepository.count()).thenReturn(5L);
        when(bookRepository.findByAvailableCopiesGreaterThan(0))
            .thenReturn(Arrays.asList(partiallyBorrowedBook, fullyAvailableBook, fictionBook));
        when(bookRepository.findAll()).thenReturn(complexLibrary);
        when(bookRepository.countAvailableBooksByGenre(BookGenre.TECHNOLOGY)).thenReturn(2L);
        when(bookRepository.countAvailableBooksByGenre(BookGenre.FICTION)).thenReturn(1L);

        // When
        long totalBooks = bookStatisticsService.countAllBooks();
        long availableBooks = bookStatisticsService.countAvailableBooks();
        List<Book> borrowedBooks = bookStatisticsService.getBooksWithBorrowedCopies();
        long techBooks = bookStatisticsService.countAvailableBooksByGenre(BookGenre.TECHNOLOGY);
        long fictionBooks = bookStatisticsService.countAvailableBooksByGenre(BookGenre.FICTION);

        // Then
        assertThat(totalBooks).isEqualTo(5L);
        assertThat(availableBooks).isEqualTo(3L);
        assertThat(borrowedBooks).hasSize(4); // All except fullyAvailableBook
        assertThat(techBooks).isEqualTo(2L);
        assertThat(fictionBooks).isEqualTo(1L);
        
        // Verify all interactions
        verify(bookRepository).count();
        verify(bookRepository).findByAvailableCopiesGreaterThan(0);
        verify(bookRepository).findAll();
        verify(bookRepository).countAvailableBooksByGenre(BookGenre.TECHNOLOGY);
        verify(bookRepository).countAvailableBooksByGenre(BookGenre.FICTION);
    }
}