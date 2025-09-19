package com.librarymanager.backend.controller;

import com.librarymanager.backend.dto.request.BookCreationRequest;
import com.librarymanager.backend.dto.response.BookResponse;
import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BookGenre;
import com.librarymanager.backend.mapper.BookMapper;
import com.librarymanager.backend.service.BookCatalogService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookCatalogController.
 * 
 * Tests book catalog endpoints including CRUD operations, search functionality, and filtering.
 * Follows Spring Boot testing best practices with proper mocking and assertions.
 * 
 * @author Marcel Pulido
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookCatalogController Unit Tests")
class BookCatalogControllerTest {

    @Mock
    private BookCatalogService bookCatalogService;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookCatalogController bookCatalogController;

    private Book testBook;
    private BookResponse testBookResponse;
    private BookCreationRequest testBookCreationRequest;
    private List<Book> testBooks;
    private List<BookResponse> testBookResponses;

    @BeforeEach
    void setUp() {
        // Setup test data
        testBook = TestDataFactory.createDefaultTechBook();
        testBook.setId(1L);

        testBookResponse = BookResponse.builder()
            .id(1L)
            .title("Effective Java")
            .author("Joshua Bloch")
            .description("Best practices for Java programming language")
            .genre(BookGenre.TECHNOLOGY)
            .availableCopies(3)
            .publicationDate(LocalDate.of(2017, 12, 27))
            .build();

        testBookCreationRequest = new BookCreationRequest();
        testBookCreationRequest.setIsbn("978-0134685991");
        testBookCreationRequest.setTitle("Effective Java");
        testBookCreationRequest.setAuthor("Joshua Bloch");
        testBookCreationRequest.setDescription("Best practices for Java programming language");
        testBookCreationRequest.setGenre(BookGenre.TECHNOLOGY);
        testBookCreationRequest.setTotalCopies(5);
        testBookCreationRequest.setAvailableCopies(3);
        testBookCreationRequest.setPublicationDate(LocalDate.of(2017, 12, 27));

        testBooks = TestDataFactory.createSampleBooks();
        testBooks.forEach(book -> book.setId((long) (testBooks.indexOf(book) + 1)));

        testBookResponses = Arrays.asList(
            createBookResponse(1L, "Effective Java", "Joshua Bloch", BookGenre.TECHNOLOGY, 3),
            createBookResponse(2L, "To Kill a Mockingbird", "Harper Lee", BookGenre.FICTION, 2),
            createBookResponse(3L, "Clean Code", "Robert Martin", BookGenre.TECHNOLOGY, 0),
            createBookResponse(4L, "Learning Python", "Mark Lutz", BookGenre.TECHNOLOGY, 4)
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

    // ========== Create Book Tests ==========

    @Test
    @DisplayName("createBook_shouldReturnCreatedBookResponse_whenValidRequestProvided")
    void createBook_shouldReturnCreatedBookResponse_whenValidRequestProvided() {
        // Given
        when(bookMapper.toEntity(testBookCreationRequest)).thenReturn(testBook);
        when(bookCatalogService.createBook(testBook)).thenReturn(testBook);
        when(bookMapper.toResponse(testBook)).thenReturn(testBookResponse);

        // When
        ResponseEntity<BookResponse> response = bookCatalogController.createBook(testBookCreationRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Effective Java");
        assertThat(response.getBody().getAuthor()).isEqualTo("Joshua Bloch");
        assertThat(response.getBody().getGenre()).isEqualTo(BookGenre.TECHNOLOGY);

        verify(bookMapper).toEntity(testBookCreationRequest);
        verify(bookCatalogService).createBook(testBook);
        verify(bookMapper).toResponse(testBook);
    }

    @Test
    @DisplayName("createBook_shouldCallServiceWithMappedEntity_whenValidRequestProvided")
    void createBook_shouldCallServiceWithMappedEntity_whenValidRequestProvided() {
        // Given
        when(bookMapper.toEntity(testBookCreationRequest)).thenReturn(testBook);
        when(bookCatalogService.createBook(testBook)).thenReturn(testBook);
        when(bookMapper.toResponse(testBook)).thenReturn(testBookResponse);

        // When
        bookCatalogController.createBook(testBookCreationRequest);

        // Then
        verify(bookMapper).toEntity(testBookCreationRequest);
        verify(bookCatalogService).createBook(testBook);
        verify(bookMapper).toResponse(testBook);
    }

    // ========== Get Book by ID Tests ==========

    @Test
    @DisplayName("getBookById_shouldReturnBookResponse_whenValidIdProvided")
    void getBookById_shouldReturnBookResponse_whenValidIdProvided() {
        // Given
        Long bookId = 1L;
        when(bookCatalogService.findById(bookId)).thenReturn(testBook);
        when(bookMapper.toResponse(testBook)).thenReturn(testBookResponse);

        // When
        ResponseEntity<BookResponse> response = bookCatalogController.getBookById(bookId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getTitle()).isEqualTo("Effective Java");

        verify(bookCatalogService).findById(bookId);
        verify(bookMapper).toResponse(testBook);
    }

    @Test
    @DisplayName("getBookById_shouldCallServiceWithCorrectId_whenIdProvided")
    void getBookById_shouldCallServiceWithCorrectId_whenIdProvided() {
        // Given
        Long bookId = 1L;
        when(bookCatalogService.findById(bookId)).thenReturn(testBook);
        when(bookMapper.toResponse(testBook)).thenReturn(testBookResponse);

        // When
        bookCatalogController.getBookById(bookId);

        // Then
        verify(bookCatalogService).findById(bookId);
        verify(bookMapper).toResponse(testBook);
    }

    // ========== Get Book by ISBN Tests ==========

    @Test
    @DisplayName("getBookByIsbn_shouldReturnBookResponse_whenValidIsbnProvided")
    void getBookByIsbn_shouldReturnBookResponse_whenValidIsbnProvided() {
        // Given
        String isbn = "978-0134685991";
        when(bookCatalogService.findByIsbn(isbn)).thenReturn(testBook);
        when(bookMapper.toResponse(testBook)).thenReturn(testBookResponse);

        // When
        ResponseEntity<BookResponse> response = bookCatalogController.getBookByIsbn(isbn);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Effective Java");

        verify(bookCatalogService).findByIsbn(isbn);
        verify(bookMapper).toResponse(testBook);
    }

    // ========== Update Book Tests ==========

    @Test
    @DisplayName("updateBook_shouldReturnUpdatedBookResponse_whenValidRequestProvided")
    void updateBook_shouldReturnUpdatedBookResponse_whenValidRequestProvided() {
        // Given
        Long bookId = 1L;
        Book updatedBook = TestDataFactory.createDefaultTechBook();
        updatedBook.setId(bookId);
        updatedBook.setTitle("Updated Effective Java");

        BookResponse updatedResponse = BookResponse.builder()
            .id(bookId)
            .title("Updated Effective Java")
            .author("Joshua Bloch")
            .description("Best practices for Java programming language")
            .genre(BookGenre.TECHNOLOGY)
            .availableCopies(3)
            .publicationDate(LocalDate.of(2017, 12, 27))
            .build();

        when(bookMapper.toEntity(testBookCreationRequest)).thenReturn(updatedBook);
        when(bookCatalogService.updateBook(any(Book.class))).thenReturn(updatedBook);
        when(bookMapper.toResponse(updatedBook)).thenReturn(updatedResponse);

        // When
        ResponseEntity<BookResponse> response = bookCatalogController.updateBook(bookId, testBookCreationRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Updated Effective Java");

        verify(bookMapper).toEntity(testBookCreationRequest);
        verify(bookCatalogService).updateBook(argThat(book -> bookId.equals(book.getId())));
        verify(bookMapper).toResponse(updatedBook);
    }

    @Test
    @DisplayName("updateBook_shouldSetIdOnBook_whenUpdatingExistingBook")
    void updateBook_shouldSetIdOnBook_whenUpdatingExistingBook() {
        // Given
        Long bookId = 1L;
        when(bookMapper.toEntity(testBookCreationRequest)).thenReturn(testBook);
        when(bookCatalogService.updateBook(any(Book.class))).thenReturn(testBook);
        when(bookMapper.toResponse(testBook)).thenReturn(testBookResponse);

        // When
        bookCatalogController.updateBook(bookId, testBookCreationRequest);

        // Then
        verify(bookCatalogService).updateBook(argThat(book -> bookId.equals(book.getId())));
    }

    // ========== Delete Book Tests ==========

    @Test
    @DisplayName("deleteBook_shouldReturnNoContent_whenValidIdProvided")
    void deleteBook_shouldReturnNoContent_whenValidIdProvided() {
        // Given
        Long bookId = 1L;
        doNothing().when(bookCatalogService).deleteBook(bookId);

        // When
        ResponseEntity<Void> response = bookCatalogController.deleteBook(bookId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();

        verify(bookCatalogService).deleteBook(bookId);
    }

    @Test
    @DisplayName("deleteBook_shouldCallServiceWithCorrectId_whenIdProvided")
    void deleteBook_shouldCallServiceWithCorrectId_whenIdProvided() {
        // Given
        Long bookId = 1L;
        doNothing().when(bookCatalogService).deleteBook(bookId);

        // When
        bookCatalogController.deleteBook(bookId);

        // Then
        verify(bookCatalogService).deleteBook(bookId);
    }

    // ========== Get Books with Pagination Tests ==========

    @Test
    @DisplayName("getBooks_shouldReturnPaginatedBooks_whenDefaultParametersProvided")
    void getBooks_shouldReturnPaginatedBooks_whenDefaultParametersProvided() {
        // Given
        Page<Book> bookPage = new PageImpl<>(testBooks, PageRequest.of(0, 20), testBooks.size());

        when(bookCatalogService.searchBooks(isNull(), isNull(), eq(false), any(Pageable.class))).thenReturn(bookPage);
        when(bookMapper.toResponse(any(Book.class))).thenAnswer(invocation -> {
            Book book = invocation.getArgument(0);
            return testBookResponses.stream()
                .filter(response -> response.getTitle().equals(book.getTitle()))
                .findFirst()
                .orElse(testBookResponses.get(0));
        });

        // When
        ResponseEntity<Page<BookResponse>> response = bookCatalogController.getBooks(0, 20, "title", "asc", null, null, false);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(7);
        assertThat(response.getBody().getTotalElements()).isEqualTo(7);

        verify(bookCatalogService).searchBooks(isNull(), isNull(), eq(false), any(Pageable.class));
    }

    @Test
    @DisplayName("getBooks_shouldReturnFilteredBooks_whenSearchTermProvided")
    void getBooks_shouldReturnFilteredBooks_whenSearchTermProvided() {
        // Given
        String searchTerm = "Java";
        List<Book> filteredBooks = testBooks.stream()
            .filter(book -> book.getTitle().contains(searchTerm) || book.getAuthor().contains(searchTerm))
            .toList();
        Page<Book> bookPage = new PageImpl<>(filteredBooks, PageRequest.of(0, 20), filteredBooks.size());

        when(bookCatalogService.searchBooks(eq(searchTerm), isNull(), eq(false), any(Pageable.class))).thenReturn(bookPage);
        when(bookMapper.toResponse(any(Book.class))).thenReturn(testBookResponse);

        // When
        ResponseEntity<Page<BookResponse>> response = bookCatalogController.getBooks(0, 20, "title", "asc", searchTerm, null, false);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent().size()).isEqualTo(1);

        verify(bookCatalogService).searchBooks(eq(searchTerm), isNull(), eq(false), any(Pageable.class));
    }

    @Test
    @DisplayName("getBooks_shouldReturnBooksByGenre_whenGenreFilterProvided")
    void getBooks_shouldReturnBooksByGenre_whenGenreFilterProvided() {
        // Given
        BookGenre genre = BookGenre.TECHNOLOGY;
        List<Book> techBooks = testBooks.stream()
            .filter(book -> book.getGenre() == genre)
            .toList();
        Page<Book> bookPage = new PageImpl<>(techBooks, PageRequest.of(0, 20), techBooks.size());

        when(bookCatalogService.searchBooks(isNull(), eq(genre), eq(false), any(Pageable.class))).thenReturn(bookPage);
        when(bookMapper.toResponse(any(Book.class))).thenReturn(testBookResponse);

        // When
        ResponseEntity<Page<BookResponse>> response = bookCatalogController.getBooks(0, 20, "title", "asc", null, genre, false);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        verify(bookCatalogService).searchBooks(isNull(), eq(genre), eq(false), any(Pageable.class));
    }

    @Test
    @DisplayName("getBooks_shouldReturnAvailableBooksOnly_whenAvailableOnlyTrue")
    void getBooks_shouldReturnAvailableBooksOnly_whenAvailableOnlyTrue() {
        // Given
        List<Book> availableBooks = testBooks.stream()
            .filter(book -> book.getAvailableCopies() > 0)
            .toList();
        Page<Book> bookPage = new PageImpl<>(availableBooks, PageRequest.of(0, 20), availableBooks.size());

        when(bookCatalogService.searchBooks(isNull(), isNull(), eq(true), any(Pageable.class))).thenReturn(bookPage);
        when(bookMapper.toResponse(any(Book.class))).thenReturn(testBookResponse);

        // When
        ResponseEntity<Page<BookResponse>> response = bookCatalogController.getBooks(0, 20, "title", "asc", null, null, true);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        verify(bookCatalogService).searchBooks(isNull(), isNull(), eq(true), any(Pageable.class));
    }

    // ========== Search Books Tests ==========

    @Test
    @DisplayName("searchBooks_shouldReturnSearchResults_whenSearchParametersProvided")
    void searchBooks_shouldReturnSearchResults_whenSearchParametersProvided() {
        // Given
        String searchTerm = "Java";
        BookGenre genre = BookGenre.TECHNOLOGY;
        boolean availableOnly = true;
        Page<Book> bookPage = new PageImpl<>(testBooks, PageRequest.of(0, 20), testBooks.size());

        when(bookCatalogService.searchBooks(eq(searchTerm), eq(genre), eq(availableOnly), any(Pageable.class))).thenReturn(bookPage);
        when(bookMapper.toResponse(any(Book.class))).thenReturn(testBookResponse);

        // When
        ResponseEntity<Page<BookResponse>> response = bookCatalogController.searchBooks(searchTerm, genre, availableOnly, 0, 20, "title", "asc");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        verify(bookCatalogService).searchBooks(eq(searchTerm), eq(genre), eq(availableOnly), any(Pageable.class));
    }

    // ========== Full Text Search Tests ==========

    @Test
    @DisplayName("fullTextSearch_shouldReturnRelevantBooks_whenSearchTermProvided")
    void fullTextSearch_shouldReturnRelevantBooks_whenSearchTermProvided() {
        // Given
        String searchTerm = "programming";
        List<Book> searchResults = testBooks.stream()
            .filter(book -> book.getDescription().contains(searchTerm))
            .toList();

        when(bookCatalogService.fullTextSearch(searchTerm)).thenReturn(searchResults);
        when(bookMapper.toResponse(any(Book.class))).thenReturn(testBookResponse);

        // When
        ResponseEntity<List<BookResponse>> response = bookCatalogController.fullTextSearch(searchTerm);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(searchResults.size());

        verify(bookCatalogService).fullTextSearch(searchTerm);
        verify(bookMapper, times(searchResults.size())).toResponse(any(Book.class));
    }

    // ========== Get Books by Genre Tests ==========

    @Test
    @DisplayName("getBooksByGenre_shouldReturnBooksInGenre_whenGenreProvided")
    void getBooksByGenre_shouldReturnBooksInGenre_whenGenreProvided() {
        // Given
        BookGenre genre = BookGenre.FICTION;
        List<Book> fictionBooks = testBooks.stream()
            .filter(book -> book.getGenre() == genre)
            .toList();

        when(bookCatalogService.findBooksByGenre(genre)).thenReturn(fictionBooks);
        when(bookMapper.toResponse(any(Book.class))).thenReturn(testBookResponse);

        // When
        ResponseEntity<List<BookResponse>> response = bookCatalogController.getBooksByGenre(genre);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(fictionBooks.size());

        verify(bookCatalogService).findBooksByGenre(genre);
        verify(bookMapper, times(fictionBooks.size())).toResponse(any(Book.class));
    }

    // ========== Get Available Books Tests ==========

    @Test
    @DisplayName("getAvailableBooks_shouldReturnAvailableBooks_whenCalled")
    void getAvailableBooks_shouldReturnAvailableBooks_whenCalled() {
        // Given
        List<Book> availableBooks = testBooks.stream()
            .filter(book -> book.getAvailableCopies() > 0)
            .toList();

        when(bookCatalogService.findAvailableBooks()).thenReturn(availableBooks);
        when(bookMapper.toResponse(any(Book.class))).thenReturn(testBookResponse);

        // When
        ResponseEntity<List<BookResponse>> response = bookCatalogController.getAvailableBooks();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(availableBooks.size());

        verify(bookCatalogService).findAvailableBooks();
        verify(bookMapper, times(availableBooks.size())).toResponse(any(Book.class));
    }

    // ========== Get Books by Title Tests ==========

    @Test
    @DisplayName("getBooksByTitle_shouldReturnBooksWithTitle_whenTitleProvided")
    void getBooksByTitle_shouldReturnBooksWithTitle_whenTitleProvided() {
        // Given
        String title = "Java";
        List<Book> titleBooks = testBooks.stream()
            .filter(book -> book.getTitle().contains(title))
            .toList();

        when(bookCatalogService.findBooksByTitle(title)).thenReturn(titleBooks);
        when(bookMapper.toResponse(any(Book.class))).thenReturn(testBookResponse);

        // When
        ResponseEntity<List<BookResponse>> response = bookCatalogController.getBooksByTitle(title);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(titleBooks.size());

        verify(bookCatalogService).findBooksByTitle(title);
        verify(bookMapper, times(titleBooks.size())).toResponse(any(Book.class));
    }

    // ========== Get Books by Author Tests ==========

    @Test
    @DisplayName("getBooksByAuthor_shouldReturnBooksByAuthor_whenAuthorProvided")
    void getBooksByAuthor_shouldReturnBooksByAuthor_whenAuthorProvided() {
        // Given
        String author = "Joshua";
        List<Book> authorBooks = testBooks.stream()
            .filter(book -> book.getAuthor().contains(author))
            .toList();

        when(bookCatalogService.findBooksByAuthor(author)).thenReturn(authorBooks);
        when(bookMapper.toResponse(any(Book.class))).thenReturn(testBookResponse);

        // When
        ResponseEntity<List<BookResponse>> response = bookCatalogController.getBooksByAuthor(author);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(authorBooks.size());

        verify(bookCatalogService).findBooksByAuthor(author);
        verify(bookMapper, times(authorBooks.size())).toResponse(any(Book.class));
    }

    // ========== Get Available Genres Tests ==========

    @Test
    @DisplayName("getAvailableGenres_shouldReturnAllGenres_whenCalled")
    void getAvailableGenres_shouldReturnAllGenres_whenCalled() {
        // When
        ResponseEntity<BookGenre[]> response = bookCatalogController.getAvailableGenres();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEqualTo(BookGenre.values());
    }

    // ========== Edge Cases and Error Scenarios ==========

    @Test
    @DisplayName("getBooks_shouldHandleEmptyResults_whenNoBooksFound")
    void getBooks_shouldHandleEmptyResults_whenNoBooksFound() {
        // Given
        Page<Book> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 20), 0);

        when(bookCatalogService.searchBooks(anyString(), any(), anyBoolean(), any(Pageable.class))).thenReturn(emptyPage);

        // When
        ResponseEntity<Page<BookResponse>> response = bookCatalogController.getBooks(0, 20, "title", "asc", "nonexistent", null, false);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();
        assertThat(response.getBody().getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("createBook_shouldHandleServiceException_whenServiceThrowsException")
    void createBook_shouldHandleServiceException_whenServiceThrowsException() {
        // Given
        when(bookMapper.toEntity(testBookCreationRequest)).thenReturn(testBook);
        when(bookCatalogService.createBook(testBook)).thenThrow(new RuntimeException("Service error"));

        // When & Then
        try {
            bookCatalogController.createBook(testBookCreationRequest);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Service error");
        }

        verify(bookMapper).toEntity(testBookCreationRequest);
        verify(bookCatalogService).createBook(testBook);
        verify(bookMapper, never()).toResponse(any(Book.class));
    }

    // ========== Integration with Dependencies ==========

    @Test
    @DisplayName("createBook_shouldUseAllDependenciesCorrectly_whenProcessingRequest")
    void createBook_shouldUseAllDependenciesCorrectly_whenProcessingRequest() {
        // Given
        when(bookMapper.toEntity(testBookCreationRequest)).thenReturn(testBook);
        when(bookCatalogService.createBook(testBook)).thenReturn(testBook);
        when(bookMapper.toResponse(testBook)).thenReturn(testBookResponse);

        // When
        bookCatalogController.createBook(testBookCreationRequest);

        // Then
        verify(bookMapper).toEntity(testBookCreationRequest);
        verify(bookCatalogService).createBook(testBook);
        verify(bookMapper).toResponse(testBook);
        verifyNoMoreInteractions(bookMapper, bookCatalogService);
    }

    @Test
    @DisplayName("getBooks_shouldUseAllDependenciesCorrectly_whenProcessingPaginationRequest")
    void getBooks_shouldUseAllDependenciesCorrectly_whenProcessingPaginationRequest() {
        // Given
        Page<Book> bookPage = new PageImpl<>(testBooks, PageRequest.of(0, 20), testBooks.size());
        when(bookCatalogService.searchBooks(any(), any(), anyBoolean(), any(Pageable.class))).thenReturn(bookPage);
        when(bookMapper.toResponse(any(Book.class))).thenReturn(testBookResponse);

        // When
        bookCatalogController.getBooks(0, 20, "title", "asc", null, null, false);

        // Then
        verify(bookCatalogService).searchBooks(any(), any(), anyBoolean(), any(Pageable.class));
        verify(bookMapper, times(testBooks.size())).toResponse(any(Book.class));
        verifyNoMoreInteractions(bookCatalogService, bookMapper);
    }
}
