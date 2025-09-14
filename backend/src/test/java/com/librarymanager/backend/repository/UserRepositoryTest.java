package com.librarymanager.backend.repository;

import com.librarymanager.backend.entity.User;
import com.librarymanager.backend.entity.UserRole;
import com.librarymanager.backend.testutil.TestDataFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for UserRepository using @DataJpaTest.
 * 
 * Testing Strategy:
 * - Real database operations with H2
 * - Custom JPQL queries validation
 * - Entity constraints and validations
 * - Database relationships and mappings
 * 
 * @author Marcel Pulido
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Integration Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User memberUser1;
    private User memberUser2;
    private User adminUser;

    @BeforeEach
    void setUp() {
        // Clear existing data
        entityManager.clear();

        // Create test users using your builder pattern
        memberUser1 = User.builder()
            .email("john.doe@example.com")
            .password("hashedPassword123")
            .firstName("John")
            .lastName("Doe")
            .phone("1234567890")
            .role(UserRole.MEMBER)
            .build();

        memberUser2 = User.builder()
            .email("jane.smith@example.com")
            .password("hashedPassword456")
            .firstName("Jane")
            .lastName("Smith")
            .phone("0987654321")
            .role(UserRole.MEMBER)
            .build();

        adminUser = User.builder()
            .email("admin@library.com")
            .password("adminPassword789")
            .firstName("Admin")
            .lastName("User")
            .phone("5555555555")
            .role(UserRole.ADMIN)
            .build();

        // Persist test data
        entityManager.persist(memberUser1);
        entityManager.persist(memberUser2);
        entityManager.persist(adminUser);
        entityManager.flush();
    }

    // ========== Basic Spring Data JPA Methods ==========

    @Test
    @DisplayName("Should find user by email")
    void findByEmail_ExistingEmail_ReturnsUser() {
        // When
        Optional<User> result = userRepository.findByEmail("john.doe@example.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getFirstName()).isEqualTo("John");
        assertThat(result.get().getLastName()).isEqualTo("Doe");
        assertThat(result.get().getRole()).isEqualTo(UserRole.MEMBER);
    }

    @Test
    @DisplayName("Should return empty when email not found")
    void findByEmail_NonExistentEmail_ReturnsEmpty() {
        // When
        Optional<User> result = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should check if user exists by email")
    void existsByEmail_ExistingEmail_ReturnsTrue() {
        // When
        boolean result = userRepository.existsByEmail("jane.smith@example.com");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when email doesn't exist")
    void existsByEmail_NonExistentEmail_ReturnsFalse() {
        // When
        boolean result = userRepository.existsByEmail("missing@example.com");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should find users by role")
    void findByRole_ExistingRole_ReturnsUsersWithRole() {
        // When
        List<User> members = userRepository.findByRole(UserRole.MEMBER);
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);

        // Then
        assertThat(members).hasSize(2);
        assertThat(members).extracting(User::getRole).containsOnly(UserRole.MEMBER);
        assertThat(members).extracting(User::getEmail)
            .containsExactlyInAnyOrder("john.doe@example.com", "jane.smith@example.com");

        assertThat(admins).hasSize(1);
        assertThat(admins.get(0).getEmail()).isEqualTo("admin@library.com");
    }

    // ========== Custom JPQL Query Tests ==========

    @Test
    @DisplayName("Should find all members ordered by name")
    void findAllMembersOrderedByName_ReturnsOrderedMembers() {
        // When
        List<User> result = userRepository.findAllMembersOrderedByName();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(User::getRole).containsOnly(UserRole.MEMBER);
        
        // Verify ordering: Doe comes before Smith
        assertThat(result.get(0).getLastName()).isEqualTo("Doe");
        assertThat(result.get(1).getLastName()).isEqualTo("Smith");
    }

    @Test
    @DisplayName("Should exclude admins from members query")
    void findAllMembersOrderedByName_ExcludesAdmins() {
        // When
        List<User> result = userRepository.findAllMembersOrderedByName();

        // Then
        assertThat(result).extracting(User::getEmail)
            .doesNotContain("admin@library.com");
        assertThat(result).allMatch(user -> user.getRole() == UserRole.MEMBER);
    }

    @Test
    @DisplayName("Should find users by name containing (case-insensitive)")
    void findByNameContainingIgnoreCase_MatchingNames_ReturnsUsers() {
        // When
        List<User> johnResults = userRepository.findByNameContainingIgnoreCase("john", "");
        List<User> smithResults = userRepository.findByNameContainingIgnoreCase("", "smith");
        List<User> adminResults = userRepository.findByNameContainingIgnoreCase("admin", "user");

        // Then
        assertThat(johnResults).hasSize(1);
        assertThat(johnResults.get(0).getFirstName()).isEqualTo("John");

        assertThat(smithResults).hasSize(1);
        assertThat(smithResults.get(0).getLastName()).isEqualTo("Smith");

        assertThat(adminResults).hasSize(1);
        assertThat(adminResults.get(0).getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("Should handle case-insensitive search correctly")
    void findByNameContainingIgnoreCase_CaseInsensitive_ReturnsMatches() {
        // When
        List<User> upperCaseResults = userRepository.findByNameContainingIgnoreCase("JOHN", "");
        List<User> lowerCaseResults = userRepository.findByNameContainingIgnoreCase("john", "");
        List<User> mixedCaseResults = userRepository.findByNameContainingIgnoreCase("JoHn", "");

        // Then
        assertThat(upperCaseResults).hasSize(1);
        assertThat(lowerCaseResults).hasSize(1);
        assertThat(mixedCaseResults).hasSize(1);
        
        assertThat(upperCaseResults.get(0).getFirstName()).isEqualTo("John");
        assertThat(lowerCaseResults.get(0).getFirstName()).isEqualTo("John");
        assertThat(mixedCaseResults.get(0).getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should return empty list when no names match")
    void findByNameContainingIgnoreCase_NoMatches_ReturnsEmptyList() {
        // When
        List<User> result = userRepository.findByNameContainingIgnoreCase("nonexistent", "name");

        // Then
        assertThat(result).isEmpty();
    }

    // ========== Entity Constraint Tests ==========

    @Test
    @DisplayName("Should enforce email uniqueness constraint")
    void save_DuplicateEmail_ThrowsException() {
        // Given
        User duplicateEmailUser = TestDataFactory.createCustomUser(
            "john.doe@example.com", // same as memberUser1
            "differentPassword", 
            "Different",
            "User",
            UserRole.MEMBER);
    
        // When & Then
        assertThatThrownBy(() -> {
            entityManager.persist(duplicateEmailUser);
            entityManager.flush();
        }).isInstanceOf(Exception.class); // H2 will throw constraint violation
    }

    @Test
    @DisplayName("Should handle timestamp fields correctly")
    void save_NewUser_AutoGeneratesTimestamps() {
        // Given
        User newUser = TestDataFactory.createCustomUser(
            "new.user@example.com", "newPassword", "New", "User", UserRole.MEMBER);
        
        // When
        User savedUser = userRepository.save(newUser);
        entityManager.flush();

        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update timestamps on modification")
    void save_UpdateExistingUser_UpdatesTimestamp() throws InterruptedException {
        // Given
        User originalUser = userRepository.save(TestDataFactory.createCustomUser(
            "update.test@example.com", "originalPassword", "Original", "Name", UserRole.MEMBER));
        entityManager.flush();


        // Small delay to ensure timestamp difference
        Thread.sleep(10);

        // When
        originalUser.setFirstName("Updated");
        User updatedUser = userRepository.save(originalUser);
        entityManager.flush();

        // Then
        assertThat(updatedUser.getFirstName()).isEqualTo("Updated");
        assertThat(updatedUser.getUpdatedAt()).isAfter(updatedUser.getCreatedAt());
    }

    // ========== Default Value Tests ==========

    @Test
    @DisplayName("Should set default role as MEMBER when not specified")
    void save_NoRoleSpecified_DefaultsToMember() {
        // Given
        User userWithoutRole = User.builder()
            .email("default.role@example.com")
            .password("password")
            .firstName("Default")
            .lastName("Role")
            // .role() not specified, should default to MEMBER
            .build();

        // When
        User savedUser = userRepository.save(userWithoutRole);
        entityManager.flush();

        // Then
        assertThat(savedUser.getRole()).isEqualTo(UserRole.MEMBER);
    }

    // ========== Complex Query Scenarios ==========

    @Test
    @DisplayName("Should handle multiple users with similar names")
    void complexNameSearch_MultipleMatches_ReturnsAllMatches() {
        // Given - Add more users with similar names
        User johnSmith = TestDataFactory.createCustomUser(
            "john.smith@example.com", "password", "John", "Smith", UserRole.MEMBER);

        User janeJohnson = TestDataFactory.createCustomUser(
            "jane.johnson@example.com", "password", "Jane", "Johnson", UserRole.MEMBER);

        entityManager.persist(johnSmith);
        entityManager.persist(janeJohnson);
        entityManager.flush();

        // When
        List<User> johnResults = userRepository.findByNameContainingIgnoreCase("john", "");

        // Then
        assertThat(johnResults).hasSize(2); // Original John Doe + John Smith 
        assertThat(johnResults).extracting(User::getFirstName)
            .contains("John", "John"); // Jane Johnson does not match
    }

    @Test
    @DisplayName("Should maintain data integrity across operations")
    void dataIntegrityTest_MultipleOperations_MaintainsConsistency() {
        // When - Perform multiple operations
        long initialCount = userRepository.count();
        
        // Add a user
        User newUser = userRepository.save(TestDataFactory.createCustomUser(
            "integrity.test@example.com", "password", "Integrity", "Test", UserRole.MEMBER));

        long afterAddCount = userRepository.count();
        
        // Find the user
        Optional<User> foundUser = userRepository.findByEmail("integrity.test@example.com");
        
        // Update the user
        newUser.setFirstName("Updated");
        User updatedUser = userRepository.save(newUser);
        
        // Delete the user
        userRepository.delete(newUser);
        long afterDeleteCount = userRepository.count();

        // Then
        assertThat(afterAddCount).isEqualTo(initialCount + 1);
        assertThat(foundUser).isPresent();
        assertThat(updatedUser.getFirstName()).isEqualTo("Updated");
        assertThat(afterDeleteCount).isEqualTo(initialCount);
    }
}