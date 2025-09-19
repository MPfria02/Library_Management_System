package com.librarymanager.backend.controller;

import com.librarymanager.backend.dto.response.BookResponse;
import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BookGenre;
import com.librarymanager.backend.mapper.BookMapper;
import com.librarymanager.backend.service.BookStatisticsService;
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
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookStatisticsController.
 * 
 * Tests book statistics and analytics endpoints including count operations and reporting functionality.
 * Follows Spring Boot testing best practices with proper mocking and assertions.
 * 
 * @author Marcel Pulido
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookStatisticsController Unit Tests")
class BookStatisticsControllerTest {

    @Mock
    private BookStatisticsService bookStatisticsService;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookStatisticsController bookStatisticsController;

    private List<Book> testBooks;
    private List<BookResponse> testBookResponses;

    @BeforeEach
    void setUp() {
        // Setup test data
        testBooks = TestDataFactory.createSampleBooks();
        testBooks.forEach(book -> book.setId((long) (testBooks.indexOf(book) + 1)));

        testBookResponses = Arrays.asList(
            createBookResponse(1L, "Effective Java", "Joshua Bloch", BookGenre.TECHNOLOGY, 3),
            createBookResponse(2L, "To Kill a Mockingbird", "Harper Lee", BookGenre.FICTION, 2),
            createBookResponse(3L, "Clean Code", "Robert Martin", BookGenre.TECHNOLOGY, 0),
            createBookResponse(4L, "Learning Python", "Mark Lutz", BookGenre.TECHNOLOGY, 4),
            createBookResponse(5L, "A Brief History of Time", "Stephen Hawking", BookGenre.SCIENCE, 1),
            createBookResponse(6L, "Of Mice and Men", "John Steinbeck", BookGenre.FICTION, 2),
            createBookResponse(7L, "Foundation", "Isaac Asimov", BookGenre.FANTASY, 0)
        );
    }

    private BookResponse createBookResponse(Long id, String title, String author, BookGenre genre, Integer availableCopies) {
        return BookResponse.builder()
            .id(id)
            .title(title)
            .author(author)
            .description("Test description")
            .genre(genre)
            .availableCopies(availableCopies)
            .publicationDate(LocalDate.of(2020, 1, 1))
            .build();
    }

    // ========== Total Book Count Tests ==========

    @Test
    @DisplayName("getTotalBookCount_shouldReturnTotalCount_whenCalled")
    void getTotalBookCount_shouldReturnTotalCount_whenCalled() {
        // Given
        long expectedCount = 7L;
        when(bookStatisticsService.countAllBooks()).thenReturn(expectedCount);

        // When
        ResponseEntity<Long> response = bookStatisticsController.getTotalBookCount();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedCount);

