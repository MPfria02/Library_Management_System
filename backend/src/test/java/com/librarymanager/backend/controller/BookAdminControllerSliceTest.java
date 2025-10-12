package com.librarymanager.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarymanager.backend.dto.request.BookCreationRequest;
import com.librarymanager.backend.dto.response.BookAdminResponse;
import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BookGenre;
import com.librarymanager.backend.exception.DuplicateResourceException;
import com.librarymanager.backend.exception.ResourceNotFoundException;
import com.librarymanager.backend.mapper.BookMapper;
import com.librarymanager.backend.security.JwtAuthenticationFilter;
import com.librarymanager.backend.security.JwtTokenService;
import com.librarymanager.backend.service.BookCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
public class BookAdminControllerSliceTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private BookCatalogService bookCatalogService;
    @MockitoBean private BookMapper bookMapper;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private JwtTokenService jwtTokenService;

    private BookAdminResponse makeResponse() {
        return BookAdminResponse.builder()
                .id(1L)
                .isbn("TEST-ADMIN-ISB")
                .title("Slice Admin Book")
                .author("Author Admin")
                .description("Admin book")
                .genre(BookGenre.TECHNOLOGY)
                .totalCopies(8)
                .availableCopies(2)
                .publicationDate(LocalDate.of(2022,10,10))
                .createdAt(LocalDateTime.of(2024,10,10,1,0))
                .updatedAt(LocalDateTime.of(2024,10,10,2,0))
                .build();
    }

    @Test
    public void shouldReturn201WhenValidBookIsCreatedByAdmin() throws Exception {
        BookCreationRequest req = new BookCreationRequest();
        req.setIsbn("TEST-ADMIN-ISB");
        req.setTitle("Slice Admin Book");
        req.setAuthor("Author Admin");
        req.setDescription("Admin book");
        req.setGenre(BookGenre.TECHNOLOGY);
        req.setTotalCopies(8);
        req.setAvailableCopies(2);
        req.setPublicationDate(LocalDate.of(2022,10,10));
        Book book = new Book();
        given(bookMapper.toEntity(any(BookCreationRequest.class))).willReturn(book);
        given(bookCatalogService.createBook(any(Book.class))).willReturn(book);
        given(bookMapper.toAdminResponse(any(Book.class))).willReturn(makeResponse());
        
        mockMvc.perform(post("/api/admin/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.isbn").value("TEST-ADMIN-ISB"))
                .andExpect(jsonPath("$.totalCopies").value(8));
    }

    @Test
    public void shouldReturn409WhenDuplicateIsbnIsProvided() throws Exception {
        BookCreationRequest req = new BookCreationRequest("1234567890123", "Admin Only", "A Admin", "D", BookGenre.FICTION, 2, LocalDate.now());
        req.setIsbn("CONFLICT-ISB");
        req.setTitle("Admin");
        given(bookMapper.toEntity(any(BookCreationRequest.class))).willReturn(new Book());
        willThrow(new DuplicateResourceException("ISBN already exists")).given(bookCatalogService).createBook(any(Book.class));
        
        mockMvc.perform(post("/api/admin/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("ISBN already exists"))
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));
    }

    @Test
    public void shouldReturn400WhenInvalidBookDataIsProvided() throws Exception {
        BookCreationRequest req = new BookCreationRequest();
        
        mockMvc.perform(post("/api/admin/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    public void shouldReturn200WhenBookIsFoundById() throws Exception {
        Book book = new Book();
        given(bookCatalogService.findById(1L)).willReturn(book);
        given(bookMapper.toAdminResponse(any(Book.class))).willReturn(makeResponse());
        
        mockMvc.perform(get("/api/admin/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("TEST-ADMIN-ISB"))
                .andExpect(jsonPath("$.totalCopies").value(8));
    }

    @Test
    public void shouldReturn404WhenBookNotFoundById() throws Exception {
        // Arrange
        willThrow(new ResourceNotFoundException("Book not found with ID: 999", "BOOK_NOT_FOUND"))
                .given(bookCatalogService).findById(999L);

        // Act & Assert
        mockMvc.perform(get("/api/admin/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Book not found with ID: 999"))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    public void shouldReturn200WhenGettingBooksWithPagination() throws Exception {
        
        BookAdminResponse resp = makeResponse();
        Page<Book> page = new PageImpl<>(List.of(new Book()), PageRequest.of(0, 30), 1);
        given(bookCatalogService.searchBooks(any(), any(), anyBoolean(), any(Pageable.class))).willReturn(page);
        given(bookMapper.toAdminResponse(any(Book.class))).willReturn(resp);
        
        mockMvc.perform(get("/api/admin/books")
                .param("page", "0")
                .param("size", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    public void shouldReturn200WhenSearchingBooksWithFilters() throws Exception {
        
        BookAdminResponse resp = makeResponse();
        Page<Book> page = new PageImpl<>(List.of(new Book()), PageRequest.of(0, 30), 1);
        given(bookCatalogService.searchBooks(eq("testsearch"), eq(BookGenre.TECHNOLOGY), eq(true), any(Pageable.class)))
            .willReturn(page);
        given(bookMapper.toAdminResponse(any(Book.class))).willReturn(resp);
        
        mockMvc.perform(get("/api/admin/books")
                .param("searchTerm", "testsearch")
                .param("genre", "TECHNOLOGY")
                .param("availableOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    public void shouldReturn200WhenBookIsUpdated() throws Exception {
        
        BookCreationRequest req = new BookCreationRequest("1234567890123", "Admin Only", "A Admin", "D", BookGenre.FICTION, 2, LocalDate.now());
        given(bookMapper.toEntity(any(BookCreationRequest.class))).willReturn(new Book());
        given(bookCatalogService.updateBook(any(Book.class))).willReturn(new Book());
        given(bookMapper.toAdminResponse(any(Book.class))).willReturn(makeResponse());
        
        mockMvc.perform(put("/api/admin/books/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    public void shouldReturn404WhenUpdatingNonExistentBook() throws Exception {
        
        BookCreationRequest req = new BookCreationRequest("1234567890123", "Admin Only", "A Admin", "D", BookGenre.FICTION, 2, LocalDate.now());
        given(bookMapper.toEntity(any(BookCreationRequest.class))).willReturn(new Book());
        willThrow(new ResourceNotFoundException("Not found")).given(bookCatalogService).updateBook(any(Book.class));
        
        mockMvc.perform(put("/api/admin/books/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    public void shouldReturn204WhenBookIsDeleted() throws Exception {
        mockMvc.perform(delete("/api/admin/books/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void shouldReturn404WhenDeletingNonExistentBook() throws Exception {
        willThrow(new ResourceNotFoundException("Not found")).given(bookCatalogService).deleteBook(999L);
        mockMvc.perform(delete("/api/admin/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    public void shouldReturn400WhenInvalidJsonIsProvidedForCreation() throws Exception {
        mockMvc.perform(post("/api/admin/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
    }
}
