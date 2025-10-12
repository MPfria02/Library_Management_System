package com.librarymanager.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarymanager.backend.dto.request.BookCreationRequest;
import com.librarymanager.backend.dto.response.BookAdminResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class BookAdminControllerIT extends AbstractIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private BookRepository bookRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String memberToken;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
        userRepository.deleteAll();
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
        adminToken = jwtTokenService.generateToken(new com.librarymanager.backend.security.CustomUserDetails(admin));
        memberToken = jwtTokenService.generateToken(new com.librarymanager.backend.security.CustomUserDetails(member));
    }

    private HttpHeaders authHeader(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        return headers;
    }

    private ResultActions createBookAsAdmin(BookCreationRequest request) throws Exception {
        return mockMvc.perform(post("/api/admin/books")
            .headers(authHeader(adminToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {
        @Test
        void shouldReturn403WhenMemberTriesToAccessAdminEndpoint() throws Exception {
            BookCreationRequest req = new BookCreationRequest("1234567890123", "Admin Only", "A Admin", "D", BookGenre.FICTION, 2, LocalDate.now());
            
            mockMvc.perform(post("/api/admin/books")
                .headers(authHeader(memberToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
        }
        @Test
        void shouldReturn401WhenNoAuthenticationTokenIsProvided() throws Exception {
            BookCreationRequest req = new BookCreationRequest("1234567890123", "Admin Only", "A Admin", "D", BookGenre.FICTION, 2, LocalDate.now());
            mockMvc.perform(post("/api/admin/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("CRUD Flow Tests")
    class CrudFlowTests {
        @Test
        void shouldReturn201WhenValidBookIsCreatedByAdmin() throws Exception {
            BookCreationRequest req = new BookCreationRequest("1122334455", "Integration Admin Book", "Integration Author", "Testing", BookGenre.HISTORY, 6, LocalDate.of(2023,2,3));
            createBookAsAdmin(req).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.isbn").value("1122334455"))
                    .andExpect(jsonPath("$.totalCopies").value(6));
            assertThat(bookRepository.findByIsbn("1122334455")).isPresent();
        }
        @Test
        void shouldReturn409WhenDuplicateIsbnIsProvided() throws Exception {
            BookCreationRequest req = new BookCreationRequest("DUP-9999999","DUP TITLE","Auth","D", BookGenre.FICTION, 2, LocalDate.now());
            createBookAsAdmin(req).andExpect(status().isCreated());
            createBookAsAdmin(req).andExpect(status().isConflict());
            assertThat(bookRepository.findAll().size()).isEqualTo(1);
        }
        @Test
        void shouldReturnAllBooksWithPaginationForAdmin() throws Exception {
            createBookAsAdmin(new BookCreationRequest("ISBN-1234567", "TestTitle", "TestAuthor", "TestDescription", BookGenre.FICTION, 2, LocalDate.now()));
            
            mockMvc.perform(get("/api/admin/books")
                .headers(authHeader(adminToken))
                .param("page", "0")
                .param("size","30"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
        }
        @Test
        void shouldReturnBookByIdWithAllFields() throws Exception {
            BookCreationRequest req = new BookCreationRequest("9999abcd000","BookX","AuthorX","DescX", BookGenre.HISTORY, 10, LocalDate.now());
            MvcResult result = createBookAsAdmin(req).andExpect(status().isCreated()).andReturn();
            BookAdminResponse book = objectMapper.readValue(result.getResponse().getContentAsString(), BookAdminResponse.class);
            mockMvc.perform(get("/api/admin/books/"+book.getId())
                .headers(authHeader(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("9999abcd000"))
                .andExpect(jsonPath("$.totalCopies").value(10))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
        }
        @Test
        void shouldUpdateBookAndReturnFullData() throws Exception {
            BookCreationRequest req = new BookCreationRequest("0000XUUP00","BookToUpdate","AuY","dsc", BookGenre.HISTORY, 10, LocalDate.now());
            MvcResult result = createBookAsAdmin(req).andExpect(status().isCreated()).andReturn();
            BookAdminResponse orig = objectMapper.readValue(result.getResponse().getContentAsString(), BookAdminResponse.class);
            req.setTitle("BOOK_UPDATED");
            mockMvc.perform(put("/api/admin/books/"+orig.getId())
                .headers(authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("BOOK_UPDATED"));
        }
        @Test
        void shouldDeleteBookSuccessfully() throws Exception {
            BookCreationRequest req = new BookCreationRequest("000DEL7788","DELME","DEL AUTH","D", BookGenre.FICTION, 2, LocalDate.now());
            MvcResult result = createBookAsAdmin(req).andExpect(status().isCreated()).andReturn();
            BookAdminResponse book = objectMapper.readValue(result.getResponse().getContentAsString(), BookAdminResponse.class);
            mockMvc.perform(delete("/api/admin/books/"+book.getId())
                .headers(authHeader(adminToken)))
                .andExpect(status().isNoContent());
            assertThat(bookRepository.findById(book.getId())).isEmpty();
        }
    }
}
