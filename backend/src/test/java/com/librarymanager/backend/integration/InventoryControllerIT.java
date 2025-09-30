package com.librarymanager.backend.integration;

import com.librarymanager.backend.entity.Book;
import com.librarymanager.backend.entity.BookGenre;
import com.librarymanager.backend.entity.User;
import com.librarymanager.backend.entity.UserRole;
import com.librarymanager.backend.repository.BookRepository;
import com.librarymanager.backend.repository.UserRepository;
import com.librarymanager.backend.security.CustomUserDetails;
import com.librarymanager.backend.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for InventoryController using Testcontainers.
 * 
 * These tests verify end-to-end book borrowing and returning operations:
 * - Borrowing books when copies are available
 * - Returning books successfully
 * - Attempting to borrow when no copies are available
 * - Authentication and authorization requirements
 * - Error handling and response format consistency
 * 
 * Tests use real PostgreSQL database via Testcontainers and MockMvc
 * for realistic integration testing.
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
@AutoConfigureMockMvc
class InventoryControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String memberToken;
    private String adminToken;
    private Long availableBookId;
    private Long unavailableBookId;

    /**
     * Set up test data before each test.
     */
    @BeforeEach
    void setUp() throws Exception {
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

        // Generate JWT tokens
        adminToken = jwtTokenService.generateToken(new CustomUserDetails(admin));
        memberToken = jwtTokenService.generateToken(new CustomUserDetails(member));
        
        createTestBooks();
}

    private void createTestBooks() throws Exception {
        
        // Create book with available copies
        Book availableBook = Book.builder()
                .isbn("978-0-123456-78-9")
                .title("The Great Adventure")
                .author("John Smith")
                .description("An exciting adventure story")
                .genre(BookGenre.FICTION)
                .totalCopies(3) // 3 copies available
                .availableCopies(3)
                .publicationDate(LocalDate.of(2023, 1, 15))
                .build();
        availableBookId = bookRepository.save(availableBook).getId();

        Book unavailableBook = Book.builder()
                .isbn("978-0134685991")
                .title("Out of Stock Book")
                .author("Bob Jones")
                .description("An unavailable book")
                .genre(BookGenre.NON_FICTION)
                .totalCopies(1)
                .availableCopies(0)
                .publicationDate(LocalDate.of(2020, 1, 24))
                .build();
        unavailableBookId = bookRepository.save(unavailableBook).getId();
    }

      // ---------- Helpers ----------

      private ResultActions borrowBook(Long bookId, String token) throws Exception {
        return mockMvc.perform(post("/api/inventory/books/{id}/borrow", bookId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions returnBook(Long bookId, String token) throws Exception {
        return mockMvc.perform(post("/api/inventory/books/{id}/return", bookId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private void assertAvailableCopies(Long bookId, int expected) {
        var book = bookRepository.findById(bookId);
        assertThat(book).isPresent();
        assertThat(book.get().getAvailableCopies()).isEqualTo(expected);
    }

    @Nested
    @DisplayName("Book Borrowing Tests")
    class BookBorrowingTests {

        @Test
        @DisplayName("Should return 200 when book is successfully borrowed")
        void shouldReturn200WhenBookIsSuccessfullyBorrowed() throws Exception {
            // When & Then
            borrowBook(availableBookId, memberToken)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(availableBookId))
                .andExpect(jsonPath("$.availableCopies").value(2)); // Should decrease by 1

            // Verify book was updated in database
            assertAvailableCopies(availableBookId, 2);
        }

        @Test
        @DisplayName("Should return 422 when trying to borrow book with no available copies")
        void shouldReturn422WhenTryingToBorrowBookWithNoAvailableCopies() throws Exception {
            // When & Then
            borrowBook(unavailableBookId, memberToken)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
        }

        @Test
        @DisplayName("Should return 401 when no authentication token is provided")
        void shouldReturn401WhenNoAuthenticationTokenIsProvided() throws Exception {
            // When & Then
            borrowBook(availableBookId, "invalid.jwt.token")
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 404 when trying to borrow non-existent book")
        void shouldReturn404WhenTryingToBorrowNonExistentBook() throws Exception {
            // When & Then
            Long nonExistentBookId = 9999L;

            borrowBook(nonExistentBookId, memberToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Book with ID " + nonExistentBookId + " not found"));
        }

        @Test
        @DisplayName("Should allow multiple borrows until copies are exhausted")
        void shouldAllowMultipleBorrowsUntilCopiesAreExhausted() throws Exception {
                int initialCopies = 3;

                // Borrow until no copies left
                for (int i = 1; i <= initialCopies; i++) {
                    int expectedRemaining = initialCopies - i;
            
                    borrowBook(availableBookId, memberToken)
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.availableCopies").value(expectedRemaining));
                }
            
                // One more borrow attempt should fail
                borrowBook(availableBookId, memberToken)
                        .andExpect(status().isUnprocessableEntity())
                        .andExpect(jsonPath("$.status").value(422))
                        .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
        }
    }

    @Nested
    @DisplayName("Book Returning Tests")
    class BookReturningTests {

        @Test
        @DisplayName("Should return 200 when book is successfully returned")
        void shouldReturn200WhenBookIsSuccessfullyReturned() throws Exception {
            // Given - First borrow a book
            borrowBook(availableBookId, memberToken).andExpect(status().isOk());

            // When - Return the book
            returnBook(availableBookId, memberToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCopies").value(3)); // Should increase by 1

            // Verify book was updated in database
            assertAvailableCopies(availableBookId, 3);
        }

        @Test
        @DisplayName("Should return 422 when trying to return book that is already at maximum copies")
        void shouldReturn422WhenTryingToReturnBookThatIsAlreadyAtMaximumCopies() throws Exception {
            // When & Then - Try to return a book that hasn't been borrowed
            // availableBookId starts with all copies available in @BeforeEach
            returnBook(availableBookId, memberToken)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
        }

        @Test
        @DisplayName("Should return 401 when no authentication token is provided for return")
        void shouldReturn401WhenNoAuthenticationTokenIsProvidedForReturn() throws Exception {
            // When & Then
            returnBook(unavailableBookId, "invalid.jwt.token")
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 404 when trying to return non-existent book")
        void shouldReturn404WhenTryingToReturnNonExistentBook() throws Exception {
            // When & Then
            Long nonExistentBookId = 9999L;

            returnBook(nonExistentBookId, memberToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Book with ID " + nonExistentBookId + " not found"));
        }
    }

    @Nested
    @DisplayName("Authentication and Authorization Tests")
    class AuthenticationAndAuthorizationTests {

        @Test
        @DisplayName("Should allow both members and admins to borrow books")
        void shouldAllowBothMembersAndAdminsToBorrowBooks() throws Exception {
            // Member borrow
            borrowBook(availableBookId, memberToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.availableCopies").value(2));

            // Admin borrow
            borrowBook(availableBookId, adminToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.availableCopies").value(1));
        }

        @Test
        @DisplayName("Should allow both members and admins to return books")
        void shouldAllowBothMembersAndAdminsToReturnBooks() throws Exception {
            // Given - Borrow books first
            borrowBook(availableBookId, memberToken)
                .andExpect(status().isOk());

            borrowBook(availableBookId, adminToken)
                .andExpect(status().isOk());

            // When - Return books
            returnBook(availableBookId, memberToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.availableCopies").value(2));

            returnBook(availableBookId, adminToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.availableCopies").value(3));
        }
    }
}
