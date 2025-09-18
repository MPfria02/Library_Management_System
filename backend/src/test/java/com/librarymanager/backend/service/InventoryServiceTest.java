package com.librarymanager.backend.service;

import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BookGenre;
import com.librarymanager.backend.testutil.TestDataFactory;
import com.librarymanager.backend.exception.BusinessRuleViolationException;
import com.librarymanager.backend.exception.ResourceNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InventoryService focusing on book borrowing/returning domain logic.
 * 
 * Key Testing Focus:
 * - Domain-driven design validation (Book.borrowCopy(), Book.returnCopy())
 * - Service orchestration between InventoryService and BookCatalogService
 * - Business rule enforcement and error handling
 * - State changes and side effects verification
 * 
 * @author Marcel Pulido
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService Unit Tests")
class InventoryServiceTest {

    @Mock
    private BookCatalogService bookCatalogService;

    @InjectMocks
    private InventoryService inventoryService;

    private Book availableBook;
    private Book unavailableBook;
    private Book singleCopyBook;

    @BeforeEach
    void setUp() {
        availableBook = TestDataFactory.createCustomBook(
            "978-0134685991",
            "Effective Java",
            "Joshua Bloch",
            "Best practices for Java programming",
            LocalDate.of(2017, 12, 27),
            BookGenre.TECHNOLOGY,
            5,
            3);
        availableBook.setId(1L);

        unavailableBook = TestDataFactory.createCustomBook(
            "978-0321356680",
            "Clean Code",
            "Robert Martin",
            "A Handbook of Agile Software Craftsmanship",
            LocalDate.of(2008, 8, 1),
            BookGenre.TECHNOLOGY,
            2,
            0);    
        unavailableBook.setId(2L);

        singleCopyBook = TestDataFactory.createCustomBook(
            "978-0201633610",
            "Design Patterns",
            "Gang of Four",
            "Elements of Reusable Object-Oriented Software",
            LocalDate.of(1994, 10, 31),
            BookGenre.TECHNOLOGY,
            1,
            1);
        singleCopyBook.setId(3L);
    }

    // ========== borrowBook Tests ==========

    @Test
    @DisplayName("Should successfully borrow book when available")
    void borrowBook_BookAvailable_UpdatesInventoryAndReturnsBook() {
        // Given
        Long bookId = 1L;
        Book expectedUpdatedBook = TestDataFactory.createCustomBook(
            availableBook.getIsbn(), availableBook.getTitle(), availableBook.getAuthor(), availableBook.getDescription(), availableBook.getPublicationDate(),
            availableBook.getGenre(), 5, 2); // Decremented by 1
        expectedUpdatedBook.setId(1L);

        when(bookCatalogService.findById(bookId)).thenReturn(availableBook);
        when(bookCatalogService.updateBook(any(Book.class))).thenReturn(expectedUpdatedBook);

        // When
        Book result = inventoryService.borrowBook(bookId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAvailableCopies()).isEqualTo(2);
        assertThat(result.getTotalCopies()).isEqualTo(5);
        
        verify(bookCatalogService).findById(bookId);
        verify(bookCatalogService).updateBook(any(Book.class));
    }

    @Test
    @DisplayName("Should successfully borrow last available copy")
    void borrowBook_LastCopy_UpdatesInventoryToZero() {
        // Given
        Long bookId = 3L;
        Book expectedUpdatedBook = TestDataFactory.createCustomBook(
            singleCopyBook.getIsbn(), singleCopyBook.getTitle(), singleCopyBook.getAuthor(), singleCopyBook.getDescription(), singleCopyBook.getPublicationDate(),
            singleCopyBook.getGenre(),
            1, 0); // Last copy borrowed
        expectedUpdatedBook.setId(3L); 

        when(bookCatalogService.findById(bookId)).thenReturn(singleCopyBook);
        when(bookCatalogService.updateBook(any(Book.class))).thenReturn(expectedUpdatedBook);

        // When
        Book result = inventoryService.borrowBook(bookId);

        // Then
        assertThat(result.getAvailableCopies()).isEqualTo(0);
        assertThat(result.getTotalCopies()).isEqualTo(1);
        
        verify(bookCatalogService).findById(bookId);
        verify(bookCatalogService).updateBook(any(Book.class));
    }

