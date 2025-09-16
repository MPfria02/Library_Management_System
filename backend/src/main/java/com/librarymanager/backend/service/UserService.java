package com.librarymanager.backend.service;

import com.librarymanager.backend.entity.User;
import com.librarymanager.backend.entity.UserRole;
import com.librarymanager.backend.exception.AuthenticationException;
import com.librarymanager.backend.exception.DuplicateResourceException;
import com.librarymanager.backend.exception.ResourceNotFoundException;
import com.librarymanager.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for User management operations.
 * 
 * This service provides business logic for user operations including:
 * - User retrieval and search
 * - User profile management
 * - Role-based operations
 * 
 * @author Marcel Pulido
 * @version 1.0
 */
@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;

    /**
     * Constructor injection for UserRepository.
     * 
     * @param userRepository the user repository dependency
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retrieves a user by their ID.
     * 
     * @param id the user ID
     * @return Optional containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        log.debug("Finding user by ID: {}", id);
        return userRepository.findById(id);
    }

    /**
     * Retrieves a user by their email address.
     * 
     * @param email the user's email
     * @return Optional containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * Creates a new user after validating unique email.
     *
     * @param user user entity to persist
     * @return saved user
     * @throws DuplicateResourceException when email already exists
     */
    public User createUser(User user) {
        log.info("Creating user with email: {}", user.getEmail());
        if (userRepository.existsByEmail(user.getEmail())) {
            throw DuplicateResourceException.forUserEmail(user.getEmail());
        }
        return userRepository.save(user);
    }

    /**
     * Retrieves all members ordered by last name, first name.
     * 
     * @return List of member users
     */
    @Transactional(readOnly = true)
    public List<User> findAllMembers() {
        log.debug("Retrieving all members ordered by name");
        return userRepository.findAllMembersOrderedByName();
    }

    /**
     * Searches users by name (first name or last name containing the search term).
     * 
     * @param firstname the term to search for in first names
     * @param lastname the term to search for in last names
     * @return List of users matching the search term
     */
    @Transactional(readOnly = true)
    public List<User> searchUsersByName(String firstname, String lastname) {
        log.debug("Searching users by name: {} {}", firstname, lastname);
        return userRepository.findByNameContainingIgnoreCase(firstname, lastname);
    }

    /**
     * Checks if a user exists with the given email.
     * 
     * @param email the email to check
     * @return true if user exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        log.debug("Checking if user exists with email: {}", email);
        return userRepository.existsByEmail(email);
    }

    /**
     * Authenticates a user by email and password.
     *
     * Note: Plain-text password comparison is used here because
     * password hashing/security layer is not implemented yet.
     *
     * @param email user email
     * @param password user password
     * @return Optional user when credentials are valid
     * @throws AuthenticationException when email or password is invalid
     */
    @Transactional(readOnly = true)
    public Optional<User> authenticate(String email, String password) {
        log.debug("Authenticating user by email: {}", email);
        if (email == null || !findByEmail(email).isPresent()) {
            throw AuthenticationException.invalidEmail(email);
        }
        if (password == null || !findByEmail(email).get().getPassword().equals(password)) {
            throw AuthenticationException.invalidPassword(password);
        }
        return findByEmail(email);
    }

    /**
     * Retrieves users by their role.
     * 
     * @param role the user role to filter by
     * @return List of users with the specified role
     */
    @Transactional(readOnly = true)
    public List<User> findUsersByRole(UserRole role) {
        log.debug("Finding users by role: {}", role);
        return userRepository.findByRole(role);
    }

    /**
     * Updates user information.
     * Note: This method doesn't handle password updates for security reasons.
     * 
     * @param user the user with updated information
     * @return the updated user
     * @throws ResourceNotFoundException when user doesn't exist
     */
    public User updateUser(User user) {
        log.info("Updating user with ID: {}", user.getId());
        
        // Verify user exists before updating
        if (!userRepository.existsById(user.getId())) {
            log.warn("Attempted to update non-existent user with ID: {}", user.getId());
            throw ResourceNotFoundException.forUser(user.getId());
        }
        
        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", updatedUser.getEmail());
        return updatedUser;
    }

    /**
     * Counts total number of users.
     * 
     * @return total user count
     */
    @Transactional(readOnly = true)
    public long countAllUsers() {
        log.debug("Counting all users");
        return userRepository.count();
    }

    /**
     * Counts users by role.
     * 
     * @param role the role to count
     * @return number of users with specified role
     */
    @Transactional(readOnly = true)
    public long countUsersByRole(UserRole role) {
        log.debug("Counting users by role: {}", role);
        return userRepository.findByRole(role).size();
    }
}