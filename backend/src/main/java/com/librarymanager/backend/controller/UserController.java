package com.librarymanager.backend.controller;

import com.librarymanager.backend.dto.response.UserResponse;
import com.librarymanager.backend.entity.User;
import com.librarymanager.backend.entity.UserRole;
import com.librarymanager.backend.mapper.UserMapper;
import com.librarymanager.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for user operations other than registration and login.
 *
 * Routes are grouped under /api/users following Spring REST conventions.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        User user = userService.findById(id);
        UserResponse response = userMapper.toResponse(user);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getMembers() {
        List<UserResponse> users = userService.findAllMembers().stream()
            .map(userMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> search(
        @RequestParam(required = false) String firstName,
        @RequestParam(required = false) String lastName
    ) {
        List<UserResponse> users = userService.searchUsersByName(firstName, lastName).stream()
            .map(userMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> findByRole(@PathVariable UserRole role) {
        List<UserResponse> users = userService.findUsersByRole(role).stream()
            .map(userMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserResponse update) {
        User existing = userService.findById(id);
        existing.setFirstName(update.getFirstName());
        existing.setLastName(update.getLastName());
        existing.setPhone(update.getPhone());
        existing.setRole(update.getRole());
        User saved = userService.updateUser(existing);
        return ResponseEntity.ok(userMapper.toResponse(saved));
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> countAll() {
        return ResponseEntity.ok(userService.countAllUsers());
    }

    @GetMapping("/count/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> countByRole(@PathVariable UserRole role) {
        return ResponseEntity.ok(userService.countUsersByRole(role));
    }
}