    @Test
    @DisplayName("Should throw exception when book not found")
    void borrowBook_BookNotFound_ThrowsException() {
        // Given
        Long bookId = 999L;
        when(bookCatalogService.findById(bookId)).thenThrow(ResourceNotFoundException.forBook(bookId));

        // When & Then
        assertThatThrownBy(() -> inventoryService.borrowBook(bookId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Book with ID 999 not found");

        verify(bookCatalogService).findById(bookId);
        verify(bookCatalogService, never()).updateBook(any());
    }

    @Test
    @DisplayName("Should throw exception when book not available")
    void borrowBook_BookNotAvailable_ThrowsException() {
        // Given
        Long bookId = 2L;
        when(bookCatalogService.findById(bookId)).thenReturn(unavailableBook);

        // When & Then
        assertThatThrownBy(() -> inventoryService.borrowBook(bookId))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("Book 'Clean Code' is not available for borrowing");

        verify(bookCatalogService).findById(bookId);
        verify(bookCatalogService, never()).updateBook(any());
    }

    // ========== returnBook Tests ==========

    @Test
    @DisplayName("Should successfully return book when valid")
    void returnBook_ValidReturn_UpdatesInventoryAndReturnsBook() {
        // Given
        Long bookId = 1L;
        // Simulate a book with some borrowed copies
        Book bookWithBorrowedCopies = TestDataFactory.createCustomBook(availableBook.getIsbn(), availableBook.getTitle(), availableBook.getAuthor(),
            availableBook.getDescription(), availableBook.getPublicationDate(), availableBook.getGenre(), 5, 2); // 3 copies borrowed
        bookWithBorrowedCopies.setId(1L);

        Book expectedUpdatedBook =TestDataFactory.createCustomBook(availableBook.getIsbn(), availableBook.getTitle(), availableBook.getAuthor(),
            availableBook.getDescription(), availableBook.getPublicationDate(), availableBook.getGenre(), 5, 3); // Incremented by 1
        expectedUpdatedBook.setId(1L);

        when(bookCatalogService.findById(bookId)).thenReturn(bookWithBorrowedCopies);
        when(bookCatalogService.updateBook(any(Book.class))).thenReturn(expectedUpdatedBook);

        // When
        Book result = inventoryService.returnBook(bookId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAvailableCopies()).isEqualTo(3);
        assertThat(result.getTotalCopies()).isEqualTo(5);
        
        verify(bookCatalogService).findById(bookId);
        verify(bookCatalogService).updateBook(any(Book.class));
    }

    @Test
    @DisplayName("Should throw exception when book not found for return")
    void returnBook_BookNotFound_ThrowsException() {
        // Given
        Long bookId = 999L;
        when(bookCatalogService.findById(bookId)).thenThrow(ResourceNotFoundException.forBook(bookId));

        // When & Then
        assertThatThrownBy(() -> inventoryService.returnBook(bookId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Book with ID 999 not found");

        verify(bookCatalogService).findById(bookId);
        verify(bookCatalogService, never()).updateBook(any());
    }

    @Test
    @DisplayName("Should throw exception when trying to return more copies than total")
    void returnBook_ExceedsTotalCopies_ThrowsException() {
        // Given
        Long bookId = 1L;
        // Book with all copies already available (none borrowed)
        Book allCopiesAvailable = TestDataFactory.createCustomBook(
            availableBook.getIsbn(), availableBook.getTitle(), availableBook.getAuthor(),
            availableBook.getDescription(), availableBook.getPublicationDate(), availableBook.getGenre(), 5, 5); // All copies available, none borrowed
        allCopiesAvailable.setId(1L);

        when(bookCatalogService.findById(bookId)).thenReturn(allCopiesAvailable);

        // When & Then
        assertThatThrownBy(() -> inventoryService.returnBook(bookId))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("all copies are already available");

        verify(bookCatalogService).findById(bookId);
        verify(bookCatalogService, never()).updateBook(any());
    }

    // ========== Edge Cases and Boundary Tests ==========

    @Test
    @DisplayName("Should handle multiple consecutive borrows correctly")
    void borrowBook_MultipleBorrows_TracksInventoryCorrectly() {
        // Given - Simulate borrowing multiple times
        Long bookId = 1L;
        
        // First borrow: 3 -> 2 available
        Book afterFirstBorrow = TestDataFactory.createCustomBook(availableBook.getIsbn(), availableBook.getTitle(), availableBook.getAuthor(), 
            availableBook.getDescription(), availableBook.getPublicationDate(), availableBook.getGenre(), 5, 2);  // First borrow: 3 -> 2 available

        afterFirstBorrow.setId(1L);

        when(bookCatalogService.findById(bookId)).thenReturn(availableBook);
        when(bookCatalogService.updateBook(any(Book.class))).thenReturn(afterFirstBorrow);

        // When
        Book result = inventoryService.borrowBook(bookId);

        // Then
        assertThat(result.getAvailableCopies()).isEqualTo(2);
        verify(bookCatalogService).findById(bookId);
        verify(bookCatalogService).updateBook(any(Book.class));
    }

    @Test
    @DisplayName("Should handle return after multiple borrows")
    void returnBook_AfterMultipleBorrows_RestoresInventoryCorrectly() {
        // Given - Book with 2 available out of 5 total (3 borrowed)
        Long bookId = 1L;
        Book heavilyBorrowedBook = TestDataFactory.createCustomBook(
            availableBook.getIsbn(), availableBook.getTitle(), availableBook.getAuthor(), 
            availableBook.getDescription(), availableBook.getPublicationDate(), availableBook.getGenre(), 5, 2); // 3 copies borrowed

        heavilyBorrowedBook.setId(1L);

        Book afterReturn = TestDataFactory.createCustomBook(
            availableBook.getIsbn(), availableBook.getTitle(), availableBook.getAuthor(),
            availableBook.getDescription(), availableBook.getPublicationDate(), availableBook.getGenre(), 5, 3); // Incremented by 1
        afterReturn.setId(1L);

        when(bookCatalogService.findById(bookId)).thenReturn(heavilyBorrowedBook);
        when(bookCatalogService.updateBook(any(Book.class))).thenReturn(afterReturn);

        // When
        Book result = inventoryService.returnBook(bookId);

        // Then
        assertThat(result.getAvailableCopies()).isEqualTo(3);
        assertThat(result.getTotalCopies()).isEqualTo(5);
        verify(bookCatalogService).findById(bookId);
        verify(bookCatalogService).updateBook(any(Book.class));
    }
}