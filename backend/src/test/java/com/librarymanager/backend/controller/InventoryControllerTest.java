package com.librarymanager.backend.controller;

import com.librarymanager.backend.dto.response.BookResponse;
import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BookGenre;
import com.librarymanager.backend.mapper.BookMapper;
import com.librarymanager.backend.service.InventoryService;
import com.librarymanager.backend.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InventoryController.
 * 
 * Tests inventory management endpoints including book borrowing and returning functionality.
 * Follows Spring Boot testing best practices with proper mocking and assertions.
 * 
 * @author Marcel Pulido
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryController Unit Tests")
class InventoryControllerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private InventoryController inventoryController;

    private Book testBook;
    private BookResponse testBookResponse;
    private Book borrowedBook;
    private Book returnedBook;

    @BeforeEach
    void setUp() {
        // Setup test data for borrowing
        testBook = TestDataFactory.createBookForBorrowing();
        testBook.setId(1L);

        testBookResponse = BookResponse.builder()
            .id(1L)
            .title("Borrowing Test Book")
            .author("Test Author")
            .description("Test description")
            .genre(BookGenre.TECHNOLOGY)
            .availableCopies(8)
            .publicationDate(LocalDate.of(2020, 1, 1))
            .build();

        // Setup test data for after borrowing
        borrowedBook = TestDataFactory.createBookForBorrowing();
        borrowedBook.setId(1L);
        borrowedBook.setAvailableCopies(7); // One copy borrowed

        // Setup test data for returning
        returnedBook = TestDataFactory.createBookForReturning();
        returnedBook.setId(2L);
        returnedBook.setAvailableCopies(3); // One copy returned
    }

    // ========== Borrow Book Tests ==========

    @Test
    @DisplayName("borrowBook_shouldReturnUpdatedBookResponse_whenValidBookIdProvided")
    void borrowBook_shouldReturnUpdatedBookResponse_whenValidBookIdProvided() {
        // Given
        Long bookId = 1L;
        BookResponse borrowedBookResponse = BookResponse.builder()
            .id(1L)
            .title("Borrowing Test Book")
            .author("Test Author")
            .description("Test description")
            .genre(BookGenre.TECHNOLOGY)
            .availableCopies(7) // Decreased by 1
            .publicationDate(LocalDate.of(2020, 1, 1))
            .build();

        when(inventoryService.borrowBook(bookId)).thenReturn(borrowedBook);
        when(bookMapper.toResponse(borrowedBook)).thenReturn(borrowedBookResponse);

        // When
        ResponseEntity<BookResponse> response = inventoryController.borrowBook(bookId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getTitle()).isEqualTo("Borrowing Test Book");
        assertThat(response.getBody().getAvailableCopies()).isEqualTo(7);

        verify(inventoryService).borrowBook(bookId);
        verify(bookMapper).toResponse(borrowedBook);
    }

    @Test
    @DisplayName("borrowBook_shouldCallServiceWithCorrectId_whenBookIdProvided")
    void borrowBook_shouldCallServiceWithCorrectId_whenBookIdProvided() {
        // Given
        Long bookId = 1L;
        when(inventoryService.borrowBook(bookId)).thenReturn(borrowedBook);
        when(bookMapper.toResponse(borrowedBook)).thenReturn(testBookResponse);

        // When
        inventoryController.borrowBook(bookId);

        // Then
        verify(inventoryService).borrowBook(bookId);
        verify(bookMapper).toResponse(borrowedBook);
    }

    @Test
    @DisplayName("borrowBook_shouldDecreaseAvailableCopies_whenBookBorrowed")
    void borrowBook_shouldDecreaseAvailableCopies_whenBookBorrowed() {
        // Given
        Long bookId = 1L;
        BookResponse borrowedBookResponse = BookResponse.builder()
            .id(1L)
            .title("Borrowing Test Book")
            .author("Test Author")
            .description("Test description")
            .genre(BookGenre.TECHNOLOGY)
            .availableCopies(7) // Original was 8, now 7
            .publicationDate(LocalDate.of(2020, 1, 1))
            .build();

        when(inventoryService.borrowBook(bookId)).thenReturn(borrowedBook);
        when(bookMapper.toResponse(borrowedBook)).thenReturn(borrowedBookResponse);

        // When
        ResponseEntity<BookResponse> response = inventoryController.borrowBook(bookId);

        // Then
        assertThat(response.getBody().getAvailableCopies()).isLessThan(8); // Original available copies
        assertThat(response.getBody().getAvailableCopies()).isEqualTo(7);
       
    }

    @Test
    @DisplayName("borrowBook_shouldHandleUnavailableBook_whenNoCopiesAvailable")
    void borrowBook_shouldHandleUnavailableBook_whenNoCopiesAvailable() {
        // Given
        Long bookId = 1L;
        Book unavailableBook = TestDataFactory.createUnavailableBook();
        unavailableBook.setId(bookId);

        BookResponse unavailableBookResponse = BookResponse.builder()
            .id(1L)
            .title("Clean Code")
            .author("Robert Martin")
            .description("Writing clean, maintainable code")
            .genre(BookGenre.TECHNOLOGY)
            .availableCopies(0) // No copies available
            .publicationDate(LocalDate.of(2008, 8, 1))
            .build();

        when(inventoryService.borrowBook(bookId)).thenReturn(unavailableBook);
        when(bookMapper.toResponse(unavailableBook)).thenReturn(unavailableBookResponse);

        // When
        ResponseEntity<BookResponse> response = inventoryController.borrowBook(bookId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getAvailableCopies()).isEqualTo(0);

        verify(inventoryService).borrowBook(bookId);
        verify(bookMapper).toResponse(unavailableBook);
    }

    // ========== Return Book Tests ==========

    @Test
    @DisplayName("returnBook_shouldReturnUpdatedBookResponse_whenValidBookIdProvided")
    void returnBook_shouldReturnUpdatedBookResponse_whenValidBookIdProvided() {
        // Given
        Long bookId = 2L;
        BookResponse returnedBookResponse = BookResponse.builder()
            .id(2L)
            .title("Return Test Book")
            .author("Test Author")
            .description("Test description")
            .genre(BookGenre.SCIENCE)
            .availableCopies(3) // Increased by 1
            .publicationDate(LocalDate.of(2020, 1, 1))
            .build();

        when(inventoryService.returnBook(bookId)).thenReturn(returnedBook);
        when(bookMapper.toResponse(returnedBook)).thenReturn(returnedBookResponse);

        // When
        ResponseEntity<BookResponse> response = inventoryController.returnBook(bookId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(2L);
        assertThat(response.getBody().getTitle()).isEqualTo("Return Test Book");
        assertThat(response.getBody().getAvailableCopies()).isEqualTo(3);

        verify(inventoryService).returnBook(bookId);
        verify(bookMapper).toResponse(returnedBook);
    }

    @Test
    @DisplayName("returnBook_shouldCallServiceWithCorrectId_whenBookIdProvided")
    void returnBook_shouldCallServiceWithCorrectId_whenBookIdProvided() {
        // Given
        Long bookId = 2L;
        when(inventoryService.returnBook(bookId)).thenReturn(returnedBook);
        when(bookMapper.toResponse(returnedBook)).thenReturn(testBookResponse);

        // When
        inventoryController.returnBook(bookId);

        // Then
        verify(inventoryService).returnBook(bookId);
        verify(bookMapper).toResponse(returnedBook);
    }

    @Test
    @DisplayName("returnBook_shouldIncreaseAvailableCopies_whenBookReturned")
    void returnBook_shouldIncreaseAvailableCopies_whenBookReturned() {
        // Given
        Long bookId = 2L;
        BookResponse returnedBookResponse = BookResponse.builder()
            .id(2L)
            .title("Return Test Book")
            .author("Test Author")
            .description("Test description")
            .genre(BookGenre.SCIENCE)
            .availableCopies(3) // Original was 2, now 3
            .publicationDate(LocalDate.of(2020, 1, 1))
            .build();

        when(inventoryService.returnBook(bookId)).thenReturn(returnedBook);
        when(bookMapper.toResponse(returnedBook)).thenReturn(returnedBookResponse);

        // When
        ResponseEntity<BookResponse> response = inventoryController.returnBook(bookId);

        // Then
        assertThat(response.getBody().getAvailableCopies()).isGreaterThan(2); // Original available copies
        assertThat(response.getBody().getAvailableCopies()).isEqualTo(3);
        
    }

    @Test
    @DisplayName("returnBook_shouldHandleBookWithAllCopiesAvailable_whenReturningBook")
    void returnBook_shouldHandleBookWithAllCopiesAvailable_whenReturningBook() {
        // Given
        Long bookId = 1L;
        Book fullyAvailableBook = TestDataFactory.createDefaultTechBook();
        fullyAvailableBook.setId(bookId);
        fullyAvailableBook.setAvailableCopies(5); // All copies available

        BookResponse fullyAvailableBookResponse = BookResponse.builder()
            .id(1L)
            .title("Effective Java")
            .author("Joshua Bloch")
            .description("Best practices for Java programming language")
            .genre(BookGenre.TECHNOLOGY)
            .availableCopies(5) // All copies available
            .publicationDate(LocalDate.of(2017, 12, 27))
            .build();

        when(inventoryService.returnBook(bookId)).thenReturn(fullyAvailableBook);
        when(bookMapper.toResponse(fullyAvailableBook)).thenReturn(fullyAvailableBookResponse);

        // When
        ResponseEntity<BookResponse> response = inventoryController.returnBook(bookId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getAvailableCopies()).isEqualTo(5);

        verify(inventoryService).returnBook(bookId);
        verify(bookMapper).toResponse(fullyAvailableBook);
    }

    // ========== Edge Cases and Error Scenarios ==========

    @Test
    @DisplayName("borrowBook_shouldHandleServiceException_whenServiceThrowsException")
    void borrowBook_shouldHandleServiceException_whenServiceThrowsException() {
        // Given
        Long bookId = 1L;
        when(inventoryService.borrowBook(bookId)).thenThrow(new RuntimeException("Book not found"));

        // When & Then
        try {
            inventoryController.borrowBook(bookId);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Book not found");
        }

        verify(inventoryService).borrowBook(bookId);
        verify(bookMapper, never()).toResponse(any(Book.class));
    }

    @Test
    @DisplayName("returnBook_shouldHandleServiceException_whenServiceThrowsException")
    void returnBook_shouldHandleServiceException_whenServiceThrowsException() {
        // Given
        Long bookId = 2L;
        when(inventoryService.returnBook(bookId)).thenThrow(new RuntimeException("Book not found"));

        // When & Then
        try {
            inventoryController.returnBook(bookId);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Book not found");
        }

        verify(inventoryService).returnBook(bookId);
        verify(bookMapper, never()).toResponse(any(Book.class));
    }

    @Test
    @DisplayName("borrowBook_shouldHandleNegativeBookId_whenInvalidIdProvided")
    void borrowBook_shouldHandleNegativeBookId_whenInvalidIdProvided() {
        // Given
        Long invalidBookId = -1L;
        when(inventoryService.borrowBook(invalidBookId)).thenThrow(new IllegalArgumentException("Invalid book ID"));

        // When & Then
        try {
            inventoryController.borrowBook(invalidBookId);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("Invalid book ID");
        }

        verify(inventoryService).borrowBook(invalidBookId);
        verify(bookMapper, never()).toResponse(any(Book.class));
    }

    @Test
    @DisplayName("returnBook_shouldHandleNegativeBookId_whenInvalidIdProvided")
    void returnBook_shouldHandleNegativeBookId_whenInvalidIdProvided() {
        // Given
        Long invalidBookId = -1L;
        when(inventoryService.returnBook(invalidBookId)).thenThrow(new IllegalArgumentException("Invalid book ID"));

        // When & Then
        try {
            inventoryController.returnBook(invalidBookId);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("Invalid book ID");
        }

        verify(inventoryService).returnBook(invalidBookId);
        verify(bookMapper, never()).toResponse(any(Book.class));
    }

    // ========== Business Logic Validation ==========

    @Test
    @DisplayName("borrowBook_shouldMaintainBookIntegrity_whenBorrowingBook")
    void borrowBook_shouldMaintainBookIntegrity_whenBorrowingBook() {
        // Given
        Long bookId = 1L;
        BookResponse borrowedBookResponse = BookResponse.builder()
            .id(1L)
            .title("Borrowing Test Book")
            .author("Test Author")
            .description("Test description")
            .genre(BookGenre.TECHNOLOGY)
            .availableCopies(7)
            .publicationDate(LocalDate.of(2020, 1, 1))
            .build();

        when(inventoryService.borrowBook(bookId)).thenReturn(borrowedBook);
        when(bookMapper.toResponse(borrowedBook)).thenReturn(borrowedBookResponse);

        // When
        ResponseEntity<BookResponse> response = inventoryController.borrowBook(bookId);

        // Then
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getTitle()).isEqualTo("Borrowing Test Book");
        assertThat(response.getBody().getAuthor()).isEqualTo("Test Author");
        assertThat(response.getBody().getGenre()).isEqualTo(BookGenre.TECHNOLOGY);
        assertThat(response.getBody().getPublicationDate()).isEqualTo(LocalDate.of(2020, 1, 1));
    }

    @Test
    @DisplayName("returnBook_shouldMaintainBookIntegrity_whenReturningBook")
    void returnBook_shouldMaintainBookIntegrity_whenReturningBook() {
        // Given
        Long bookId = 2L;
        BookResponse returnedBookResponse = BookResponse.builder()
            .id(2L)
            .title("Return Test Book")
            .author("Test Author")
            .description("Test description")
            .genre(BookGenre.SCIENCE)
            .availableCopies(3)
            .publicationDate(LocalDate.of(2020, 1, 1))
            .build();

        when(inventoryService.returnBook(bookId)).thenReturn(returnedBook);
        when(bookMapper.toResponse(returnedBook)).thenReturn(returnedBookResponse);

        // When
        ResponseEntity<BookResponse> response = inventoryController.returnBook(bookId);

        // Then
        assertThat(response.getBody().getId()).isEqualTo(2L);
        assertThat(response.getBody().getTitle()).isEqualTo("Return Test Book");
        assertThat(response.getBody().getAuthor()).isEqualTo("Test Author");
        assertThat(response.getBody().getGenre()).isEqualTo(BookGenre.SCIENCE);
        assertThat(response.getBody().getPublicationDate()).isEqualTo(LocalDate.of(2020, 1, 1));
    }

    // ========== Integration with Dependencies ==========

    @Test
    @DisplayName("borrowBook_shouldUseAllDependenciesCorrectly_whenProcessingBorrowRequest")
    void borrowBook_shouldUseAllDependenciesCorrectly_whenProcessingBorrowRequest() {
        // Given
        Long bookId = 1L;
        when(inventoryService.borrowBook(bookId)).thenReturn(borrowedBook);
        when(bookMapper.toResponse(borrowedBook)).thenReturn(testBookResponse);

        // When
        inventoryController.borrowBook(bookId);

        // Then
        verify(inventoryService).borrowBook(bookId);
        verify(bookMapper).toResponse(borrowedBook);
        verifyNoMoreInteractions(inventoryService, bookMapper);
    }

    @Test
    @DisplayName("returnBook_shouldUseAllDependenciesCorrectly_whenProcessingReturnRequest")
    void returnBook_shouldUseAllDependenciesCorrectly_whenProcessingReturnRequest() {
        // Given
        Long bookId = 2L;
        when(inventoryService.returnBook(bookId)).thenReturn(returnedBook);
        when(bookMapper.toResponse(returnedBook)).thenReturn(testBookResponse);

        // When
        inventoryController.returnBook(bookId);

        // Then
        verify(inventoryService).returnBook(bookId);
        verify(bookMapper).toResponse(returnedBook);
        verifyNoMoreInteractions(inventoryService, bookMapper);
    }

    // ========== Multiple Operations Tests ==========

    @Test
    @DisplayName("borrowAndReturnBook_shouldHandleMultipleOperations_whenProcessingSequentialRequests")
    void borrowAndReturnBook_shouldHandleMultipleOperations_whenProcessingSequentialRequests() {
        // Given
        Long bookId = 1L;
        
        // Setup for borrow
        when(inventoryService.borrowBook(bookId)).thenReturn(borrowedBook);
        when(bookMapper.toResponse(borrowedBook)).thenReturn(testBookResponse);
        
        // Setup for return
        when(inventoryService.returnBook(bookId)).thenReturn(returnedBook);
        when(bookMapper.toResponse(returnedBook)).thenReturn(testBookResponse);

        // When
        ResponseEntity<BookResponse> borrowResponse = inventoryController.borrowBook(bookId);
        ResponseEntity<BookResponse> returnResponse = inventoryController.returnBook(bookId);

        // Then
        assertThat(borrowResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(returnResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(inventoryService).borrowBook(bookId);
        verify(inventoryService).returnBook(bookId);
        verify(bookMapper, times(2)).toResponse(any(Book.class));
    }
}
