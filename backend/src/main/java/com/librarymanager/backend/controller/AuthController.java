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
import org.springframework.web.bind.annotation.*;

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

    public AuthController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    /**
     * Registers a new user.
     * Returns 201 Created with the created user (without sensitive fields).
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        log.info("Registering user with email: {}", request.getEmail());

        if (userService.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User user = userMapper.toEntity(request);
        User saved = userService.createUser(user);
        UserResponse response = userMapper.toResponse(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates a user and returns an auth token response.
     * Note: Token generation is not yet implemented in this project; this returns a stub.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        return userService.authenticate(request.getEmail(), request.getPassword())
            .map(u -> {
                AuthResponse auth = AuthResponse.builder()
                    .token("stub-token")
                    .email(u.getEmail())
                    .role(u.getRole())
                    .expiresIn(3600L)
                    .build();
                return ResponseEntity.ok(auth);
            })
            .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}


