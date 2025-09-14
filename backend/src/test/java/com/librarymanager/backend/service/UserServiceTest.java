package com.librarymanager.backend.service;

import com.librarymanager.backend.entity.User;
import com.librarymanager.backend.entity.UserRole;
import com.librarymanager.backend.repository.UserRepository;
import com.librarymanager.backend.testutil.TestDataFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService using JUnit 5 + Mockito.
 * 
 * Testing Strategy:
 * - Pure unit tests with mocked dependencies
 * - Focus on business logic validation
 * - Fast execution without Spring context
 * 
 * @author Marcel Pulido
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User testAdmin;

    @BeforeEach
    void setUp() {
        // Test data setup following your existing entity builder pattern
        testUser = User.builder()
            .email("john.doe@example.com")
            .password("hashedPassword123")
            .firstName("John")
            .lastName("Doe")
            .phone("1234567890")
            .role(UserRole.MEMBER)
            .build();

        testAdmin = User.builder()
            .email("admin@library.com")
            .password("adminPass123")
            .firstName("Admin")
            .lastName("User")
            .role(UserRole.ADMIN)
            .build();
    }

    // ========== findById Tests ==========

    @Test
    @DisplayName("Should return user when valid ID provided")
    void findById_ValidId_ReturnsUser() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findById(userId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("john.doe@example.com");
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should return empty when user not found")
    void findById_InvalidId_ReturnsEmpty() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findById(userId);

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findById(userId);
    }

    // ========== findByEmail Tests ==========

    @Test
    @DisplayName("Should return user when valid email provided")
    void findByEmail_ValidEmail_ReturnsUser() {
        // Given
        String email = "john.doe@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByEmail(email);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getFirstName()).isEqualTo("John");
        verify(userRepository).findByEmail(email);
    }

    // ========== findAllMembers Tests ==========

    @Test
    @DisplayName("Should return all members ordered by name")
    void findAllMembers_ReturnsOrderedList() {
        // Given
        List<User> members = Arrays.asList(testUser, 
            TestDataFactory.createCustomUser("jane.smith@example.com", "password", "Jane", "Smith", UserRole.MEMBER));
        when(userRepository.findAllMembersOrderedByName()).thenReturn(members);

        // When
        List<User> result = userService.findAllMembers();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRole()).isEqualTo(UserRole.MEMBER);
        verify(userRepository).findAllMembersOrderedByName();
    }

    // ========== updateUser Tests ==========

    @Test
    @DisplayName("Should update user when user exists")
    void updateUser_ExistingUser_ReturnsUpdatedUser() {
        // Given
        testUser.setId(1L);
        testUser.setFirstName("John Updated");
        
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        User result = userService.updateUser(testUser);

        // Then
        assertThat(result.getFirstName()).isEqualTo("John Updated");
        verify(userRepository).existsById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent user")
    void updateUser_NonExistentUser_ThrowsException() {
        // Given
        testUser.setId(999L);
        when(userRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(testUser))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User with ID 999 does not exist");

        verify(userRepository).existsById(999L);
        verify(userRepository, never()).save(any());
    }

    // ========== Role-based Tests ==========

    @Test
    @DisplayName("Should return users by role")
    void findUsersByRole_ValidRole_ReturnsFilteredUsers() {
        // Given
        List<User> adminUsers = Arrays.asList(testAdmin);
        when(userRepository.findByRole(UserRole.ADMIN)).thenReturn(adminUsers);

        // When
        List<User> result = userService.findUsersByRole(UserRole.ADMIN);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo(UserRole.ADMIN);
        verify(userRepository).findByRole(UserRole.ADMIN);
    }

    @Test
    @DisplayName("Should count users by role correctly")
    void countUsersByRole_ValidRole_ReturnsCount() {
        // Given
        List<User> members = Arrays.asList(testUser, testUser);
        when(userRepository.findByRole(UserRole.MEMBER)).thenReturn(members);

        // When
        long count = userService.countUsersByRole(UserRole.MEMBER);

        // Then
        assertThat(count).isEqualTo(2);
        verify(userRepository).findByRole(UserRole.MEMBER);
    }

    // ========== Search Tests ==========

    @Test
    @DisplayName("Should search users by name")
    void searchUsersByName_ValidTerms_ReturnsMatchingUsers() {
        // Given
        String firstName = "John";
        String lastName = "Doe";
        List<User> searchResults = Arrays.asList(testUser);
        
        when(userRepository.findByNameContainingIgnoreCase(firstName, lastName))
            .thenReturn(searchResults);

        // When
        List<User> result = userService.searchUsersByName(firstName, lastName);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualTo("John");
        verify(userRepository).findByNameContainingIgnoreCase(firstName, lastName);
    }

    // ========== Validation Tests ==========

    @Test
    @DisplayName("Should check if user exists by email")
    void existsByEmail_ExistingEmail_ReturnsTrue() {
        // Given
        String email = "john.doe@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // When
        boolean exists = userService.existsByEmail(email);

        // Then
        assertThat(exists).isTrue();
        verify(userRepository).existsByEmail(email);
    }

    @Test
    @DisplayName("Should count all users")
    void countAllUsers_ReturnsCorrectCount() {
        // Given
        when(userRepository.count()).thenReturn(5L);

        // When
        long count = userService.countAllUsers();

        // Then
        assertThat(count).isEqualTo(5L);
        verify(userRepository).count();
    }
}