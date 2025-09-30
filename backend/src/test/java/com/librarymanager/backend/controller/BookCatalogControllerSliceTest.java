package com.librarymanager.backend.controller;

import com.librarymanager.backend.dto.request.BookCreationRequest;
import com.librarymanager.backend.dto.response.BookResponse;
import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BookGenre;
import com.librarymanager.backend.exception.BusinessRuleViolationException;
import com.librarymanager.backend.exception.DuplicateResourceException;
import com.librarymanager.backend.exception.ResourceNotFoundException;
import com.librarymanager.backend.mapper.BookMapper;
import com.librarymanager.backend.security.JwtAuthenticationFilter;
import com.librarymanager.backend.security.JwtTokenService;
import com.librarymanager.backend.service.BookCatalogService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for BookCatalogController using @WebMvcTest.
 * 
 * Tests the web layer in isolation, focusing on HTTP behavior,
 * request/response mapping, and exception handling for all book catalog operations.
 * 
 * @author Marcel Pulido
 */
@WebMvcTest(BookCatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
public class BookCatalogControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookCatalogService bookCatalogService;

    @MockitoBean
    private BookMapper bookMapper;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    // ========== CRUD Operations Tests ==========

    @Test
    public void shouldReturn201WhenValidBookIsCreated() throws Exception {
        // Arrange
        BookCreationRequest request = new BookCreationRequest(
            "978-0-123456-78-9", "The Great Book", "John Author", 
            "A great book description", BookGenre.FICTION, 5, LocalDate.of(2023, 1, 1)
        );
        Book createdBook = new Book();
        createdBook.setId(1L);
        createdBook.setIsbn("978-0-123456-78-9");
        createdBook.setTitle("The Great Book");
        createdBook.setAuthor("John Author");
        createdBook.setDescription("A great book description");
        createdBook.setGenre(BookGenre.FICTION);
        createdBook.setTotalCopies(5);
        createdBook.setAvailableCopies(5);
        createdBook.setPublicationDate(LocalDate.of(2023, 1, 1));
        BookResponse response = BookResponse.builder()
            .id(1L)
            .title("The Great Book")
            .author("John Author")
            .description("A great book description")
            .genre(BookGenre.FICTION)
            .availableCopies(5)
            .publicationDate(LocalDate.of(2023, 1, 1))
            .build();
        given(bookMapper.toEntity(any(BookCreationRequest.class))).willReturn(createdBook);
        given(bookCatalogService.createBook(any(Book.class))).willReturn(createdBook);
        given(bookMapper.toResponse(any(Book.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("The Great Book"))
                .andExpect(jsonPath("$.author").value("John Author"))
                .andExpect(jsonPath("$.description").value("A great book description"))
                .andExpect(jsonPath("$.genre").value("FICTION"))
                .andExpect(jsonPath("$.availableCopies").value(5))
                .andExpect(jsonPath("$.publicationDate").value("2023-01-01"));
    }

    @Test
    public void shouldReturn409WhenDuplicateIsbnIsProvided() throws Exception {
        // Arrange
        BookCreationRequest request = new BookCreationRequest(
            "978-0-123456-78-9", "The Great Book", "John Author", 
            "A great book description", BookGenre.FICTION, 5, LocalDate.of(2023, 1, 1)
        );
        Book book = new Book();
        given(bookMapper.toEntity(any(BookCreationRequest.class))).willReturn(book);
        willThrow(new DuplicateResourceException("ISBN already exists", "DUPLICATE_ISBN"))
                .given(bookCatalogService).createBook(any(Book.class));

        // Act & Assert
        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("ISBN already exists"))
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));
    }

    @Test
    public void shouldReturn400WhenInvalidBookDataIsProvided() throws Exception {
        // Arrange
        BookCreationRequest request = new BookCreationRequest(
            "", "", "", 
            "", null, 0, null
        );
        // No need to mock service/mapper, validation should fail before service is called

        // Act & Assert
        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    public void shouldReturn200WhenBookIsFoundById() throws Exception {
        // Arrange
        Book book = new Book();
        book.setId(1L);
        book.setTitle("The Great Book");
        book.setAuthor("John Author");
        book.setGenre(BookGenre.FICTION);
        book.setAvailableCopies(3);
        book.setPublicationDate(LocalDate.of(2023, 1, 1));
        BookResponse response = BookResponse.builder()
            .id(1L)
            .title("The Great Book")
            .author("John Author")
            .genre(BookGenre.FICTION)
            .availableCopies(3)
            .publicationDate(LocalDate.of(2023, 1, 1))
            .build();
        given(bookCatalogService.findById(1L)).willReturn(book);
        given(bookMapper.toResponse(any(Book.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("The Great Book"))
                .andExpect(jsonPath("$.author").value("John Author"))
                .andExpect(jsonPath("$.genre").value("FICTION"))
                .andExpect(jsonPath("$.availableCopies").value(3));
    }

    @Test
    public void shouldReturn404WhenBookNotFoundById() throws Exception {
        // Arrange
        willThrow(new ResourceNotFoundException("Book not found with ID: 999", "BOOK_NOT_FOUND"))
                .given(bookCatalogService).findById(999L);

        // Act & Assert
        mockMvc.perform(get("/api/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Book not found with ID: 999"))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    public void shouldReturn200WhenBookIsFoundByIsbn() throws Exception {
        // Arrange
        Book book = new Book();
        book.setId(1L);
        book.setIsbn("978-0-123456-78-9");
        book.setTitle("The Great Book");
        book.setAuthor("John Author");
        book.setGenre(BookGenre.FICTION);
        book.setAvailableCopies(3);
        BookResponse response = BookResponse.builder()
            .id(1L)
            .title("The Great Book")
            .author("John Author")
            .genre(BookGenre.FICTION)
            .availableCopies(3)
            .build();
        given(bookCatalogService.findByIsbn("978-0-123456-78-9")).willReturn(book);
        given(bookMapper.toResponse(any(Book.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/books/isbn/978-0-123456-78-9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("The Great Book"))
                .andExpect(jsonPath("$.author").value("John Author"));
    }

    @Test
    public void shouldReturn404WhenBookNotFoundByIsbn() throws Exception {
        // Arrange
        willThrow(new ResourceNotFoundException("Book not found with ISBN: invalid-isbn", "BOOK_NOT_FOUND"))
                .given(bookCatalogService).findByIsbn("invalid-isbn");

        // Act & Assert
        mockMvc.perform(get("/api/books/isbn/invalid-isbn"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Book not found with ISBN: invalid-isbn"))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    public void shouldReturn200WhenBookIsUpdated() throws Exception {
        // Arrange
        BookCreationRequest request = new BookCreationRequest(
            "978-0-123456-78-9", "Updated Book Title", "Updated Author", 
            "Updated description", BookGenre.SCIENCE, 3, LocalDate.of(2023, 2, 1)
        );
        Book updatedBook = new Book();
        updatedBook.setId(1L);
        updatedBook.setTitle("Updated Book Title");
        updatedBook.setAuthor("Updated Author");
        updatedBook.setGenre(BookGenre.SCIENCE);
        updatedBook.setAvailableCopies(2);
        BookResponse response = BookResponse.builder()
            .id(1L)
            .title("Updated Book Title")
            .author("Updated Author")
            .genre(BookGenre.SCIENCE)
            .availableCopies(2)
            .build();
        given(bookMapper.toEntity(any(BookCreationRequest.class))).willReturn(updatedBook);
        given(bookCatalogService.updateBook(any(Book.class))).willReturn(updatedBook);
        given(bookMapper.toResponse(any(Book.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/books/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Updated Book Title"))
                .andExpect(jsonPath("$.author").value("Updated Author"))
                .andExpect(jsonPath("$.genre").value("SCIENCE"));
    }

    @Test
    public void shouldReturn404WhenUpdatingNonExistentBook() throws Exception {
        // Arrange
        BookCreationRequest request = new BookCreationRequest(
            "978-0-123456-78-9", "Updated Book Title", "Updated Author", 
            "Updated description", BookGenre.SCIENCE, 3, LocalDate.of(2023, 2, 1)
        );
        Book book = new Book();
        book.setId(999L);
        given(bookMapper.toEntity(any(BookCreationRequest.class))).willReturn(book);
        willThrow(new ResourceNotFoundException("Book not found with ID: 999", "BOOK_NOT_FOUND"))
                .given(bookCatalogService).updateBook(any(Book.class));

        // Act & Assert
        mockMvc.perform(put("/api/books/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Book not found with ID: 999"))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    public void shouldReturn204WhenBookIsDeleted() throws Exception {
        // Arrange
        // deleteBook returns void, so we don't need to mock a return value

        // Act & Assert
        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void shouldReturn404WhenDeletingNonExistentBook() throws Exception {
        // Arrange
        willThrow(new ResourceNotFoundException("Book not found with ID: 999", "BOOK_NOT_FOUND"))
                .given(bookCatalogService).deleteBook(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Book not found with ID: 999"))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    public void shouldReturn422WhenDeletingBookWithBorrowedCopies() throws Exception {
        // Arrange
        willThrow(new BusinessRuleViolationException("Cannot delete book with borrowed copies", "BOOK_HAS_BORROWED_COPIES"))
                .given(bookCatalogService).deleteBook(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value("Cannot delete book with borrowed copies"))
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }

    // ========== Search and Filtering Tests ==========

    @Test
    public void shouldReturn200WhenGettingBooksWithPagination() throws Exception {
        // Arrange
        Book book1 = new Book();
        book1.setId(1L);
        book1.setTitle("Book 1");
        book1.setAuthor("Author 1");
        book1.setGenre(BookGenre.FICTION);
        book1.setAvailableCopies(2);
        Book book2 = new Book();
        book2.setId(2L);
        book2.setTitle("Book 2");
        book2.setAuthor("Author 2");
        book2.setGenre(BookGenre.SCIENCE);
        book2.setAvailableCopies(1);
        List<Book> books = Arrays.asList(book1, book2);
        Page<Book> bookPage = new PageImpl<>(books, PageRequest.of(0, 20), 2);
        BookResponse response1 = BookResponse.builder()
            .id(1L)
            .title("Book 1")
            .author("Author 1")
            .genre(BookGenre.FICTION)
            .availableCopies(2)
            .build();
        BookResponse response2 = BookResponse.builder()
            .id(2L)
            .title("Book 2")
            .author("Author 2")
            .genre(BookGenre.SCIENCE)
            .availableCopies(1)
            .build();
        given(bookCatalogService.searchBooks(nullable(String.class), any(), anyBoolean(), any(Pageable.class)))
                .willReturn(bookPage);
        given(bookMapper.toResponse(book1)).willReturn(response1);
        given(bookMapper.toResponse(book2)).willReturn(response2);

        // Act & Assert
        mockMvc.perform(get("/api/books/search")
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "title")
                .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Book 1"))
                .andExpect(jsonPath("$.content[1].title").value("Book 2"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    public void shouldReturn200WhenSearchingBooksWithFilters() throws Exception {
        // Arrange
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Science Book");
        book.setAuthor("Science Author");
        book.setGenre(BookGenre.SCIENCE);
        book.setAvailableCopies(1);
        List<Book> books = Arrays.asList(book);
        Page<Book> bookPage = new PageImpl<>(books, PageRequest.of(0, 20), 1);
        BookResponse response = BookResponse.builder()
            .id(1L)
            .title("Science Book")
            .author("Science Author")
            .genre(BookGenre.SCIENCE)
            .availableCopies(1)
            .build();
        given(bookCatalogService.searchBooks(eq("science"), eq(BookGenre.SCIENCE), eq(true), any(Pageable.class)))
                .willReturn(bookPage);
        given(bookMapper.toResponse(any(Book.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/books/search")
                .param("searchTerm", "science")
                .param("genre", "SCIENCE")
                .param("availableOnly", "true")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Science Book"))
                .andExpect(jsonPath("$.content[0].genre").value("SCIENCE"));
    }

    @Test
    public void shouldReturn200WhenPerformingFullTextSearch() throws Exception {
        // Arrange
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Machine Learning Guide");
        book.setAuthor("ML Expert");
        book.setGenre(BookGenre.SCIENCE);
        book.setAvailableCopies(2);
        List<Book> books = Arrays.asList(book);
        BookResponse response = BookResponse.builder()
            .id(1L)
            .title("Machine Learning Guide")
            .author("ML Expert")
            .genre(BookGenre.SCIENCE)
            .availableCopies(2)
            .build();
        given(bookCatalogService.fullTextSearch("machine learning")).willReturn(books);
        given(bookMapper.toResponse(any(Book.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/books/search/full-text")
                .param("searchTerm", "machine learning"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Machine Learning Guide"))
                .andExpect(jsonPath("$[0].author").value("ML Expert"));
    }

    @Test
    public void shouldReturn200WhenGettingBooksByGenre() throws Exception {
        // Arrange
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Fiction Book");
        book.setAuthor("Fiction Author");
        book.setGenre(BookGenre.FICTION);
        book.setAvailableCopies(3);
        List<Book> books = Arrays.asList(book);
        BookResponse response = BookResponse.builder()
            .id(1L)
            .title("Fiction Book")
            .author("Fiction Author")
            .genre(BookGenre.FICTION)
            .availableCopies(3)
            .build();
        given(bookCatalogService.findBooksByGenre(BookGenre.FICTION)).willReturn(books);
        given(bookMapper.toResponse(any(Book.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/books/genre/FICTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Fiction Book"))
                .andExpect(jsonPath("$[0].genre").value("FICTION"));
    }

    @Test
    public void shouldReturn200WhenGettingAvailableBooks() throws Exception {
        // Arrange
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Available Book");
        book.setAuthor("Available Author");
        book.setGenre(BookGenre.FICTION);
        book.setAvailableCopies(1);
        List<Book> books = Arrays.asList(book);
        BookResponse response = BookResponse.builder()
            .id(1L)
            .title("Available Book")
            .author("Available Author")
            .genre(BookGenre.FICTION)
            .availableCopies(1)
            .build();
        given(bookCatalogService.findAvailableBooks()).willReturn(books);
        given(bookMapper.toResponse(any(Book.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/books/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Available Book"))
                .andExpect(jsonPath("$[0].availableCopies").value(1));
    }

    @Test
    public void shouldReturn200WhenSearchingBooksByTitle() throws Exception {
        // Arrange
        Book book = new Book();
        book.setId(1L);
        book.setTitle("The Great Gatsby");
        book.setAuthor("F. Scott Fitzgerald");
        book.setGenre(BookGenre.FICTION);
        book.setAvailableCopies(2);
        List<Book> books = Arrays.asList(book);
        BookResponse response = BookResponse.builder()
            .id(1L)
            .title("The Great Gatsby")
            .author("F. Scott Fitzgerald")
            .genre(BookGenre.FICTION)
            .availableCopies(2)
            .build();
        given(bookCatalogService.findBooksByTitle("Gatsby")).willReturn(books);
        given(bookMapper.toResponse(any(Book.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/books/title/Gatsby"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("The Great Gatsby"))
                .andExpect(jsonPath("$[0].author").value("F. Scott Fitzgerald"));
    }

    @Test
    public void shouldReturn200WhenSearchingBooksByAuthor() throws Exception {
        // Arrange
        Book book = new Book();
        book.setId(1L);
        book.setTitle("1984");
        book.setAuthor("George Orwell");
        book.setGenre(BookGenre.FICTION);
        book.setAvailableCopies(1);
        List<Book> books = Arrays.asList(book);
        BookResponse response = BookResponse.builder()
            .id(1L)
            .title("1984")
            .author("George Orwell")
            .genre(BookGenre.FICTION)
            .availableCopies(1)
            .build();
        given(bookCatalogService.findBooksByAuthor("Orwell")).willReturn(books);
        given(bookMapper.toResponse(any(Book.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/books/author/Orwell"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("1984"))
                .andExpect(jsonPath("$[0].author").value("George Orwell"));
    }

    @Test
    public void shouldReturn200WhenGettingAvailableGenres() throws Exception {
        // No mocking needed, controller returns BookGenre.values()
        mockMvc.perform(get("/api/books/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(BookGenre.values().length))
                .andExpect(jsonPath("$[0]").value("FICTION"))
                .andExpect(jsonPath("$[1]").value("NON_FICTION"));
    }

    // ========== Edge Cases ==========

    @Test
    public void shouldReturn400WhenInvalidGenreIsProvided() throws Exception {
        // No mocking needed, Spring will throw MethodArgumentTypeMismatchException
        mockMvc.perform(get("/api/books/genre/INVALID_GENRE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
    }

    @Test
    public void shouldReturn400WhenInvalidPageParametersAreProvided() throws Exception {
        // No mocking needed, Spring will throw MethodArgumentTypeMismatchException
        mockMvc.perform(get("/api/books")
                .param("page", "invalid")
                .param("size", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
    }

    @Test
    public void shouldReturn500WhenInvalidSortDirectionIsProvided() throws Exception {
        // No mocking needed, Spring will throw IllegalArgumentException inside controller
        mockMvc.perform(get("/api/books")
                .param("sortDir", "invalid"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"));
    }

    @Test
    public void shouldReturn400WhenInvalidJsonIsProvidedForCreation() throws Exception {
        // No mocking needed, Spring will throw HttpMessageNotReadableException
        mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_JSON"));
    }

    @Test
    public void shouldReturn400WhenInvalidJsonIsProvidedForUpdate() throws Exception {
        // No mocking needed, Spring will throw HttpMessageNotReadableException
        mockMvc.perform(put("/api/books/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_JSON"));
    }
}
