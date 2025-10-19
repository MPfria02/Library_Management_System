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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;


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
    private Long availableBookId, unavailableBookId, partiallyAvailableBookId;
    private Book availableBook, unavailableBook, partiallyAvailableBook;

    /**
     * Set up test data before each test.
     */
    @BeforeEach
    void setUp() throws Exception {
        // Clean up database
        bookRepository.deleteAll();
        userRepository.deleteAll();

        User member = User.builder()
                .email("member@library.com")
                .password(passwordEncoder.encode("member123"))
                .firstName("Member")
                .lastName("User")
                .role(UserRole.MEMBER)
                .build();
        userRepository.save(member);

        // Generate JWT tokens
        memberToken = jwtTokenService.generateToken(new CustomUserDetails(member));
        
        createTestBooks();
}

    private void createTestBooks() throws Exception {
        
        // Create book with available copies
        availableBook = Book.builder()
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

        unavailableBook = Book.builder()
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

        partiallyAvailableBook = Book.builder()
                .isbn("978-0134685551")
                .title("Partially Available")
                .author("Paul Jones")
                .description("A partially available book")
                .genre(BookGenre.NON_FICTION)
                .totalCopies(4)
                .availableCopies(2)
                .publicationDate(LocalDate.of(2020, 1, 24))
                .build();
        partiallyAvailableBookId = bookRepository.save(partiallyAvailableBook).getId(); 
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

    private ResultActions checkBorrowStatus(Long bookId, String token) throws Exception {
        return mockMvc.perform(get("/api/inventory/books/{id}/status", bookId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions getUserBorrowRecords(String token) throws Exception {
        return mockMvc.perform(get("/api/inventory/books")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON));
    }

    private void assertAvailableCopies(Long bookId, int expected) {
        var book = bookRepository.findById(bookId);
        assertThat(book).isPresent();
        assertThat(book.get().getAvailableCopies()).isEqualTo(expected);
    }

    @Nested
    @DisplayName("Borrow Book Tests")
    class BorrowBookTests {
        @Test
        @DisplayName("Should return 200 when book is successfully borrowed")
        void shouldReturn200WhenBookIsSuccessfullyBorrowed() throws Exception {
            // When & Then
            borrowBook(availableBookId, memberToken)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bookId").value(availableBookId))
                .andExpect(jsonPath("$.bookTitle").value(availableBook.getTitle()))
                .andExpect(jsonPath("$.status").value("BORROWED"));

            // Assert available copies decreased
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
                .andExpect(jsonPath("$.message").value("Book not found"));
        }

        @Test
        @DisplayName("Should not allow multiple borrows of the same book from the same user")
        void shouldAllowMultipleBorrowsUntilCopiesAreExhausted() throws Exception {
                // First time borrowing should be handle gracefully
                borrowBook(availableBookId, memberToken)
                        .andExpect(status().isOk());
            
                // One more borrow attempt should fail
                borrowBook(availableBookId, memberToken)
                        .andExpect(status().isUnprocessableEntity())
                        .andExpect(jsonPath("$.status").value(422))
                        .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
        }
    }

    @Nested
    @DisplayName("Return Book Tests")
    class ReturnBookTests {
        @Test
        @DisplayName("Should return 200 when book is successfully returned")
        void shouldReturn200WhenBookIsSuccessfullyReturned() throws Exception {
            // Given - First borrow a book
            borrowBook(availableBookId, memberToken).andExpect(status().isOk());

            // When - Return the book
            returnBook(availableBookId, memberToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.returnDate").value(LocalDate.now().toString()));
                

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
        @DisplayName("Should return 422 when trying to return book that the user has not borrowed")
        void shouldReturn422WhenTryingToReturnBookThatTheUserHasNotBorrowed() throws Exception {
            // When & Then - Try to return a book that hasn't borrowed
            returnBook(unavailableBookId, memberToken)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
        }

        @Test
        @DisplayName("Should return 422 when trying to return book that the user has already return")
        void shouldReturn422WhenTryingToReturnBookThatTheUserHasAlreadyReturn() throws Exception {
            // User borrows the book
            borrowBook(partiallyAvailableBookId, memberToken).andExpect(status().isOk());

            // User successfully returns the book
            returnBook(partiallyAvailableBookId, memberToken).andExpect(status().isOk());            
            
            // Tries to return a book that already returned
            returnBook(unavailableBookId, memberToken)
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
                .andExpect(jsonPath("$.message").value("User or Book not found"));
        }
    }

    @Nested
    @DisplayName("Check Borrow Status Tests")
    class CheckBorrowStatusTests {
        @Test
        @DisplayName("Should return true when user has borrowed book")
        void shouldReturnTrueWhenUserHasBorrowedBook() throws Exception {
            borrowBook(availableBookId, memberToken).andExpect(status().isOk());
            checkBorrowStatus(availableBookId, memberToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.borrowed").value(true));
        }
        @Test
        @DisplayName("Should return false when user has not borrowed book")
        void shouldReturnFalseWhenUserHasNotBorrowedBook() throws Exception {
            checkBorrowStatus(availableBookId, memberToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.borrowed").value(false));
        }
        @Test
        @DisplayName("Should return 401 when no authentication token is provided for status check")
        void shouldReturn401WhenNoAuthenticationTokenIsProvidedForStatusCheck() throws Exception {
            checkBorrowStatus(availableBookId, null)
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("User Borrow Records Tests")
    class UserBorrowRecordsTests {
        @Test
        @DisplayName("Should return page of borrow record responses when valid user and default params")
        void shouldReturnPageOfBorrowRecordResponsesWhenValidUserAndDefaultParams() throws Exception {
            // Borrow a book so there is a record
            borrowBook(availableBookId, memberToken).andExpect(status().isOk());
            getUserBorrowRecords(memberToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].bookId").value(availableBookId))
                .andExpect(jsonPath("$.content[0].status").value("BORROWED"));             
        }

        @Test
        @DisplayName("Should return empty page when user has no borrowed books")
        void shouldReturnEmptyPageWhenUserHasNoBorrowedBooks() throws Exception {
            getUserBorrowRecords(memberToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
        }
        @Test
        @DisplayName("Should return 401 when no authentication token is provided for borrow records")
        void shouldReturn401WhenNoAuthenticationTokenIsProvidedForBorrowRecords() throws Exception {
            getUserBorrowRecords(null)
                .andExpect(status().isUnauthorized());
        }
        @Test
        @DisplayName("Should return page of borrow record responses when page and size provided")
        void shouldReturnPageOfBorrowRecordResponsesWhenPageAndSizeProvided() throws Exception {
            // Borrow a book so there is a record
            borrowBook(availableBookId, memberToken).andExpect(status().isOk());
            mockMvc.perform(
                get("/api/inventory/books")
                    .header("Authorization", "Bearer " + memberToken)
                    .param("page", "1")
                    .param("size", "5")
                    .param("status", "BORROWED")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.content.length()").value(lessThanOrEqualTo(5)));

        }
    }
}
