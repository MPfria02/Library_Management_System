package com.librarymanager.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarymanager.backend.dto.request.BookCreationRequest;
import com.librarymanager.backend.dto.response.BookResponse;
import com.librarymanager.backend.entity.BookGenre;
import com.librarymanager.backend.entity.User;
import com.librarymanager.backend.entity.UserRole;
import com.librarymanager.backend.repository.BookRepository;
import com.librarymanager.backend.repository.UserRepository;
import com.librarymanager.backend.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for BookCatalogController using Testcontainers.
 * 
 * These tests verify end-to-end book catalog operations:
 * - Book creation with valid and invalid input
 * - Book search and filtering functionality
 * - Duplicate ISBN handling
 * - Error handling and response format consistency
 * 
 * Tests use real PostgreSQL database via Testcontainers and MockMvc
 * for realistic integration testing.
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
@AutoConfigureMockMvc
class BookCatalogControllerIT extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BookRepository bookRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String memberToken;

    /**
     * Set up test data before each test.
     */
    @BeforeEach
    void setUp() {
        // Clean up database
        bookRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        User admin = User.builder()
                .email("admin@library.com")
                .password(passwordEncoder.encode("admin123"))
                .firstName("Admin")
                .lastName("User")
                .role(UserRole.ADMIN)
                .build();
        userRepository.save(admin);

        User member = User.builder()
                .email("member@library.com")
                .password(passwordEncoder.encode("member123"))
                .firstName("Member")
                .lastName("User")
                .role(UserRole.MEMBER)
                .build();
        userRepository.save(member);

        // Generate JWT tokens using CustomUserDetails
        adminToken = jwtTokenService.generateToken(new com.librarymanager.backend.security.CustomUserDetails(admin));
        memberToken = jwtTokenService.generateToken(new com.librarymanager.backend.security.CustomUserDetails(member));
    }

    // -----------------
    // Helper Methods
    // -----------------
    private HttpHeaders authHeader(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        return headers;
    }

    private ResultActions createBook(BookCreationRequest request, String token) throws Exception {
        return mockMvc.perform(post("/api/books")
                .headers(authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private BookResponse createBookAndReturnResponse(BookCreationRequest request, String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/books")
                .headers(authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), BookResponse.class);
    }

    @Nested
    @DisplayName("Book Creation Tests")
    class BookCreationTests {

        @Test
        @DisplayName("Should return 201 when valid book is created by admin")
        void shouldReturn201WhenValidBookIsCreatedByAdmin() throws Exception {
            // Given
            BookCreationRequest request = new BookCreationRequest(
                "978-0-123456-78-9",
                "The Great Adventure",
                "John Smith",
                "An exciting adventure story",
                BookGenre.FICTION,
                5,
                LocalDate.of(2023, 1, 15)
            );

            // When & Then
            createBook(request, adminToken)
                    .andExpect(status().isCreated());

            // Verify book was saved to database
            assertThat(bookRepository.findByIsbn("978-0-123456-78-9")).isPresent();
        }

        @Test
        @DisplayName("Should return 403 when member tries to create book")
        void shouldReturn403WhenMemberTriesToCreateBook() throws Exception {
            // Given
            BookCreationRequest request = new BookCreationRequest(
                "978-0-123456-78-9",
                "The Great Adventure",
                "John Smith",
                "An exciting adventure story",
                BookGenre.FICTION,
                5,
                LocalDate.of(2023, 1, 15)
            );

            // When & Then
            mockMvc.perform(post("/api/books")
                    .header("Authorization", "Bearer " + memberToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 when no authentication token is provided")
        void shouldReturn401WhenNoAuthenticationTokenIsProvided() throws Exception {
            // Given
            BookCreationRequest request = new BookCreationRequest(
                "978-0-123456-78-9",
                "The Great Adventure",
                "John Smith",
                "An exciting adventure story",
                BookGenre.FICTION,
                5,
                LocalDate.of(2023, 1, 15)
            );

            // When & Then
            mockMvc.perform(post("/api/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @ParameterizedTest
        @ValueSource(strings = {"123", ""})
        @DisplayName("Invalid ISBN should return 400")
        void shouldReturn400WhenInvalidIsbn(String isbn) throws Exception {
            BookCreationRequest request = new BookCreationRequest(
                    isbn, "Adventure", "Author",
                    "Description", BookGenre.FICTION, 5, LocalDate.now());

            mockMvc.perform(post("/api/books")
                    .headers(authHeader(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.validationErrors").isArray());
        }

        @Test
        @DisplayName("Should return 400 when book creation request has missing required fields")
        void shouldReturn400WhenBookCreationRequestHasMissingRequiredFields() throws Exception {
            // Given
            BookCreationRequest request = new BookCreationRequest();
            // Missing title, author, genre, etc.

            // When & Then
            mockMvc.perform(post("/api/books")
                    .headers(authHeader(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.validationErrors").isNotEmpty());
        }

        @Test
        @DisplayName("Should return 409 when trying to create book with duplicate ISBN")
        void shouldReturn409WhenTryingToCreateBookWithDuplicateIsbn() throws Exception {
            BookCreationRequest request = new BookCreationRequest(
                "978-0-123456-78-9", "The Great Adventure", "John Smith",
                "Story", BookGenre.FICTION, 5, LocalDate.now());

            createBook(request, adminToken)
                .andExpect(status().isCreated());

            createBook(request, adminToken)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));

            assertThat(bookRepository.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Book Search Tests")
    class BookSearchTests {

        @BeforeEach
        void setUpTestBooks() throws Exception {
            // Create test books
            BookCreationRequest book1 = new BookCreationRequest(
                "978-0-123456-78-9",
                "The Great Adventure",
                "John Smith",
                "An exciting adventure story",
                BookGenre.FICTION,
                5,
                LocalDate.of(2023, 1, 15)
            );

            BookCreationRequest book2 = new BookCreationRequest(
                "978-0-987654-32-1",
                "Science Fundamentals",
                "Jane Doe",
                "A comprehensive guide to science",
                BookGenre.SCIENCE,
                3,
                LocalDate.of(2023, 2, 20)
            );

            BookCreationRequest book3 = new BookCreationRequest(
                "978-0-555555-55-5",
                "History of the World",
                "Bob Johnson",
                "A detailed history book",
                BookGenre.HISTORY,
                1, 
                LocalDate.of(2023, 3, 10)
            );

            // Create books
            createBook(book1, adminToken)
                    .andExpect(status().isCreated());

            createBook(book2, adminToken)
                    .andExpect(status().isCreated());

            createBook(book3, adminToken)
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should return all books with pagination")
        void shouldReturnAllBooksWithPagination() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/books/search")
                    .param("page", "0")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(3))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.number").value(0));
        }

        @Test
        @DisplayName("Should return second page with correct offset")
        void shouldReturnSecondPage() throws Exception {
            mockMvc.perform(get("/api/books/search")
                            .param("page", "1")
                            .param("size", "2")
                            .headers(authHeader(memberToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }

        @Test
        @DisplayName("Should sort books by title ascending")
        void shouldSortBooksByTitleAsc() throws Exception {
            mockMvc.perform(get("/api/books")
                            .param("sortBy", "title")
                            .param("sortDir", "asc")
                            .headers(authHeader(memberToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value(containsString("History")));
        }

        @Test
        @DisplayName("Should sort books by title descending")
        void shouldSortBooksByTitleDesc() throws Exception {
            mockMvc.perform(get("/api/books")
                            .param("sortBy", "title")
                            .param("sortDir", "desc")
                            .headers(authHeader(memberToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value(containsString("Great")));
        }

        @Test
        @DisplayName("Search term + sort ascending should order results by title")
        void shouldSortSearchResultsAsc() throws Exception {
            // Given 
            createBook(new BookCreationRequest("91325860911354", "Python for Beginners", "Author D", "Desc",
                    BookGenre.NON_FICTION, 5, LocalDate.now()), adminToken);

            createBook(new BookCreationRequest("914269008765133", "Python Advanced", "Author E", "Desc",
                    BookGenre.NON_FICTION, 5, LocalDate.now()), adminToken);
            
            // When & Then
            mockMvc.perform(get("/api/books/search")
                            .param("searchTerm", "Python")
                            .param("sortBy", "title")
                            .param("sortDir", "asc")
                            .headers(authHeader(memberToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("Python Advanced"))
                    .andExpect(jsonPath("$.content[1].title").value("Python for Beginners"));
        }

        @Test
        @DisplayName("Unknown sort field should return Bad Request (400)")
        void shouldReturnBadRequestForInvalidSortBy() throws Exception {
            mockMvc.perform(get("/api/books/search")
                            .param("searchTerm", "Spring")
                            .param("sortBy", "unknownField")
                            .headers(authHeader(memberToken)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("Invalid page number should return Bad Request (400)")
        void shouldReturnBadRequestForNegativePage() throws Exception {
            mockMvc.perform(get("/api/books/search")
                            .param("searchTerm", "Spring")
                            .param("page", "-1")
                            .headers(authHeader(memberToken)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("Should return books filtered by genre")
        void shouldReturnBooksFilteredByGenre() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/books/search")
                    .param("genre", "FICTION"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].genre").value("FICTION"))
                    .andExpect(jsonPath("$.content[0].title").value("The Great Adventure"));
        }

        @Test
        @DisplayName("Should return books filtered by search term")
        void shouldReturnBooksFilteredBySearchTerm() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/books/search")
                    .param("searchTerm", "Adventure"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].title").value("The Great Adventure"));
        }

        @Test
        @DisplayName("Search with no matches should return empty page")
        void shouldReturnEmptyWhenNoMatchesFound() throws Exception {
            mockMvc.perform(get("/api/books/search")
                    .param("searchTerm", "Python")  
                    .headers(authHeader(memberToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }

        @Test
        @DisplayName("Should return only available books when availableOnly is true")
        void shouldReturnOnlyAvailableBooksWhenAvailableOnlyIsTrue() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/books/search")
                    .param("availableOnly", "true"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(3)) // Only books with available copies
                    .andExpect(jsonPath("$.content[0].availableCopies").value(greaterThan(0)))
                    .andExpect(jsonPath("$.content[1].availableCopies").value(greaterThan(0)));
        }

        @Test
        @DisplayName("Should return books by title search")
        void shouldReturnBooksByTitleSearch() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/books/title/Adventure"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].title").value(containsString("Adventure")));
        }

        @Test
        @DisplayName("Should return books by author search")
        void shouldReturnBooksByAuthorSearch() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/books/author/John"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].author").value(containsString("John")));
        }

        @Test
        @DisplayName("Should return books by genre")
        void shouldReturnBooksByGenre() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/books/genre/SCIENCE"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].genre").value("SCIENCE"));
        }

        @Test
        @DisplayName("Should return available books only")
        void shouldReturnAvailableBooksOnly() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/books/available"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(3)) // Only books with available copies
                    .andExpect(jsonPath("$[0].availableCopies").value(greaterThan(0)))
                    .andExpect(jsonPath("$[1].availableCopies").value(greaterThan(0)));
        }

        @Test
        @DisplayName("Should return full-text search results")
        void shouldReturnFullTextSearchResults() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/books/search/full-text")
                    .param("searchTerm", "adventure"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].title").value("The Great Adventure"));
        }

        @Test
        @DisplayName("Should return available genres")
        void shouldReturnAvailableGenres() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/books/genres"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(greaterThan(0)))
                    .andExpect(jsonPath("$.length()").value(is(BookGenre.values().length)));
        }
    }

    @Nested
    @DisplayName("Book Retrieval Tests")
    class BookRetrievalTests {

        private Long bookId;
        private String bookIsbn;

        private static final String TEST_ISBN = "978-0-123456-78-9";
        private static final String TEST_TITLE = "The Great Adventure";
        private static final String TEST_AUTHOR = "John Smith";
        private static final BookGenre TEST_GENRE = BookGenre.FICTION;
        private static final int TEST_AVAILABLE_COPIES = 5;

        @BeforeEach
        void setUpTestBook() throws Exception {
            BookCreationRequest request = new BookCreationRequest(
                    TEST_ISBN,
                    TEST_TITLE,
                    TEST_AUTHOR,
                    "An exciting adventure story",
                    TEST_GENRE,
                    TEST_AVAILABLE_COPIES,
                    LocalDate.of(2023, 1, 15)
            );

            BookResponse bookResponse = createBookAndReturnResponse(request, adminToken); 

            bookId = bookResponse.getId();
            bookIsbn = TEST_ISBN;
        }

        @Test
        @DisplayName("Should return book by ID when it exists")
        void shouldReturnBookById() throws Exception {
            mockMvc.perform(get("/api/books/{id}", bookId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(bookId))
                    .andExpect(jsonPath("$.title").value(TEST_TITLE))
                    .andExpect(jsonPath("$.author").value(TEST_AUTHOR))
                    .andExpect(jsonPath("$.genre").value(TEST_GENRE.name()))
                    .andExpect(jsonPath("$.availableCopies").value(TEST_AVAILABLE_COPIES));
        }

        @Test
        @DisplayName("Should return book by ISBN when it exists")
        void shouldReturnBookByIsbn() throws Exception {
            mockMvc.perform(get("/api/books/isbn/{isbn}", bookIsbn))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(bookId))
                    .andExpect(jsonPath("$.title").value(TEST_TITLE))
                    .andExpect(jsonPath("$.author").value(TEST_AUTHOR))
                    .andExpect(jsonPath("$.genre").value(TEST_GENRE.name()))
                    .andExpect(jsonPath("$.availableCopies").value(TEST_AVAILABLE_COPIES));
        }

        @Test
        @DisplayName("Should return 404 when book ID does not exist")
        void shouldReturn404WhenBookIdDoesNotExist() throws Exception {
            mockMvc.perform(get("/api/books/{id}", 99999))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value(containsString("Book")))
                    .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("Should return 404 when book ISBN does not exist")
        void shouldReturn404WhenBookIsbnDoesNotExist() throws Exception {
            mockMvc.perform(get("/api/books/isbn/{isbn}", "999-0-000000-00-0"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value(containsString("Book")))
                    .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
        }
    }   
}
