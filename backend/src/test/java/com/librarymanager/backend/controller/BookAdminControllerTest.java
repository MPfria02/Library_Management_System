package com.librarymanager.backend.controller;

import com.librarymanager.backend.dto.request.BookCreationRequest;
import com.librarymanager.backend.dto.response.BookAdminResponse;
import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BookGenre;
import com.librarymanager.backend.exception.ResourceNotFoundException;
import com.librarymanager.backend.mapper.BookMapper;
import com.librarymanager.backend.service.BookCatalogService;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookAdminController
 * Covers CRUD, pagination, mapping, and error handling
 * @author Marcel Pulido
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookAdminController Unit Tests")
class BookAdminControllerTest {
    @Mock private BookCatalogService bookCatalogService;
    @Mock private BookMapper bookMapper;
    @InjectMocks private BookAdminController bookAdminController;

    private Book testBook;
    private BookAdminResponse testBookAdminResponse;
    private BookCreationRequest testBookCreationRequest;

    @BeforeEach
    void setUp() {
        testBook = new Book();
        testBook.setId(1L);
        testBook.setIsbn("111-TEST-ISBN");
        testBook.setTitle("Test Book");
        testBook.setAuthor("Test Author");
        testBook.setDescription("A book for testing.");
        testBook.setGenre(BookGenre.TECHNOLOGY);
        testBook.setTotalCopies(5);
        testBook.setAvailableCopies(3);
        testBook.setPublicationDate(LocalDate.of(2020, 1, 1));
        // not setting createdAt/updatedAt for simplicity

        testBookAdminResponse = BookAdminResponse.builder()
            .id(1L)
            .isbn("111-TEST-ISBN")
            .title("Test Book")
            .author("Test Author")
            .description("A book for testing.")
            .genre(BookGenre.TECHNOLOGY)
            .totalCopies(5)
            .availableCopies(3)
            .publicationDate(LocalDate.of(2020, 1, 1))
            .createdAt(LocalDateTime.of(2024,10,10,12,0))
            .updatedAt(LocalDateTime.of(2024,10,10,13,0))
            .build();

        testBookCreationRequest = new BookCreationRequest();
        testBookCreationRequest.setIsbn("111-TEST-ISBN");
        testBookCreationRequest.setTitle("Test Book");
        testBookCreationRequest.setAuthor("Test Author");
        testBookCreationRequest.setDescription("A book for testing.");
        testBookCreationRequest.setGenre(BookGenre.TECHNOLOGY);
        testBookCreationRequest.setTotalCopies(5);
        testBookCreationRequest.setAvailableCopies(3);
        testBookCreationRequest.setPublicationDate(LocalDate.of(2020, 1, 1));
    }

    @Test
    @DisplayName("createBook_shouldReturnCreatedBookAdminResponse_whenValidRequestProvided")
    void createBook_shouldReturnCreatedBookAdminResponse_whenValidRequestProvided() {
        // Given
        when(bookMapper.toEntity(testBookCreationRequest)).thenReturn(testBook);
        when(bookCatalogService.createBook(testBook)).thenReturn(testBook);
        when(bookMapper.toAdminResponse(testBook)).thenReturn(testBookAdminResponse);
        
        // When
        ResponseEntity<BookAdminResponse> response = bookAdminController.createBook(testBookCreationRequest);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(testBookAdminResponse);
        verify(bookMapper).toEntity(testBookCreationRequest);
        verify(bookCatalogService).createBook(testBook);
        verify(bookMapper).toAdminResponse(testBook);
    }

    @Test
    @DisplayName("getBookById_shouldReturnBookAdminResponse_whenValidIdProvided")
    void getBookById_shouldReturnBookAdminResponse_whenValidIdProvided() {
        // Given
        when(bookCatalogService.findById(1L)).thenReturn(testBook);
        when(bookMapper.toAdminResponse(testBook)).thenReturn(testBookAdminResponse);
        
        // When
        ResponseEntity<BookAdminResponse> response = bookAdminController.getBookById(1L);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(testBookAdminResponse);
        verify(bookCatalogService).findById(1L);
        verify(bookMapper).toAdminResponse(testBook);
    }

