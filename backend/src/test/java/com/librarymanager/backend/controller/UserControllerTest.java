package com.librarymanager.backend.controller;

import com.librarymanager.backend.dto.response.UserResponse;
import com.librarymanager.backend.entity.User;
import com.librarymanager.backend.entity.UserRole;
import com.librarymanager.backend.mapper.UserMapper;
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

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserController.
 * 
 * Tests user management endpoints including CRUD operations and search functionality.
 * Follows Spring Boot testing best practices with proper mocking and assertions.
 * 
 * @author Marcel Pulido
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserController userController;

    private User testUser;
    private UserResponse testUserResponse;
    private List<User> testUsers;
    private List<UserResponse> testUserResponses;

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

        testUsers = TestDataFactory.createSampleUsers();
        testUsers.forEach(user -> user.setId((long) (testUsers.indexOf(user) + 1)));

        testUserResponses = Arrays.asList(
            createUserResponse(1L, "john.doe@test.com", "John", "Doe", UserRole.MEMBER),
            createUserResponse(2L, "jane.smith@test.com", "Jane", "Smith", UserRole.MEMBER),
            createUserResponse(3L, "admin.user@test.com", "Admin", "User", UserRole.ADMIN),
            createUserResponse(4L, "bob.wilson@test.com", "Bob", "Wilson", UserRole.MEMBER)
        );
    }

    private UserResponse createUserResponse(Long id, String email, String firstName, String lastName, UserRole role) {
        UserResponse response = new UserResponse();
        response.setId(id);
        response.setEmail(email);
        response.setFirstName(firstName);
        response.setLastName(lastName);
        response.setPhone("5555555555");
        response.setRole(role);
        return response;
    }

    // ========== Get User by ID Tests ==========

    @Test
    @DisplayName("getById_shouldReturnUserResponse_whenValidIdProvided")
    void getById_shouldReturnUserResponse_whenValidIdProvided() {
        // Given
        Long userId = 1L;
        when(userService.findById(userId)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        // When
        ResponseEntity<UserResponse> response = userController.getById(userId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getEmail()).isEqualTo("member@test.com");
        assertThat(response.getBody().getRole()).isEqualTo(UserRole.MEMBER);

        verify(userService).findById(userId);
        verify(userMapper).toResponse(testUser);
    }

    @Test
    @DisplayName("getById_shouldCallUserServiceWithCorrectId_whenIdProvided")
    void getById_shouldCallUserServiceWithCorrectId_whenIdProvided() {
        // Given
        Long userId = 1L;
        when(userService.findById(userId)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        // When
        userController.getById(userId);

        // Then
        verify(userService).findById(userId);
        verify(userMapper).toResponse(testUser);
    }

    // ========== Get All Members Tests ==========

    @Test
    @DisplayName("getMembers_shouldReturnListOfUserResponses_whenMembersExist")
    void getMembers_shouldReturnListOfUserResponses_whenMembersExist() {
        // Given
        List<User> memberUsers = testUsers.stream()
            .filter(user -> user.getRole() == UserRole.MEMBER)
            .toList();
        

        when(userService.findAllMembers()).thenReturn(memberUsers);
        when(userMapper.toResponse(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return testUserResponses.stream()
                .filter(response -> response.getEmail().equals(user.getEmail()))
                .findFirst()
                .orElse(testUserResponse);
        });

        // When
        ResponseEntity<List<UserResponse>> response = userController.getMembers();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(3); // 3 members in test data
        assertThat(response.getBody()).allMatch(userResponse -> userResponse.getRole() == UserRole.MEMBER);

        verify(userService).findAllMembers();
        verify(userMapper, times(3)).toResponse(any(User.class));
    }

    @Test
    @DisplayName("getMembers_shouldReturnEmptyList_whenNoMembersExist")
    void getMembers_shouldReturnEmptyList_whenNoMembersExist() {
        // Given
        when(userService.findAllMembers()).thenReturn(Arrays.asList());

        // When
        ResponseEntity<List<UserResponse>> response = userController.getMembers();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();

        verify(userService).findAllMembers();
        verify(userMapper, never()).toResponse(any(User.class));
    }

    // ========== Search Users Tests ==========

    @Test
    @DisplayName("search_shouldReturnFilteredUsers_whenFirstNameProvided")
    void search_shouldReturnFilteredUsers_whenFirstNameProvided() {
        // Given
        String firstName = "John";
        List<User> filteredUsers = testUsers.stream()
            .filter(user -> user.getFirstName().equals(firstName))
            .toList();
        
        when(userService.searchUsersByName(firstName, null)).thenReturn(filteredUsers);
        when(userMapper.toResponse(any(User.class))).thenReturn(testUserResponse);

        // When
        ResponseEntity<List<UserResponse>> response = userController.search(firstName, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);

        verify(userService).searchUsersByName(firstName, null);
        verify(userMapper).toResponse(any(User.class));
    }

    @Test
    @DisplayName("search_shouldReturnFilteredUsers_whenLastNameProvided")
    void search_shouldReturnFilteredUsers_whenLastNameProvided() {
        // Given
        String lastName = "Smith";
        List<User> filteredUsers = testUsers.stream()
            .filter(user -> user.getLastName().equals(lastName))
            .toList();
        
        when(userService.searchUsersByName(null, lastName)).thenReturn(filteredUsers);
        when(userMapper.toResponse(any(User.class))).thenReturn(testUserResponse);

        // When
        ResponseEntity<List<UserResponse>> response = userController.search(null, lastName);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);

        verify(userService).searchUsersByName(null, lastName);
        verify(userMapper).toResponse(any(User.class));
    }

    @Test
    @DisplayName("search_shouldReturnFilteredUsers_whenBothNamesProvided")
    void search_shouldReturnFilteredUsers_whenBothNamesProvided() {
        // Given
        String firstName = "John";
        String lastName = "Doe";
        List<User> filteredUsers = testUsers.stream()
            .filter(user -> user.getFirstName().equals(firstName) && user.getLastName().equals(lastName))
            .toList();
        
        when(userService.searchUsersByName(firstName, lastName)).thenReturn(filteredUsers);
        when(userMapper.toResponse(any(User.class))).thenReturn(testUserResponse);

        // When
        ResponseEntity<List<UserResponse>> response = userController.search(firstName, lastName);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);

        verify(userService).searchUsersByName(firstName, lastName);
        verify(userMapper).toResponse(any(User.class));
    }

    // ========== Find Users by Role Tests ==========

    @Test
    @DisplayName("findByRole_shouldReturnUsersWithSpecificRole_whenRoleProvided")
    void findByRole_shouldReturnUsersWithSpecificRole_whenRoleProvided() {
        // Given
        UserRole role = UserRole.ADMIN;
        List<User> adminUsers = testUsers.stream()
            .filter(user -> user.getRole() == role)
            .toList();
        

        when(userService.findUsersByRole(role)).thenReturn(adminUsers);
        when(userMapper.toResponse(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return testUserResponses.stream()
                .filter(response -> response.getEmail().equals(user.getEmail()))
                .findFirst()
                .orElse(testUserResponse);
        });

        // When
        ResponseEntity<List<UserResponse>> response = userController.findByRole(role);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()).allMatch(userResponse -> userResponse.getRole() == UserRole.ADMIN);

        verify(userService).findUsersByRole(role);
        verify(userMapper).toResponse(any(User.class));
    }

    @Test
    @DisplayName("findByRole_shouldReturnEmptyList_whenNoUsersWithRoleExist")
    void findByRole_shouldReturnEmptyList_whenNoUsersWithRoleExist() {
        // Given
        UserRole role = UserRole.ADMIN;
        when(userService.findUsersByRole(role)).thenReturn(Arrays.asList());

        // When
        ResponseEntity<List<UserResponse>> response = userController.findByRole(role);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();

        verify(userService).findUsersByRole(role);
        verify(userMapper, never()).toResponse(any(User.class));
    }

    // ========== Update User Tests ==========

    @Test
    @DisplayName("update_shouldReturnUpdatedUserResponse_whenValidUpdateProvided")
    void update_shouldReturnUpdatedUserResponse_whenValidUpdateProvided() {
        // Given
        Long userId = 1L;
        UserResponse updateRequest = new UserResponse();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Name");
        updateRequest.setPhone("9999999999");
        updateRequest.setRole(UserRole.ADMIN);

        User updatedUser = TestDataFactory.createDefaultMemberUser();
        updatedUser.setId(userId);
        updatedUser.setFirstName("Updated");
        updatedUser.setLastName("Name");
        updatedUser.setPhone("9999999999");
        updatedUser.setRole(UserRole.ADMIN);

        UserResponse updatedResponse = new UserResponse();
        updatedResponse.setId(userId);
        updatedResponse.setEmail("member@test.com");
        updatedResponse.setFirstName("Updated");
        updatedResponse.setLastName("Name");
        updatedResponse.setPhone("9999999999");
        updatedResponse.setRole(UserRole.ADMIN);

        when(userService.findById(userId)).thenReturn(testUser);
        when(userService.updateUser(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toResponse(updatedUser)).thenReturn(updatedResponse);

        // When
        ResponseEntity<UserResponse> response = userController.update(userId, updateRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFirstName()).isEqualTo("Updated");
        assertThat(response.getBody().getLastName()).isEqualTo("Name");
        assertThat(response.getBody().getPhone()).isEqualTo("9999999999");
        assertThat(response.getBody().getRole()).isEqualTo(UserRole.ADMIN);

        verify(userService).findById(userId);
        verify(userService).updateUser(any(User.class));
        verify(userMapper).toResponse(updatedUser);
    }

    @Test
    @DisplayName("update_shouldUpdateCorrectFields_whenUpdateRequestProvided")
    void update_shouldUpdateCorrectFields_whenUpdateRequestProvided() {
        // Given
        Long userId = 1L;
        UserResponse updateRequest = new UserResponse();
        updateRequest.setFirstName("NewFirstName");
        updateRequest.setLastName("NewLastName");
        updateRequest.setPhone("1111111111");
        updateRequest.setRole(UserRole.MEMBER);

        when(userService.findById(userId)).thenReturn(testUser);
        when(userService.updateUser(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        // When
        userController.update(userId, updateRequest);

        // Then
        verify(userService).updateUser(argThat(user -> 
            "NewFirstName".equals(user.getFirstName()) &&
            "NewLastName".equals(user.getLastName()) &&
            "1111111111".equals(user.getPhone()) &&
            UserRole.MEMBER == user.getRole()
        ));
    }

    // ========== Count Tests ==========

    @Test
    @DisplayName("countAll_shouldReturnTotalUserCount_whenCalled")
    void countAll_shouldReturnTotalUserCount_whenCalled() {
        // Given
        long expectedCount = 4L;
        when(userService.countAllUsers()).thenReturn(expectedCount);

        // When
        ResponseEntity<Long> response = userController.countAll();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedCount);

        verify(userService).countAllUsers();
    }

    @Test
    @DisplayName("countByRole_shouldReturnCountForSpecificRole_whenRoleProvided")
    void countByRole_shouldReturnCountForSpecificRole_whenRoleProvided() {
        // Given
        UserRole role = UserRole.MEMBER;
        long expectedCount = 3L;
        when(userService.countUsersByRole(role)).thenReturn(expectedCount);

        // When
        ResponseEntity<Long> response = userController.countByRole(role);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedCount);

        verify(userService).countUsersByRole(role);
    }

    // ========== Edge Cases and Error Scenarios ==========

    @Test
    @DisplayName("getById_shouldHandleNonExistentUser_whenInvalidIdProvided")
    void getById_shouldHandleNonExistentUser_whenInvalidIdProvided() {
        // Given
        Long invalidId = 999L;
        when(userService.findById(invalidId)).thenThrow(new RuntimeException("User not found"));

        // When & Then
        try {
            userController.getById(invalidId);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("User not found");
        }

        verify(userService).findById(invalidId);
        verify(userMapper, never()).toResponse(any(User.class));
    }

    @Test
    @DisplayName("update_shouldPreserveEmail_whenUpdatingUser")
    void update_shouldPreserveEmail_whenUpdatingUser() {
        // Given
        Long userId = 1L;
        UserResponse updateRequest = new UserResponse();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Name");

        when(userService.findById(userId)).thenReturn(testUser);
        when(userService.updateUser(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        // When
        userController.update(userId, updateRequest);

        // Then
        verify(userService).updateUser(argThat(user -> 
            "member@test.com".equals(user.getEmail()) // Email should be preserved
        ));
    }

    // ========== Integration with Dependencies ==========

    @Test
    @DisplayName("getMembers_shouldUseAllDependenciesCorrectly_whenProcessingRequest")
    void getMembers_shouldUseAllDependenciesCorrectly_whenProcessingRequest() {
        // Given
        List<User> memberUsers = testUsers.stream()
            .filter(user -> user.getRole() == UserRole.MEMBER)
            .toList();
        
        when(userService.findAllMembers()).thenReturn(memberUsers);
        when(userMapper.toResponse(any(User.class))).thenReturn(testUserResponse);

        // When
        userController.getMembers();

        // Then
        verify(userService).findAllMembers();
        verify(userMapper, times(memberUsers.size())).toResponse(any(User.class));
        verifyNoMoreInteractions(userService, userMapper);
    }

    @Test
    @DisplayName("search_shouldUseAllDependenciesCorrectly_whenProcessingSearchRequest")
    void search_shouldUseAllDependenciesCorrectly_whenProcessingSearchRequest() {
        // Given
        String firstName = "John";
        String lastName = "Doe";
        List<User> searchResults = testUsers.stream()
            .filter(user -> user.getFirstName().equals(firstName) && user.getLastName().equals(lastName))
            .toList();
        
        when(userService.searchUsersByName(firstName, lastName)).thenReturn(searchResults);
        when(userMapper.toResponse(any(User.class))).thenReturn(testUserResponse);

        // When
        userController.search(firstName, lastName);

        // Then
        verify(userService).searchUsersByName(firstName, lastName);
        verify(userMapper, times(searchResults.size())).toResponse(any(User.class));
        verifyNoMoreInteractions(userService, userMapper);
    }
}
