package com.librarymanager.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarymanager.backend.dto.request.LoginRequest;
import com.librarymanager.backend.dto.request.UserRegistrationRequest;
import com.librarymanager.backend.dto.response.AuthResponse;
import com.librarymanager.backend.entity.User;
import com.librarymanager.backend.entity.UserRole;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController using Testcontainers :
 * - Stable assertions using errorCode/field existence.
 * - Whitelisted fields for response validation.
 * - Role-based access tests with real JWT tokens.
 * - Test data builders for readability.
 */
@AutoConfigureMockMvc
class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenService jwtTokenService;

    private String adminToken;
    private String memberToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User admin = User.builder()
            .email("admin@example.com")
            .password(passwordEncoder.encode("adminpass"))
            .firstName("Admin")
            .lastName("User")
            .role(UserRole.ADMIN)
            .build();
        userRepository.save(admin);

        User member = User.builder()
            .email("member@example.com")
            .password(passwordEncoder.encode("memberpass"))
            .firstName("Member")
            .lastName("User")
            .role(UserRole.MEMBER)
            .build();
        userRepository.save(member);

        adminToken = jwtTokenService.generateToken(new CustomUserDetails(admin));
        memberToken = jwtTokenService.generateToken(new CustomUserDetails(member));
    }

    // ---- Test Data Builders ----
    private UserRegistrationRequest validRegistrationRequest() {
        return new UserRegistrationRequest(
            "john.doe@example.com",
            "password123",
            "John",
            "Doe",
            "+1234567890",
            UserRole.MEMBER
        );
    }

    private LoginRequest loginRequest(String email, String password) {
        return new LoginRequest(email, password);
    }

    // ---- Registration Tests ----
    @Nested
    @DisplayName("User Registration")
    class UserRegistrationTests {

        @Test
        void shouldRegisterValidUser() throws Exception {
            UserRegistrationRequest request = validRegistrationRequest();

            MvcResult result = mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andReturn();

            // Whitelist fields
            JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());
            Set<String> allowedFields = Set.of("id", "email", "firstName", "lastName", "phone", "role");
            assertThat(responseJson.fieldNames()).toIterable().allSatisfy(allowedFields::contains);

            assertThat(userRepository.findByEmail("john.doe@example.com")).isPresent();
        }

        @Test
        void shouldRejectInvalidEmail() throws Exception {
            UserRegistrationRequest request = validRegistrationRequest();
            request.setEmail("invalid-email");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors", hasSize(greaterThan(0))));
        }

        @Test
        void shouldRejectShortPassword() throws Exception {
            UserRegistrationRequest request = validRegistrationRequest();
            request.setPassword("123");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors[0]", containsString("Password")));
        }

        @Test
        void shouldRejectMissingRequiredFields() throws Exception {
            UserRegistrationRequest request = new UserRegistrationRequest();
            request.setEmail("john.doe@example.com");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors").isArray());
        }

        @Test
        void shouldRejectDuplicateEmail() throws Exception {
            UserRegistrationRequest request = validRegistrationRequest();

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));
        }
    }

    // ---- Login Tests ----
    @Nested
    @DisplayName("User Login")
    class UserLoginTests {

        @Test
        void shouldLoginWithValidCredentials() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRegistrationRequest())))
                .andExpect(status().isCreated());

            MvcResult result = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest("john.doe@example.com", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andReturn();

            AuthResponse auth = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
            assertThat(auth.getToken()).isNotBlank();
        }

        @Test
        void shouldRejectInvalidPassword() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRegistrationRequest())))
                .andExpect(status().isCreated());

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest("john.doe@example.com", "wrong"))))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldRejectNonExistentEmail() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest("ghost@example.com", "password"))))
                .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldRejectInvalidEmailFormat() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest("bad-email", "password"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }

        @Test
        void shouldRejectMissingPassword() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest("john.doe@example.com", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }
    }

    // ---- Role-based Access Tests ----
    @Nested
    @DisplayName("Role-based Access")
    class RoleAccessTests {

        @Test
        void adminShouldAccessProtectedEndpoint() throws Exception {
            mockMvc.perform(get("/api/users/count")
                    .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        }

        @Test
        void memberShouldBeForbiddenFromAdminEndpoint() throws Exception {
            mockMvc.perform(get("/api/users/count")
                    .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
        }
    }

    // ---- Error Response Consistency ----
    @Nested
    @DisplayName("Error Response Format")
    class ErrorResponseFormatTests {

        @Test
        void shouldReturnConsistentValidationErrorFormat() throws Exception {
            UserRegistrationRequest request = new UserRegistrationRequest();

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors").isArray());
        }

        @Test
        void shouldReturnConsistentJsonParsingErrorFormat() throws Exception {
            String invalidJson = "{ \"email\": \"test@example.com\", \"password\": }";

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("INVALID_JSON"))
                .andExpect(jsonPath("$.details").value("Request body contains invalid JSON"));
        }
    }
}