    @Test
    @DisplayName("getAllBooks_shouldReturnPaginatedBookAdminResponses_whenDefaultParametersProvided")
    void getAllBooks_shouldReturnPaginatedBookAdminResponses_whenDefaultParametersProvided() {
        // Given
        Page<Book> page = new PageImpl<>(List.of(testBook), PageRequest.of(0, 30), 1);
        when(bookCatalogService.searchBooks(any(), any(), anyBoolean(), any(Pageable.class))).thenReturn(page);
        when(bookMapper.toAdminResponse(testBook)).thenReturn(testBookAdminResponse);
        
        // When
        ResponseEntity<Page<BookAdminResponse>> response = bookAdminController.getAllBooks(0, 30, "title", "asc", null, null, false);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).containsExactly(testBookAdminResponse);
        verify(bookCatalogService).searchBooks(any(), any(), anyBoolean(), any(Pageable.class));
    }

    @Test
    @DisplayName("updateBook_shouldReturnUpdatedBookAdminResponse_whenValidRequestProvided")
    void updateBook_shouldReturnUpdatedBookAdminResponse_whenValidRequestProvided() {
        // Given
        when(bookMapper.toEntity(testBookCreationRequest)).thenReturn(testBook);
        when(bookCatalogService.updateBook(any(Book.class))).thenReturn(testBook);
        when(bookMapper.toAdminResponse(testBook)).thenReturn(testBookAdminResponse);
        
        // When
        ResponseEntity<BookAdminResponse> response = bookAdminController.updateBook(1L, testBookCreationRequest);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(testBookAdminResponse);
        verify(bookMapper).toEntity(testBookCreationRequest);
        verify(bookCatalogService).updateBook(argThat(b -> 1L == b.getId()));
        verify(bookMapper).toAdminResponse(testBook);
    }

    @Test
    @DisplayName("deleteBook_shouldReturnNoContent_whenValidIdProvided")
    void deleteBook_shouldReturnNoContent_whenValidIdProvided() {
        // Given
        doNothing().when(bookCatalogService).deleteBook(1L);
        
        // When
        ResponseEntity<Void> response = bookAdminController.deleteBook(1L);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(bookCatalogService).deleteBook(1L);
    }

    @Test
    @DisplayName("getBookById_shouldThrowException_whenServiceThrowsException")
    void getBookById_shouldThrowException_whenServiceThrowsException() {
        // Given
        when(bookCatalogService.findById(1L)).thenThrow(new ResourceNotFoundException("Not found"));
        
        // When & Then
        assertThatThrownBy(() -> bookAdminController.getBookById(1L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Not found");
        
        // try {
        //     bookAdminController.getBookById(1L);
        // } catch (RuntimeException e) {
        //     assertThat(e.getMessage()).isEqualTo("Not found");
        // }
        
        verify(bookCatalogService).findById(1L);
    }

    @Test
    @DisplayName("getAllBooks_shouldHandleEmptyResults_whenNoBooksFound")
    void getAllBooks_shouldHandleEmptyResults_whenNoBooksFound() {
        // Given
        Page<Book> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 30), 0);
        when(bookCatalogService.searchBooks(any(), any(), anyBoolean(), any(Pageable.class))).thenReturn(emptyPage);
        
        // When
        ResponseEntity<Page<BookAdminResponse>> response = bookAdminController.getAllBooks(0, 30, "title", "asc", null, null, false);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).isEmpty();
        assertThat(response.getBody().getTotalElements()).isEqualTo(0);
        verify(bookCatalogService).searchBooks(any(), any(), anyBoolean(), any(Pageable.class));
    }
}
