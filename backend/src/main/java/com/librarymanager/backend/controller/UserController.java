package com.librarymanager.backend.controller;

import com.librarymanager.backend.dto.response.UserResponse;
import com.librarymanager.backend.entity.User;
import com.librarymanager.backend.entity.UserRole;
import com.librarymanager.backend.mapper.UserMapper;
import com.librarymanager.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
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
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        Optional<User> userOpt = userService.findById(id);
        return userOpt
            .map(userMapper::toResponse)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getMembers() {
        List<UserResponse> users = userService.findAllMembers().stream()
            .map(userMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/search")
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
    public ResponseEntity<List<UserResponse>> findByRole(@PathVariable UserRole role) {
        List<UserResponse> users = userService.findUsersByRole(role).stream()
            .map(userMapper::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserResponse update) {
        Optional<User> existing = userService.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        User toUpdate = existing.get();
        toUpdate.setFirstName(update.getFirstName());
        toUpdate.setLastName(update.getLastName());
        toUpdate.setPhone(update.getPhone());
        toUpdate.setRole(update.getRole());
        User saved = userService.updateUser(toUpdate);
        return ResponseEntity.ok(userMapper.toResponse(saved));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countAll() {
        return ResponseEntity.ok(userService.countAllUsers());
    }

    @GetMapping("/count/{role}")
    public ResponseEntity<Long> countByRole(@PathVariable UserRole role) {
        return ResponseEntity.ok(userService.countUsersByRole(role));
    }
}


