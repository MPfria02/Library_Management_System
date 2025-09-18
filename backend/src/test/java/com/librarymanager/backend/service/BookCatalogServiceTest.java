package com.librarymanager.backend.service;

import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BookGenre;
import com.librarymanager.backend.exception.BusinessRuleViolationException;
import com.librarymanager.backend.exception.DuplicateResourceException;
import com.librarymanager.backend.exception.ResourceNotFoundException;
import com.librarymanager.backend.repository.BookRepository;
import com.librarymanager.backend.testutil.TestDataFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookCatalogService focusing on business logic validation.
 * 
 * Key Testing Areas:
 * - CRUD operations with business rules
 * - Complex search functionality
 * - Validation logic for book creation/updates
 * - Edge cases and error handling
 * 
 * @author Marcel Pulido
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookCatalogService Unit Tests")
class BookCatalogServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookCatalogService bookCatalogService;

    private Book validBook;
    private Book bookWithBorrowedCopies;

    @BeforeEach
    void setUp() {
        validBook = TestDataFactory.createCustomBook(
            "978-0134685991",
            "Effective Java",
            "Joshua Bloch",
            "A comprehensive guide to best practices in Java programming",
            LocalDate.of(2018, 1, 6),
            BookGenre.TECHNOLOGY,
            5,
            5);

        bookWithBorrowedCopies = TestDataFactory.createCustomBook(
            "978-0321356680",
            "Effective C++",
            "Scott Meyers",
            "55 Specific Ways to Improve Your Programs and Designs",
            LocalDate.of(2005, 5, 22),
            BookGenre.TECHNOLOGY,
            3,
            1);
    }

    // ========== Create Book Tests ==========

    @Test
    @DisplayName("Should create book when valid data provided")
    void createBook_ValidBook_ReturnsCreatedBook() {
        // Given
        when(bookRepository.findByIsbn(validBook.getIsbn())).thenReturn(Optional.empty());
        when(bookRepository.save(validBook)).thenReturn(validBook);

        // When
        Book result = bookCatalogService.createBook(validBook);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Effective Java");
        verify(bookRepository).findByIsbn(validBook.getIsbn());
        verify(bookRepository).save(validBook);
    }

    @Test
    @DisplayName("Should throw exception when ISBN already exists")
    void createBook_DuplicateISBN_ThrowsException() {
        // Given
        when(bookRepository.findByIsbn(validBook.getIsbn())).thenReturn(Optional.of(validBook));

        // When & Then
        assertThatThrownBy(() -> bookCatalogService.createBook(validBook))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Book with ISBN 978-0134685991 already exists");

        verify(bookRepository).findByIsbn(validBook.getIsbn());
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when total copies less than 1")
    void createBook_InvalidCopyCount_ThrowsException() {
        // Given
        Book invalidBook = TestDataFactory.createCustomBook(
            "978-1234567890", "Invalid Book", "Test Author",
            "Test description", LocalDate.of(2024, 1, 1), BookGenre.FICTION, 0, 0);

        when(bookRepository.findByIsbn(invalidBook.getIsbn())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookCatalogService.createBook(invalidBook))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("Total copies must be at least 1");

        verify(bookRepository, never()).save(any());
    }

    // ========== Update Book Tests ==========

    @Test
    @DisplayName("Should update book when valid changes provided")
    void updateBook_ValidUpdate_ReturnsUpdatedBook() {
        // Given
        validBook.setId(1L);
        validBook.setTitle("Effective Java - Third Edition");
        
        when(bookRepository.findById(1L)).thenReturn(Optional.of(validBook));
        when(bookRepository.save(validBook)).thenReturn(validBook);

        // When
        Book result = bookCatalogService.updateBook(validBook);

        // Then
        assertThat(result.getTitle()).isEqualTo("Effective Java - Third Edition");
        verify(bookRepository).findById(1L);
        verify(bookRepository).save(validBook);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent book")
    void updateBook_NonExistentBook_ThrowsException() {
        // Given
        validBook.setId(999L);
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookCatalogService.updateBook(validBook))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Book with ID 999 not found");

        verify(bookRepository).findById(999L);
        verify(bookRepository, never()).save(any());
    }

    // ========== Delete Book Tests ==========

    @Test
    @DisplayName("Should delete book when no copies are borrowed")
    void deleteBook_NoBorrowedCopies_DeletesSuccessfully() {
        // Given
        Long bookId = 1L;
        validBook.setId(bookId);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(validBook));

        // When
        bookCatalogService.deleteBook(bookId);

        // Then
        verify(bookRepository).findById(bookId);
        verify(bookRepository).deleteById(bookId);
    }

    @Test
    @DisplayName("Should throw exception when deleting book with borrowed copies")
    void deleteBook_WithBorrowedCopies_ThrowsException() {
        // Given
        Long bookId = 1L;
        bookWithBorrowedCopies.setId(bookId);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(bookWithBorrowedCopies));

        // When & Then
        assertThatThrownBy(() -> bookCatalogService.deleteBook(bookId))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("Cannot delete book '" + bookWithBorrowedCopies.getTitle() + "'");

        verify(bookRepository).findById(bookId);
        verify(bookRepository, never()).deleteById(any());
    }

    // ========== Search Tests ==========

    @Test
    @DisplayName("Should search books with filters and pagination")
    void searchBooks_WithFilters_ReturnsPagedResults() {
        // Given
        String searchTerm = "Java";
        BookGenre genre = BookGenre.TECHNOLOGY;
        boolean availableOnly = true;
        Pageable pageable = PageRequest.of(0, 10);
        
        List<Book> books = Arrays.asList(validBook);
        Page<Book> bookPage = new PageImpl<>(books, pageable, 1);
        
        when(bookRepository.findBooksWithFilters(searchTerm, genre, availableOnly, pageable))
            .thenReturn(bookPage);

        // When
        Page<Book> result = bookCatalogService.searchBooks(searchTerm, genre, availableOnly, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Effective Java");
        verify(bookRepository).findBooksWithFilters(searchTerm, genre, availableOnly, pageable);
    }

    @Test
    @DisplayName("Should perform full-text search")
    void fullTextSearch_ValidTerm_ReturnsOrderedResults() {
        // Given
        String searchTerm = "programming";
        List<Book> searchResults = Arrays.asList(validBook);
        when(bookRepository.searchBooksFullText(searchTerm)).thenReturn(searchResults);

        // When
        List<Book> result = bookCatalogService.fullTextSearch(searchTerm);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Effective Java");
        verify(bookRepository).searchBooksFullText(searchTerm);
    }

    // ========== Find Operations Tests ==========

    @Test
    @DisplayName("Should find books by genre")
    void findBooksByGenre_ValidGenre_ReturnsFilteredBooks() {
        // Given
        BookGenre genre = BookGenre.TECHNOLOGY;
        List<Book> techBooks = Arrays.asList(validBook, bookWithBorrowedCopies);
        when(bookRepository.findByGenre(genre)).thenReturn(techBooks);

        // When
        List<Book> result = bookCatalogService.findBooksByGenre(genre);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(book -> book.getGenre() == BookGenre.TECHNOLOGY);
        verify(bookRepository).findByGenre(genre);
    }

    @Test
    @DisplayName("Should find available books only")
    void findAvailableBooks_ReturnsOnlyAvailable() {
        // Given
        List<Book> availableBooks = Arrays.asList(validBook); // Only books with available copies
        when(bookRepository.findByAvailableCopiesGreaterThan(0)).thenReturn(availableBooks);

        // When
        List<Book> result = bookCatalogService.findAvailableBooks();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAvailableCopies()).isGreaterThan(0);
        verify(bookRepository).findByAvailableCopiesGreaterThan(0);
    }

    @Test
    @DisplayName("Should find books by title containing search term")
    void findBooksByTitle_PartialMatch_ReturnsMatchingBooks() {
        // Given
        String titlePart = "Effective";
        List<Book> matchingBooks = Arrays.asList(validBook, bookWithBorrowedCopies);
        when(bookRepository.findByTitleContainingIgnoreCase(titlePart)).thenReturn(matchingBooks);

        // When
        List<Book> result = bookCatalogService.findBooksByTitle(titlePart);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(book -> 
            book.getTitle().toLowerCase().contains(titlePart.toLowerCase()));
        verify(bookRepository).findByTitleContainingIgnoreCase(titlePart);
    }
}