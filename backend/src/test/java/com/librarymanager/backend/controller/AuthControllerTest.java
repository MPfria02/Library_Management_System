package com.librarymanager.backend.controller;

import com.librarymanager.backend.dto.request.LoginRequest;
import com.librarymanager.backend.dto.request.UserRegistrationRequest;
import com.librarymanager.backend.dto.response.AuthResponse;
import com.librarymanager.backend.dto.response.UserResponse;
import com.librarymanager.backend.entity.User;
import com.librarymanager.backend.entity.UserRole;
import com.librarymanager.backend.mapper.UserMapper;
import com.librarymanager.backend.security.CustomUserDetails;
import com.librarymanager.backend.security.JwtTokenService;
import com.librarymanager.backend.service.UserService;
import com.librarymanager.backend.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthController.
 * 
 * Tests authentication endpoints including user registration and login functionality.
 * Follows Spring Boot testing best practices with proper mocking and assertions.
 * 
 * @author Marcel Pulido
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthController authController;

    private User testUser;
    private UserResponse testUserResponse;
    private UserRegistrationRequest testRegistrationRequest;
    private LoginRequest testLoginRequest;
    private CustomUserDetails testUserDetails;
    private Authentication testAuthentication;

    @BeforeEach
    void setUp() {
        // Setup test data
        testUser = TestDataFactory.createDefaultMemberUser();
        testUser.setId(1L);

        testUserResponse = new UserResponse();
        testUserResponse.setId(1L);
        testUserResponse.setEmail("member@test.com");
        testUserResponse.setFirstName("Test");
        testUserResponse.setLastName("Member");
        testUserResponse.setPhone("1234567890");
        testUserResponse.setRole(UserRole.MEMBER);

        testRegistrationRequest = new UserRegistrationRequest();
        testRegistrationRequest.setEmail("member@test.com");
        testRegistrationRequest.setPassword("rawPassword123");
        testRegistrationRequest.setFirstName("Test");
        testRegistrationRequest.setLastName("Member");
        testRegistrationRequest.setPhone("1234567890");
        testRegistrationRequest.setRole(UserRole.MEMBER);

        testLoginRequest = new LoginRequest();
        testLoginRequest.setEmail("member@test.com");
        testLoginRequest.setPassword("rawPassword123");

        testUserDetails = new CustomUserDetails(testUser);
        testAuthentication = mock(Authentication.class);
    }

    // ========== Registration Tests ==========

    @Test
    @DisplayName("register_shouldReturnCreatedUserResponse_whenValidRegistrationRequest")
    void register_shouldReturnCreatedUserResponse_whenValidRegistrationRequest() {
        // Given
        when(userMapper.toEntity(testRegistrationRequest)).thenReturn(testUser);
        when(passwordEncoder.encode("rawPassword123")).thenReturn("encodedPassword123");
        when(userService.createUser(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        // When
        ResponseEntity<UserResponse> response = authController.register(testRegistrationRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo("member@test.com");
        assertThat(response.getBody().getFirstName()).isEqualTo("Test");
        assertThat(response.getBody().getRole()).isEqualTo(UserRole.MEMBER);

        verify(userMapper).toEntity(testRegistrationRequest);
        verify(passwordEncoder).encode("rawPassword123");
        verify(userService).createUser(any(User.class));
        verify(userMapper).toResponse(testUser);
    }

    @Test
    @DisplayName("register_shouldEncodePassword_whenCreatingUser")
    void register_shouldEncodePassword_whenCreatingUser() {
        // Given
        User userWithoutEncodedPassword = TestDataFactory.createDefaultMemberUser();
        when(userMapper.toEntity(testRegistrationRequest)).thenReturn(userWithoutEncodedPassword);
        when(passwordEncoder.encode("rawPassword123")).thenReturn("encodedPassword123");
        when(userService.createUser(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        // When
        authController.register(testRegistrationRequest);

        // Then
        verify(passwordEncoder).encode("rawPassword123");
        verify(userService).createUser(argThat(user -> 
            "encodedPassword123".equals(user.getPassword())
        ));
    }

    @Test
    @DisplayName("register_shouldCallUserServiceCreateUser_whenValidRequest")
    void register_shouldCallUserServiceCreateUser_whenValidRequest() {
        // Given
        when(userMapper.toEntity(testRegistrationRequest)).thenReturn(testUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userService.createUser(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        // When
        authController.register(testRegistrationRequest);

        // Then
        verify(userService).createUser(any(User.class));
        verify(userMapper).toResponse(testUser);
    }

    // ========== Login Tests ==========

    @Test
    @DisplayName("login_shouldReturnAuthResponse_whenValidCredentials")
    void login_shouldReturnAuthResponse_whenValidCredentials() {
        // Given
        String expectedToken = "jwt.token.here";
        Date expirationDate = new Date(System.currentTimeMillis() + 3600000); // 1 hour from now
        long expectedExpiresIn = expirationDate.getTime();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(testAuthentication);
        when(testAuthentication.getPrincipal()).thenReturn(testUserDetails);
        when(jwtTokenService.generateToken(testUserDetails)).thenReturn(expectedToken);
        when(jwtTokenService.extractExpiration(expectedToken)).thenReturn(expirationDate);

        // When
        ResponseEntity<AuthResponse> response = authController.login(testLoginRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo(expectedToken);
        assertThat(response.getBody().getEmail()).isEqualTo("member@test.com");
        assertThat(response.getBody().getRole()).isEqualTo(UserRole.MEMBER);
        assertThat(response.getBody().getExpiresIn()).isEqualTo(expectedExpiresIn);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenService).generateToken(testUserDetails);
        verify(jwtTokenService).extractExpiration(expectedToken);
    }

    @Test
    @DisplayName("login_shouldAuthenticateWithCorrectCredentials_whenLoginRequestProvided")
    void login_shouldAuthenticateWithCorrectCredentials_whenLoginRequestProvided() {
        // Given
        String expectedToken = "jwt.token.here";
        Date expirationDate = new Date(System.currentTimeMillis() + 3600000);
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(testAuthentication);
        when(testAuthentication.getPrincipal()).thenReturn(testUserDetails);
        when(jwtTokenService.generateToken(testUserDetails)).thenReturn(expectedToken);
        when(jwtTokenService.extractExpiration(expectedToken)).thenReturn(expirationDate);

        // When
        authController.login(testLoginRequest);

        // Then
        verify(authenticationManager).authenticate(argThat(token -> 
            "member@test.com".equals(token.getPrincipal()) && 
            "rawPassword123".equals(token.getCredentials())
        ));
    }

    @Test
    @DisplayName("login_shouldGenerateJwtToken_whenAuthenticationSuccessful")
    void login_shouldGenerateJwtToken_whenAuthenticationSuccessful() {
        // Given
        String expectedToken = "jwt.token.here";
        Date expirationDate = new Date(System.currentTimeMillis() + 3600000);
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(testAuthentication);
        when(testAuthentication.getPrincipal()).thenReturn(testUserDetails);
        when(jwtTokenService.generateToken(testUserDetails)).thenReturn(expectedToken);
        when(jwtTokenService.extractExpiration(expectedToken)).thenReturn(expirationDate);

        // When
        authController.login(testLoginRequest);

        // Then
        verify(jwtTokenService).generateToken(testUserDetails);
        verify(jwtTokenService).extractExpiration(expectedToken);
    }

    @Test
    @DisplayName("login_shouldReturnCorrectUserDetails_whenAuthenticationSuccessful")
    void login_shouldReturnCorrectUserDetails_whenAuthenticationSuccessful() {
        // Given
        String expectedToken = "jwt.token.here";
        Date expirationDate = new Date(System.currentTimeMillis() + 3600000);
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(testAuthentication);
        when(testAuthentication.getPrincipal()).thenReturn(testUserDetails);
        when(jwtTokenService.generateToken(testUserDetails)).thenReturn(expectedToken);
        when(jwtTokenService.extractExpiration(expectedToken)).thenReturn(expirationDate);

        // When
        ResponseEntity<AuthResponse> response = authController.login(testLoginRequest);

        // Then
        assertThat(response.getBody().getEmail()).isEqualTo("member@test.com");
        assertThat(response.getBody().getRole()).isEqualTo(UserRole.MEMBER);
    }

    // ========== Edge Cases and Error Scenarios ==========

    @Test
    @DisplayName("register_shouldHandleNullRole_whenRoleNotProvided")
    void register_shouldHandleNullRole_whenRoleNotProvided() {
        // Given
        testRegistrationRequest.setRole(null);
        when(userMapper.toEntity(testRegistrationRequest)).thenReturn(testUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userService.createUser(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        // When
        ResponseEntity<UserResponse> response = authController.register(testRegistrationRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(userService).createUser(any(User.class));
    }

    @Test
    @DisplayName("login_shouldHandleAdminUser_whenAdminCredentialsProvided")
    void login_shouldHandleAdminUser_whenAdminCredentialsProvided() {
        // Given
        User adminUser = TestDataFactory.createDefaultAdminUser();
        CustomUserDetails adminUserDetails = new CustomUserDetails(adminUser);
        LoginRequest adminLoginRequest = new LoginRequest();
        adminLoginRequest.setEmail("admin@test.com");
        adminLoginRequest.setPassword("adminPassword123");

        String expectedToken = "admin.jwt.token";
        Date expirationDate = new Date(System.currentTimeMillis() + 3600000);
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(testAuthentication);
        when(testAuthentication.getPrincipal()).thenReturn(adminUserDetails);
        when(jwtTokenService.generateToken(adminUserDetails)).thenReturn(expectedToken);
        when(jwtTokenService.extractExpiration(expectedToken)).thenReturn(expirationDate);

        // When
        ResponseEntity<AuthResponse> response = authController.login(adminLoginRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getEmail()).isEqualTo("admin@test.com");
        assertThat(response.getBody().getRole()).isEqualTo(UserRole.ADMIN);
    }

    // ========== Integration with Dependencies ==========

    @Test
    @DisplayName("register_shouldUseAllDependenciesCorrectly_whenProcessingRegistration")
    void register_shouldUseAllDependenciesCorrectly_whenProcessingRegistration() {
        // Given
        when(userMapper.toEntity(testRegistrationRequest)).thenReturn(testUser);
        when(passwordEncoder.encode("rawPassword123")).thenReturn("encodedPassword123");
        when(userService.createUser(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        // When
        authController.register(testRegistrationRequest);

        // Then
        verify(userMapper).toEntity(testRegistrationRequest);
        verify(passwordEncoder).encode("rawPassword123");
        verify(userService).createUser(any(User.class));
        verify(userMapper).toResponse(testUser);
        verifyNoMoreInteractions(userMapper, passwordEncoder, userService);
    }

    @Test
    @DisplayName("login_shouldUseAllDependenciesCorrectly_whenProcessingLogin")
    void login_shouldUseAllDependenciesCorrectly_whenProcessingLogin() {
        // Given
        String expectedToken = "jwt.token.here";
        Date expirationDate = new Date(System.currentTimeMillis() + 3600000);
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(testAuthentication);
        when(testAuthentication.getPrincipal()).thenReturn(testUserDetails);
        when(jwtTokenService.generateToken(testUserDetails)).thenReturn(expectedToken);
        when(jwtTokenService.extractExpiration(expectedToken)).thenReturn(expirationDate);

        // When
        authController.login(testLoginRequest);

        // Then
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenService).generateToken(testUserDetails);
        verify(jwtTokenService).extractExpiration(expectedToken);
        verifyNoMoreInteractions(authenticationManager, jwtTokenService);
    }
}
