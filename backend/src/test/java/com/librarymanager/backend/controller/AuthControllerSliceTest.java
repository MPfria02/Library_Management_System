package com.librarymanager.backend.controller;

import com.librarymanager.backend.dto.request.LoginRequest;
import com.librarymanager.backend.dto.request.UserRegistrationRequest;
import com.librarymanager.backend.dto.response.UserResponse;
import com.librarymanager.backend.entity.User;
import com.librarymanager.backend.entity.UserRole;
import com.librarymanager.backend.exception.AuthenticationException;
import com.librarymanager.backend.exception.DuplicateResourceException;
import com.librarymanager.backend.mapper.UserMapper;
import com.librarymanager.backend.security.CustomUserDetails;
import com.librarymanager.backend.security.JwtAuthenticationFilter;
import com.librarymanager.backend.security.JwtTokenService;
import com.librarymanager.backend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;

/**
 * Slice tests for AuthController using @WebMvcTest.
 * 
 * Tests the web layer in isolation, focusing on HTTP behavior,
 * request/response mapping, and exception handling.
 * 
 * @author Marcel Pulido
 */
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    // ========== Registration Tests ==========

    @Test
    public void shouldReturn201WhenValidUserIsRegistered() throws Exception {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest(
            "john.doe@example.com", "password123", "John", "Doe", "1234567890", UserRole.MEMBER
        );
        
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail("john.doe@example.com");
        savedUser.setFirstName("John");
        savedUser.setLastName("Doe");
        savedUser.setPhone("1234567890");
        savedUser.setRole(UserRole.MEMBER);
        
        UserResponse response = new UserResponse(1L, "john.doe@example.com", "John", "Doe", "1234567890", UserRole.MEMBER);

        given(userService.createUser(any(User.class))).willReturn(savedUser);
        given(userMapper.toEntity(any(UserRegistrationRequest.class))).willReturn(savedUser);
        given(userMapper.toResponse(any(User.class))).willReturn(response);
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.phone").value("1234567890"))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    public void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest(
            "existing.email@example.com", "password123", "John", "Doe", "1234567890", UserRole.MEMBER
        );
        
        User user = new User();
        given(userMapper.toEntity(any(UserRegistrationRequest.class))).willReturn(user);
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
        willThrow(new DuplicateResourceException("Email already exists", "DUPLICATE_EMAIL"))
                .given(userService).createUser(any(User.class));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email already exists"))
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_RESOURCE"));
    }

    @Test
    public void shouldReturn400WhenValidationFails() throws Exception {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest(
            "", "short", "John", "Doe", "1234567890", UserRole.MEMBER
        );

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors").isArray())
                .andExpect(jsonPath("$.validationErrors[*]").value(
                        containsInAnyOrder(
                                containsString("Email is required"),
                                containsString("Password must be at least 6 characters long"))
                 ));
    }

    @Test
    public void shouldReturn400WhenInvalidJson() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_JSON"));
    }

    @Test
    public void shouldReturn201WhenNullRole() throws Exception {
        // Arrange
        UserRegistrationRequest request = new UserRegistrationRequest(
            "john.doe@example.com", "password123", "John", "Doe", "1234567890", null
        );
        
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail("john.doe@example.com");
        savedUser.setFirstName("John");
        savedUser.setLastName("Doe");
        savedUser.setPhone("1234567890");
        savedUser.setRole(null);
        
        UserResponse response = new UserResponse(1L, "john.doe@example.com", "John", "Doe", "1234567890", null);

        given(userService.createUser(any(User.class))).willReturn(savedUser);
        given(userMapper.toEntity(any(UserRegistrationRequest.class))).willReturn(savedUser);
        given(userMapper.toResponse(any(User.class))).willReturn(response);
        given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").isEmpty());
    }

    // ========== Login Tests ==========

    @Test
    public void shouldReturn200WhenValidCredentialsProvided() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("john.doe@example.com", "password123");
        
        CustomUserDetails userDetails = org.mockito.Mockito.mock(CustomUserDetails.class);
        given(userDetails.getEmail()).willReturn("john.doe@example.com");
        given(userDetails.getRole()).willReturn(UserRole.MEMBER);
        
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        given(authentication.getPrincipal()).willReturn(userDetails);
        given(authenticationManager.authenticate(any())).willReturn(authentication);
        given(jwtTokenService.generateToken(any(CustomUserDetails.class))).willReturn("jwt-token");
        
        Date expirationDate = new Date(System.currentTimeMillis() + 3600000); // 1 hour from now
        given(jwtTokenService.extractExpiration(anyString())).willReturn(expirationDate);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.expiresIn").exists());
    }

    @Test
    public void shouldReturn401WhenInvalidCredentialsProvided() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("john.doe@example.com", "wrongpassword");
        
        willThrow(new AuthenticationException("Invalid credentials"))
                .given(authenticationManager).authenticate(any());

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    public void shouldReturn400WhenLoginValidationFails() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest("invalid-email", "");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors").isArray())
                .andExpect(jsonPath("$.validationErrors[*]").value(
                    containsInAnyOrder(
                        containsString("Email must be valid"),
                        containsString("Password is required"))
                ));
    }

    @Test
    public void shouldReturn400WhenLoginRequestIsEmpty() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    public void shouldReturn400WhenLoginRequestIsNull() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_JSON"));
    }

    // ========== Edge Cases ==========

    @Test
    public void shouldReturn400WhenRegistrationRequestIsNull() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_JSON"));
    }

    @Test
    public void shouldReturn400WhenRegistrationRequestIsEmpty() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}