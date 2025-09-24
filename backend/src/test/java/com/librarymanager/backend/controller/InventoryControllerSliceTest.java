package com.librarymanager.backend.controller;

import com.librarymanager.backend.dto.response.BookResponse;
import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BookGenre;
import com.librarymanager.backend.exception.BusinessRuleViolationException;
import com.librarymanager.backend.exception.ResourceNotFoundException;
import com.librarymanager.backend.mapper.BookMapper;
import com.librarymanager.backend.security.JwtAuthenticationFilter;
import com.librarymanager.backend.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for InventoryController using @WebMvcTest.
 * 
 * Tests the web layer in isolation, focusing on HTTP behavior,
 * request/response mapping, and exception handling for inventory operations.
 * 
 * @author Marcel Pulido
 */
@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
public class InventoryControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private BookMapper bookMapper;

    // ========== Borrow Book Tests ==========

    @Test
    public void shouldReturn200WhenBookIsSuccessfullyBorrowed() throws Exception {
        // Arrange
        Book borrowedBook = new Book();
        borrowedBook.setId(1L);
        borrowedBook.setTitle("The Great Book");
        borrowedBook.setAuthor("John Author");
        borrowedBook.setGenre(BookGenre.FICTION);
        borrowedBook.setAvailableCopies(2); // Reduced from 3 to 2 after borrowing
        borrowedBook.setPublicationDate(LocalDate.of(2023, 1, 1));
        
        BookResponse response = BookResponse.builder()
            .id(1L)
            .title("The Great Book")
            .author("John Author")
            .genre(BookGenre.FICTION)
            .availableCopies(2)
            .publicationDate(LocalDate.of(2023, 1, 1))
            .build();

        given(inventoryService.borrowBook(1L)).willReturn(borrowedBook);
        given(bookMapper.toResponse(any(Book.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/inventory/books/1/borrow")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("The Great Book"))
                .andExpect(jsonPath("$.author").value("John Author"))
                .andExpect(jsonPath("$.genre").value("FICTION"))
                .andExpect(jsonPath("$.availableCopies").value(2))
                .andExpect(jsonPath("$.publicationDate").value("2023-01-01"));
    }

    @Test
    public void shouldReturn404WhenBorrowingNonExistentBook() throws Exception {
        // Arrange
        willThrow(new ResourceNotFoundException("Book not found with ID: 999", "BOOK_NOT_FOUND"))
                .given(inventoryService).borrowBook(999L);

        // Act & Assert
        mockMvc.perform(post("/api/inventory/books/999/borrow")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Book not found with ID: 999"))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    public void shouldReturn422WhenBorrowingUnavailableBook() throws Exception {
        // Arrange
        willThrow(new BusinessRuleViolationException("Book is not available for borrowing", "BOOK_NOT_AVAILABLE"))
                .given(inventoryService).borrowBook(1L);

        // Act & Assert
        mockMvc.perform(post("/api/inventory/books/1/borrow")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Book is not available for borrowing"))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    public void shouldReturn422WhenBorrowingBookWithZeroAvailableCopies() throws Exception {
        // Arrange
        willThrow(new BusinessRuleViolationException("No copies available for borrowing", "NO_COPIES_AVAILABLE"))
                .given(inventoryService).borrowBook(1L);

        // Act & Assert
        mockMvc.perform(post("/api/inventory/books/1/borrow")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("No copies available for borrowing"))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    public void shouldReturn400WhenBorrowingWithInvalidBookId() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/inventory/books/invalid/borrow")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
    }

    // ========== Return Book Tests ==========

    @Test
    public void shouldReturn200WhenBookIsSuccessfullyReturned() throws Exception {
        // Arrange
        Book returnedBook = new Book();
        returnedBook.setId(1L);
        returnedBook.setTitle("The Great Book");
        returnedBook.setAuthor("John Author");
        returnedBook.setGenre(BookGenre.FICTION);
        returnedBook.setAvailableCopies(3); // Increased from 2 to 3 after returning
        returnedBook.setPublicationDate(LocalDate.of(2023, 1, 1));
        
        BookResponse response = BookResponse.builder()
            .id(1L)
            .title("The Great Book")
            .author("John Author")
            .genre(BookGenre.FICTION)
            .availableCopies(3)
            .publicationDate(LocalDate.of(2023, 1, 1))
            .build();

        given(inventoryService.returnBook(1L)).willReturn(returnedBook);
        given(bookMapper.toResponse(any(Book.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/inventory/books/1/return")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("The Great Book"))
                .andExpect(jsonPath("$.author").value("John Author"))
                .andExpect(jsonPath("$.genre").value("FICTION"))
                .andExpect(jsonPath("$.availableCopies").value(3))
                .andExpect(jsonPath("$.publicationDate").value("2023-01-01"));
    }

    @Test
    public void shouldReturn404WhenReturningNonExistentBook() throws Exception {
        // Arrange
        willThrow(new ResourceNotFoundException("Book not found with ID: 999", "BOOK_NOT_FOUND"))
                .given(inventoryService).returnBook(999L);

        // Act & Assert
        mockMvc.perform(post("/api/inventory/books/999/return")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Book not found with ID: 999"))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    public void shouldReturn422WhenReturningBookThatWasNotBorrowed() throws Exception {
        // Arrange
        willThrow(new BusinessRuleViolationException("Book was not borrowed and cannot be returned", "BOOK_NOT_BORROWED"))
                .given(inventoryService).returnBook(1L);

        // Act & Assert
        mockMvc.perform(post("/api/inventory/books/1/return")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Book was not borrowed and cannot be returned"))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    public void shouldReturn422WhenReturningBookWithAllCopiesAvailable() throws Exception {
        // Arrange
        willThrow(new BusinessRuleViolationException("All copies are already available", "ALL_COPIES_AVAILABLE"))
                .given(inventoryService).returnBook(1L);

        // Act & Assert
        mockMvc.perform(post("/api/inventory/books/1/return")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("All copies are already available"))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    public void shouldReturn400WhenReturningWithInvalidBookId() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/inventory/books/invalid/return")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
    }

    // ========== Edge Cases ==========

    @Test
    public void shouldReturn200WhenBorrowingBookWithSingleAvailableCopy() throws Exception {
        // Arrange
        Book borrowedBook = new Book();
        borrowedBook.setId(1L);
        borrowedBook.setTitle("Rare Book");
        borrowedBook.setAuthor("Rare Author");
        borrowedBook.setGenre(BookGenre.SCIENCE);
        borrowedBook.setAvailableCopies(0); // All copies borrowed
        borrowedBook.setPublicationDate(LocalDate.of(2023, 1, 1));
        
        BookResponse response = BookResponse.builder()
            .id(1L)
            .title("Rare Book")
            .author("Rare Author")
            .genre(BookGenre.SCIENCE)
            .availableCopies(0)
            .publicationDate(LocalDate.of(2023, 1, 1))
            .build();

        given(inventoryService.borrowBook(1L)).willReturn(borrowedBook);
        given(bookMapper.toResponse(any(Book.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/inventory/books/1/borrow")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Rare Book"))
                .andExpect(jsonPath("$.availableCopies").value(0));
    }

    @Test
    public void shouldReturn200WhenReturningBookToFullAvailability() throws Exception {
        // Arrange
        Book returnedBook = new Book();
        returnedBook.setId(1L);
        returnedBook.setTitle("Popular Book");
        returnedBook.setAuthor("Popular Author");
        returnedBook.setGenre(BookGenre.FICTION);
        returnedBook.setAvailableCopies(5); // All copies now available
        returnedBook.setPublicationDate(LocalDate.of(2023, 1, 1));
        
        BookResponse response = BookResponse.builder()
            .id(1L)
            .title("Popular Book")
            .author("Popular Author")
            .genre(BookGenre.FICTION)
            .availableCopies(5)
            .publicationDate(LocalDate.of(2023, 1, 1))
            .build();

        given(inventoryService.returnBook(1L)).willReturn(returnedBook);
        given(bookMapper.toResponse(any(Book.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/inventory/books/1/return")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Popular Book"))
                .andExpect(jsonPath("$.availableCopies").value(5));
    }

    @Test
    public void shouldReturn422WhenReturningBookExceedsTotalCopies() throws Exception {
        // Arrange
        willThrow(new BusinessRuleViolationException("Cannot return more copies than total copies", "EXCEEDS_TOTAL_COPIES"))
                .given(inventoryService).returnBook(1L);

        // Act & Assert
        mockMvc.perform(post("/api/inventory/books/1/return")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Cannot return more copies than total copies"))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    public void shouldReturn500WhenUnexpectedErrorOccursDuringBorrow() throws Exception {
        // Arrange
        willThrow(new RuntimeException("Unexpected database error"))
                .given(inventoryService).borrowBook(1L);

        // Act & Assert
        mockMvc.perform(post("/api/inventory/books/1/borrow")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"));
    }

    @Test
    public void shouldReturn500WhenUnexpectedErrorOccursDuringReturn() throws Exception {
        // Arrange
        willThrow(new RuntimeException("Unexpected database error"))
                .given(inventoryService).returnBook(1L);

        // Act & Assert
        mockMvc.perform(post("/api/inventory/books/1/return")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"));
    }
}
