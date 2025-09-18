package com.librarymanager.backend.controller;

import com.librarymanager.backend.dto.request.LoginRequest;
import com.librarymanager.backend.dto.request.UserRegistrationRequest;
import com.librarymanager.backend.dto.response.AuthResponse;
import com.librarymanager.backend.dto.response.UserResponse;
import com.librarymanager.backend.entity.User;
import com.librarymanager.backend.mapper.UserMapper;
import com.librarymanager.backend.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.librarymanager.backend.security.CustomUserDetails;
import com.librarymanager.backend.security.JwtTokenService;

/**
 * Authentication controller exposing endpoints for user registration and login.
 *
 * Best practices followed:
 * - Request DTO validation using jakarta.validation
 * - Clear, RESTful routes under /api/auth
 * - Proper HTTP status codes and ResponseEntity wrappers
 * - No sensitive data in logs
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, UserMapper userMapper,
                          AuthenticationManager authenticationManager,
                          JwtTokenService jwtTokenService,
                          PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user.
     * Returns 201 Created with the created user (without sensitive fields).
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        log.info("Registering user with email: {}", request.getEmail());

        User user = userMapper.toEntity(request);
        // Encode the raw password before persisting
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User saved = userService.createUser(user);
        UserResponse response = userMapper.toResponse(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates a user and returns an auth token response.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("Login attempt for email: {}", request.getEmail());

        // Will throw BadCredentialsException on invalid login -> handled by RestAuthenticationEntryPoint
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String token = jwtTokenService.generateToken(userDetails);
        long timeInMillis = jwtTokenService.extractExpiration(token).getTime();


        AuthResponse auth = AuthResponse.builder()
            .token(token)
            .email(userDetails.getEmail())
            .role(userDetails.getRole())
            .expiresIn(timeInMillis)
            .build();

        return ResponseEntity.ok(auth);
    }

}