        verify(bookStatisticsService).countAllBooks();
    }

    @Test
    @DisplayName("getTotalBookCount_shouldReturnZero_whenNoBooksExist")
    void getTotalBookCount_shouldReturnZero_whenNoBooksExist() {
        // Given
        long expectedCount = 0L;
        when(bookStatisticsService.countAllBooks()).thenReturn(expectedCount);

        // When
        ResponseEntity<Long> response = bookStatisticsController.getTotalBookCount();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(0L);

        verify(bookStatisticsService).countAllBooks();
    }

    @Test
    @DisplayName("getTotalBookCount_shouldCallServiceOnce_whenCalled")
    void getTotalBookCount_shouldCallServiceOnce_whenCalled() {
        // Given
        when(bookStatisticsService.countAllBooks()).thenReturn(7L);

        // When
        bookStatisticsController.getTotalBookCount();

        // Then
        verify(bookStatisticsService, times(1)).countAllBooks();
        verifyNoMoreInteractions(bookStatisticsService);
    }

    // ========== Available Book Count Tests ==========

    @Test
    @DisplayName("getAvailableBookCount_shouldReturnAvailableCount_whenCalled")
    void getAvailableBookCount_shouldReturnAvailableCount_whenCalled() {
        // Given
        long expectedCount = 5L; // Books with available copies > 0
        when(bookStatisticsService.countAvailableBooks()).thenReturn(expectedCount);

        // When
        ResponseEntity<Long> response = bookStatisticsController.getAvailableBookCount();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedCount);

        verify(bookStatisticsService).countAvailableBooks();
    }

    @Test
    @DisplayName("getAvailableBookCount_shouldReturnZero_whenNoAvailableBooks")
    void getAvailableBookCount_shouldReturnZero_whenNoAvailableBooks() {
        // Given
        long expectedCount = 0L;
        when(bookStatisticsService.countAvailableBooks()).thenReturn(expectedCount);

        // When
        ResponseEntity<Long> response = bookStatisticsController.getAvailableBookCount();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(0L);

        verify(bookStatisticsService).countAvailableBooks();
    }

    // ========== Books with Borrowed Copies Tests ==========

    @Test
    @DisplayName("getBooksWithBorrowedCopies_shouldReturnBooksWithBorrowedCopies_whenCalled")
    void getBooksWithBorrowedCopies_shouldReturnBooksWithBorrowedCopies_whenCalled() {
        // Given
        List<Book> booksWithBorrowedCopies = testBooks.stream()
            .filter(book -> book.getAvailableCopies() < book.getTotalCopies())
            .toList();
        

        when(bookStatisticsService.getBooksWithBorrowedCopies()).thenReturn(booksWithBorrowedCopies);
        when(bookMapper.toResponse(any(Book.class))).thenAnswer(invocation -> {
            Book book = invocation.getArgument(0);
            return testBookResponses.stream()
                .filter(response -> response.getTitle().equals(book.getTitle()))
                .findFirst()
                .orElse(testBookResponses.get(0));
        });

        // When
        ResponseEntity<List<BookResponse>> response = bookStatisticsController.getBooksWithBorrowedCopies();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(booksWithBorrowedCopies.size());

        verify(bookStatisticsService).getBooksWithBorrowedCopies();
        verify(bookMapper, times(booksWithBorrowedCopies.size())).toResponse(any(Book.class));
    }

    @Test
    @DisplayName("getBooksWithBorrowedCopies_shouldReturnEmptyList_whenNoBooksHaveBorrowedCopies")
    void getBooksWithBorrowedCopies_shouldReturnEmptyList_whenNoBooksHaveBorrowedCopies() {
        // Given
        when(bookStatisticsService.getBooksWithBorrowedCopies()).thenReturn(Arrays.asList());

        // When
        ResponseEntity<List<BookResponse>> response = bookStatisticsController.getBooksWithBorrowedCopies();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();

        verify(bookStatisticsService).getBooksWithBorrowedCopies();
        verify(bookMapper, never()).toResponse(any(Book.class));
    }

    // ========== Available Books Count by Genre Tests ==========

    @Test
    @DisplayName("getAvailableBooksCountByGenre_shouldReturnCountForGenre_whenGenreProvided")
    void getAvailableBooksCountByGenre_shouldReturnCountForGenre_whenGenreProvided() {
        // Given
        BookGenre genre = BookGenre.TECHNOLOGY;
        long expectedCount = 2L; // Technology books with available copies
        when(bookStatisticsService.countAvailableBooksByGenre(genre)).thenReturn(expectedCount);

        // When
        ResponseEntity<Long> response = bookStatisticsController.getAvailableBooksCountByGenre(genre);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedCount);

        verify(bookStatisticsService).countAvailableBooksByGenre(genre);
    }

    @Test
    @DisplayName("getAvailableBooksCountByGenre_shouldReturnZero_whenNoBooksInGenre")
    void getAvailableBooksCountByGenre_shouldReturnZero_whenNoBooksInGenre() {
        // Given
        BookGenre genre = BookGenre.MYSTERY;
        long expectedCount = 0L;
        when(bookStatisticsService.countAvailableBooksByGenre(genre)).thenReturn(expectedCount);

        // When
        ResponseEntity<Long> response = bookStatisticsController.getAvailableBooksCountByGenre(genre);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(0L);

        verify(bookStatisticsService).countAvailableBooksByGenre(genre);
    }

    @Test
    @DisplayName("getAvailableBooksCountByGenre_shouldHandleAllGenres_whenDifferentGenresProvided")
    void getAvailableBooksCountByGenre_shouldHandleAllGenres_whenDifferentGenresProvided() {
        // Given
        BookGenre[] genres = {BookGenre.FICTION, BookGenre.SCIENCE, BookGenre.HISTORY};
        long[] expectedCounts = {2L, 1L, 0L};

        for (int i = 0; i < genres.length; i++) {
            when(bookStatisticsService.countAvailableBooksByGenre(genres[i])).thenReturn(expectedCounts[i]);
        }

        // When & Then
        for (int i = 0; i < genres.length; i++) {
            ResponseEntity<Long> response = bookStatisticsController.getAvailableBooksCountByGenre(genres[i]);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(expectedCounts[i]);
        }

        verify(bookStatisticsService, times(genres.length)).countAvailableBooksByGenre(any(BookGenre.class));
    }

    // ========== Availability Percentage Tests ==========

    @Test
    @DisplayName("getAvailabilityPercentage_shouldReturnCorrectPercentage_whenBooksExist")
    void getAvailabilityPercentage_shouldReturnCorrectPercentage_whenBooksExist() {
        // Given
        long totalBooks = 7L;
        long availableBooks = 5L;
        double expectedPercentage = 71.43; // (5/7) * 100, rounded to 2 decimal places

        when(bookStatisticsService.countAllBooks()).thenReturn(totalBooks);
        when(bookStatisticsService.countAvailableBooks()).thenReturn(availableBooks);

        // When
        ResponseEntity<Double> response = bookStatisticsController.getAvailabilityPercentage();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedPercentage);

        verify(bookStatisticsService).countAllBooks();
        verify(bookStatisticsService).countAvailableBooks();
    }

    @Test
    @DisplayName("getAvailabilityPercentage_shouldReturnZero_whenNoBooksExist")
    void getAvailabilityPercentage_shouldReturnZero_whenNoBooksExist() {
        // Given
        long totalBooks = 0L;
        long availableBooks = 0L;
        double expectedPercentage = 0.0;

        when(bookStatisticsService.countAllBooks()).thenReturn(totalBooks);
        when(bookStatisticsService.countAvailableBooks()).thenReturn(availableBooks);

        // When
        ResponseEntity<Double> response = bookStatisticsController.getAvailabilityPercentage();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedPercentage);

        verify(bookStatisticsService).countAllBooks();
        verify(bookStatisticsService).countAvailableBooks();
    }

    @Test
    @DisplayName("getAvailabilityPercentage_shouldReturnHundredPercent_whenAllBooksAvailable")
    void getAvailabilityPercentage_shouldReturnHundredPercent_whenAllBooksAvailable() {
        // Given
        long totalBooks = 5L;
        long availableBooks = 5L;
        double expectedPercentage = 100.0;

        when(bookStatisticsService.countAllBooks()).thenReturn(totalBooks);
        when(bookStatisticsService.countAvailableBooks()).thenReturn(availableBooks);

        // When
        ResponseEntity<Double> response = bookStatisticsController.getAvailabilityPercentage();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedPercentage);

        verify(bookStatisticsService).countAllBooks();
        verify(bookStatisticsService).countAvailableBooks();
    }

    @Test
    @DisplayName("getAvailabilityPercentage_shouldRoundToTwoDecimalPlaces_whenCalculatingPercentage")
    void getAvailabilityPercentage_shouldRoundToTwoDecimalPlaces_whenCalculatingPercentage() {
        // Given
        long totalBooks = 3L;
        long availableBooks = 1L;
        double expectedPercentage = 33.33; // (1/3) * 100 = 33.333..., rounded to 33.33

        when(bookStatisticsService.countAllBooks()).thenReturn(totalBooks);
        when(bookStatisticsService.countAvailableBooks()).thenReturn(availableBooks);

        // When
        ResponseEntity<Double> response = bookStatisticsController.getAvailabilityPercentage();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedPercentage);

        verify(bookStatisticsService).countAllBooks();
        verify(bookStatisticsService).countAvailableBooks();
    }

    // ========== Edge Cases and Error Scenarios ==========

    @Test
    @DisplayName("getTotalBookCount_shouldHandleServiceException_whenServiceThrowsException")
    void getTotalBookCount_shouldHandleServiceException_whenServiceThrowsException() {
        // Given
        when(bookStatisticsService.countAllBooks()).thenThrow(new RuntimeException("Database error"));

        // When & Then
        try {
            bookStatisticsController.getTotalBookCount();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Database error");
        }

        verify(bookStatisticsService).countAllBooks();
    }

    @Test
    @DisplayName("getAvailableBookCount_shouldHandleServiceException_whenServiceThrowsException")
    void getAvailableBookCount_shouldHandleServiceException_whenServiceThrowsException() {
        // Given
        when(bookStatisticsService.countAvailableBooks()).thenThrow(new RuntimeException("Database error"));

        // When & Then
        try {
            bookStatisticsController.getAvailableBookCount();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Database error");
        }

        verify(bookStatisticsService).countAvailableBooks();
    }

    @Test
    @DisplayName("getBooksWithBorrowedCopies_shouldHandleServiceException_whenServiceThrowsException")
    void getBooksWithBorrowedCopies_shouldHandleServiceException_whenServiceThrowsException() {
        // Given
        when(bookStatisticsService.getBooksWithBorrowedCopies()).thenThrow(new RuntimeException("Database error"));

        // When & Then
        try {
            bookStatisticsController.getBooksWithBorrowedCopies();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Database error");
        }

        verify(bookStatisticsService).getBooksWithBorrowedCopies();
        verify(bookMapper, never()).toResponse(any(Book.class));
    }

    @Test
    @DisplayName("getAvailableBooksCountByGenre_shouldHandleServiceException_whenServiceThrowsException")
    void getAvailableBooksCountByGenre_shouldHandleServiceException_whenServiceThrowsException() {
        // Given
        BookGenre genre = BookGenre.TECHNOLOGY;
        when(bookStatisticsService.countAvailableBooksByGenre(genre)).thenThrow(new RuntimeException("Database error"));

        // When & Then
        try {
            bookStatisticsController.getAvailableBooksCountByGenre(genre);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Database error");
        }

        verify(bookStatisticsService).countAvailableBooksByGenre(genre);
    }

    @Test
    @DisplayName("getAvailabilityPercentage_shouldHandleServiceException_whenServiceThrowsException")
    void getAvailabilityPercentage_shouldHandleServiceException_whenServiceThrowsException() {
        // Given
        when(bookStatisticsService.countAllBooks()).thenThrow(new RuntimeException("Database error"));

        // When & Then
        try {
            bookStatisticsController.getAvailabilityPercentage();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Database error");
        }

        verify(bookStatisticsService).countAllBooks();
        verify(bookStatisticsService, never()).countAvailableBooks();
    }

    // ========== Integration with Dependencies ==========

    @Test
    @DisplayName("getTotalBookCount_shouldUseAllDependenciesCorrectly_whenProcessingRequest")
    void getTotalBookCount_shouldUseAllDependenciesCorrectly_whenProcessingRequest() {
        // Given
        when(bookStatisticsService.countAllBooks()).thenReturn(7L);

        // When
        bookStatisticsController.getTotalBookCount();

        // Then
        verify(bookStatisticsService).countAllBooks();
        verifyNoMoreInteractions(bookStatisticsService, bookMapper);
    }

    @Test
    @DisplayName("getBooksWithBorrowedCopies_shouldUseAllDependenciesCorrectly_whenProcessingRequest")
    void getBooksWithBorrowedCopies_shouldUseAllDependenciesCorrectly_whenProcessingRequest() {
        // Given
        List<Book> booksWithBorrowedCopies = testBooks.stream()
            .filter(book -> book.getAvailableCopies() < book.getTotalCopies())
            .toList();

        when(bookStatisticsService.getBooksWithBorrowedCopies()).thenReturn(booksWithBorrowedCopies);
        when(bookMapper.toResponse(any(Book.class))).thenReturn(testBookResponses.get(0));

        // When
        bookStatisticsController.getBooksWithBorrowedCopies();

        // Then
        verify(bookStatisticsService).getBooksWithBorrowedCopies();
        verify(bookMapper, times(booksWithBorrowedCopies.size())).toResponse(any(Book.class));
        verifyNoMoreInteractions(bookStatisticsService, bookMapper);
    }

    @Test
    @DisplayName("getAvailabilityPercentage_shouldUseAllDependenciesCorrectly_whenProcessingRequest")
    void getAvailabilityPercentage_shouldUseAllDependenciesCorrectly_whenProcessingRequest() {
        // Given
        when(bookStatisticsService.countAllBooks()).thenReturn(7L);
        when(bookStatisticsService.countAvailableBooks()).thenReturn(5L);

        // When
        bookStatisticsController.getAvailabilityPercentage();

        // Then
        verify(bookStatisticsService).countAllBooks();
        verify(bookStatisticsService).countAvailableBooks();
        verifyNoMoreInteractions(bookStatisticsService, bookMapper);
    }

    // ========== Performance and Consistency Tests ==========

    @Test
    @DisplayName("getTotalBookCount_shouldReturnConsistentResults_whenCalledMultipleTimes")
    void getTotalBookCount_shouldReturnConsistentResults_whenCalledMultipleTimes() {
        // Given
        long expectedCount = 7L;
        when(bookStatisticsService.countAllBooks()).thenReturn(expectedCount);

        // When
        ResponseEntity<Long> response1 = bookStatisticsController.getTotalBookCount();
        ResponseEntity<Long> response2 = bookStatisticsController.getTotalBookCount();

        // Then
        assertThat(response1.getBody()).isEqualTo(response2.getBody());
        assertThat(response1.getBody()).isEqualTo(expectedCount);
        verify(bookStatisticsService, times(2)).countAllBooks();
    }

    @Test
    @DisplayName("getAvailabilityPercentage_shouldCalculateCorrectly_whenCalledWithDifferentCounts")
    void getAvailabilityPercentage_shouldCalculateCorrectly_whenCalledWithDifferentCounts() {
        // Given
        when(bookStatisticsService.countAllBooks()).thenReturn(10L);
        when(bookStatisticsService.countAvailableBooks()).thenReturn(7L);

        // When
        ResponseEntity<Double> response = bookStatisticsController.getAvailabilityPercentage();

        // Then
        assertThat(response.getBody()).isEqualTo(70.0); // (7/10) * 100 = 70.0
    }
}
