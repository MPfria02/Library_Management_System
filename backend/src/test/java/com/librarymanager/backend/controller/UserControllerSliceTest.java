package com.librarymanager.backend.controller;

import com.librarymanager.backend.dto.response.UserResponse;
import com.librarymanager.backend.entity.User;
import com.librarymanager.backend.entity.UserRole;
import com.librarymanager.backend.exception.ResourceNotFoundException;
import com.librarymanager.backend.mapper.UserMapper;
import com.librarymanager.backend.security.JwtAuthenticationFilter;
import com.librarymanager.backend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for UserController using @WebMvcTest.
 * 
 * Tests the web layer in isolation, focusing on HTTP behavior,
 * request/response mapping, and exception handling for user management operations.
 * 
 * @author Marcel Pulido
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false) 
public class UserControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ========== Get User by ID Tests ==========

    @Test
    public void shouldReturn200WhenUserIsFoundById() throws Exception {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("john.doe@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPhone("1234567890");
        user.setRole(UserRole.MEMBER);
        
        UserResponse response = new UserResponse(1L, "john.doe@example.com", "John", "Doe", "1234567890", UserRole.MEMBER);

        given(userService.findById(1L)).willReturn(user);
        given(userMapper.toResponse(any(User.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.phone").value("1234567890"))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    public void shouldReturn404WhenUserNotFoundById() throws Exception {
        // Arrange
        willThrow(new ResourceNotFoundException("User not found with ID: 999", "USER_NOT_FOUND"))
                .given(userService).findById(999L);

        // Act & Assert
        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with ID: 999"))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    public void shouldReturn400WhenInvalidUserIdIsProvided() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/users/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
    }

    // ========== Get All Members Tests ==========

    @Test
    public void shouldReturn200WhenGettingAllMembers() throws Exception {
        // Arrange
        User user1 = new User();
        user1.setId(1L);
        user1.setEmail("member1@example.com");
        user1.setFirstName("Member");
        user1.setLastName("One");
        user1.setPhone("1111111111");
        user1.setRole(UserRole.MEMBER);
        
        User user2 = new User();
        user2.setId(2L);
        user2.setEmail("member2@example.com");
        user2.setFirstName("Member");
        user2.setLastName("Two");
        user2.setPhone("2222222222");
        user2.setRole(UserRole.MEMBER);
        
        List<User> members = Arrays.asList(user1, user2);
        
        UserResponse response1 = new UserResponse(1L, "member1@example.com", "Member", "One", "1111111111", UserRole.MEMBER);
        UserResponse response2 = new UserResponse(2L, "member2@example.com", "Member", "Two", "2222222222", UserRole.MEMBER);

        given(userService.findAllMembers()).willReturn(members);
        given(userMapper.toResponse(user1)).willReturn(response1);
        given(userMapper.toResponse(user2)).willReturn(response2);

        // Act & Assert
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].email").value("member1@example.com"))
                .andExpect(jsonPath("$[0].firstName").value("Member"))
                .andExpect(jsonPath("$[0].lastName").value("One"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].email").value("member2@example.com"))
                .andExpect(jsonPath("$[1].firstName").value("Member"))
                .andExpect(jsonPath("$[1].lastName").value("Two"));
    }

    @Test
    public void shouldReturn200WhenNoMembersExist() throws Exception {
        // Arrange
        List<User> emptyList = Arrays.asList();
        given(userService.findAllMembers()).willReturn(emptyList);

        // Act & Assert
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ========== Search Users Tests ==========

    @Test
    public void shouldReturn200WhenSearchingUsersByName() throws Exception {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("john.doe@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPhone("1234567890");
        user.setRole(UserRole.MEMBER);
        
        List<User> searchResults = Arrays.asList(user);
        
        UserResponse response = new UserResponse(1L, "john.doe@example.com", "John", "Doe", "1234567890", UserRole.MEMBER);

        given(userService.searchUsersByName("John", "Doe")).willReturn(searchResults);
        given(userMapper.toResponse(any(User.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("firstName", "John")
                .param("lastName", "Doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].lastName").value("Doe"));
    }

    @Test
    public void shouldReturn200WhenSearchingUsersByFirstNameOnly() throws Exception {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("john.smith@example.com");
        user.setFirstName("John");
        user.setLastName("Smith");
        user.setPhone("1234567890");
        user.setRole(UserRole.MEMBER);
        
        List<User> searchResults = Arrays.asList(user);
        
        UserResponse response = new UserResponse(1L, "john.smith@example.com", "John", "Smith", "1234567890", UserRole.MEMBER);

        given(userService.searchUsersByName("John", null)).willReturn(searchResults);
        given(userMapper.toResponse(any(User.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("firstName", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].firstName").value("John"));
    }

    @Test
    public void shouldReturn200WhenSearchingUsersByLastNameOnly() throws Exception {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("jane.doe@example.com");
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setPhone("1234567890");
        user.setRole(UserRole.MEMBER);
        
        List<User> searchResults = Arrays.asList(user);
        
        UserResponse response = new UserResponse(1L, "jane.doe@example.com", "Jane", "Doe", "1234567890", UserRole.MEMBER);

        given(userService.searchUsersByName(null, "Doe")).willReturn(searchResults);
        given(userMapper.toResponse(any(User.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("lastName", "Doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].lastName").value("Doe"));
    }

    @Test
    public void shouldReturn200WhenNoSearchResultsFound() throws Exception {
        // Arrange
        List<User> emptyList = Arrays.asList();
        given(userService.searchUsersByName("NonExistent", "User")).willReturn(emptyList);

        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                .param("firstName", "NonExistent")
                .param("lastName", "User"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ========== Find Users by Role Tests ==========

    @Test
    public void shouldReturn200WhenFindingUsersByRole() throws Exception {
        // Arrange
        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@example.com");
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setPhone("1234567890");
        admin.setRole(UserRole.ADMIN);
        
        List<User> admins = Arrays.asList(admin);
        
        UserResponse response = new UserResponse(1L, "admin@example.com", "Admin", "User", "1234567890", UserRole.ADMIN);

        given(userService.findUsersByRole(UserRole.ADMIN)).willReturn(admins);
        given(userMapper.toResponse(any(User.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/users/role/ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[0].firstName").value("Admin"));
    }

    @Test
    public void shouldReturn200WhenFindingMembersByRole() throws Exception {
        // Arrange
        User member1 = new User();
        member1.setId(1L);
        member1.setEmail("member1@example.com");
        member1.setFirstName("Member");
        member1.setLastName("One");
        member1.setPhone("1111111111");
        member1.setRole(UserRole.MEMBER);
        
        User member2 = new User();
        member2.setId(2L);
        member2.setEmail("member2@example.com");
        member2.setFirstName("Member");
        member2.setLastName("Two");
        member2.setPhone("2222222222");
        member2.setRole(UserRole.MEMBER);
        
        List<User> members = Arrays.asList(member1, member2);
        
        UserResponse response1 = new UserResponse(1L, "member1@example.com", "Member", "One", "1111111111", UserRole.MEMBER);
        UserResponse response2 = new UserResponse(2L, "member2@example.com", "Member", "Two", "2222222222", UserRole.MEMBER);

        given(userService.findUsersByRole(UserRole.MEMBER)).willReturn(members);
        given(userMapper.toResponse(member1)).willReturn(response1);
        given(userMapper.toResponse(member2)).willReturn(response2);

        // Act & Assert
        mockMvc.perform(get("/api/users/role/MEMBER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("MEMBER"))
                .andExpect(jsonPath("$[1].role").value("MEMBER"));
    }

    @Test
    public void shouldReturn400WhenInvalidRoleIsProvided() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/users/role/INVALID_ROLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
    }

    // ========== Update User Tests ==========

    @Test
    public void shouldReturn200WhenUserIsUpdated() throws Exception {
        // Arrange
        UserResponse updateRequest = new UserResponse();
        updateRequest.setFirstName("Updated John");
        updateRequest.setLastName("Updated Doe");
        updateRequest.setPhone("9876543210");
        updateRequest.setRole(UserRole.ADMIN);
        
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("john.doe@example.com");
        existingUser.setFirstName("John");
        existingUser.setLastName("Doe");
        existingUser.setPhone("1234567890");
        existingUser.setRole(UserRole.MEMBER);
        
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setEmail("john.doe@example.com");
        updatedUser.setFirstName("Updated John");
        updatedUser.setLastName("Updated Doe");
        updatedUser.setPhone("9876543210");
        updatedUser.setRole(UserRole.ADMIN);
        
        UserResponse response = new UserResponse(1L, "john.doe@example.com", "Updated John", "Updated Doe", "9876543210", UserRole.ADMIN);

        given(userService.findById(1L)).willReturn(existingUser);
        given(userService.updateUser(any(User.class))).willReturn(updatedUser);
        given(userMapper.toResponse(any(User.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Updated John"))
                .andExpect(jsonPath("$.lastName").value("Updated Doe"))
                .andExpect(jsonPath("$.phone").value("9876543210"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    public void shouldReturn404WhenUpdatingNonExistentUser() throws Exception {
        // Arrange
        UserResponse updateRequest = new UserResponse();
        updateRequest.setFirstName("Updated Name");
        
        willThrow(new ResourceNotFoundException("User not found with ID: 999", "USER_NOT_FOUND"))
                .given(userService).findById(999L);

        // Act & Assert
        mockMvc.perform(put("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with ID: 999"))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    public void shouldReturn400WhenInvalidJsonIsProvidedForUpdate() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_JSON"));
    }

    // ========== Count Users Tests ==========

    @Test
    public void shouldReturn200WhenGettingTotalUserCount() throws Exception {
        // Arrange
        given(userService.countAllUsers()).willReturn(150L);

        // Act & Assert
        mockMvc.perform(get("/api/users/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(150));
    }

    @Test
    public void shouldReturn200WhenGettingUserCountByRole() throws Exception {
        // Arrange
        given(userService.countUsersByRole(UserRole.MEMBER)).willReturn(120L);
        given(userService.countUsersByRole(UserRole.ADMIN)).willReturn(5L);

        // Act & Assert
        mockMvc.perform(get("/api/users/count/MEMBER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(120));
                
        mockMvc.perform(get("/api/users/count/ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(5));
    }

    @Test
    public void shouldReturn400WhenInvalidRoleIsProvidedForCount() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/users/count/INVALID_ROLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
    }

    // ========== Edge Cases ==========

    @Test
    public void shouldReturn200WhenGettingZeroUserCount() throws Exception {
        // Arrange
        given(userService.countAllUsers()).willReturn(0L);
        given(userService.countUsersByRole(UserRole.MEMBER)).willReturn(0L);

        // Act & Assert
        mockMvc.perform(get("/api/users/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
                
        mockMvc.perform(get("/api/users/count/MEMBER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(0));
    }

    @Test
    public void shouldReturn200WhenGettingLargeUserCounts() throws Exception {
        // Arrange
        given(userService.countAllUsers()).willReturn(10000L);
        given(userService.countUsersByRole(UserRole.MEMBER)).willReturn(9500L);

        // Act & Assert
        mockMvc.perform(get("/api/users/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(10000));
                
        mockMvc.perform(get("/api/users/count/MEMBER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(9500));
    }

    @Test
    public void shouldReturn500WhenUnexpectedErrorOccurs() throws Exception {
        // Arrange
        given(userService.countAllUsers()).willThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        mockMvc.perform(get("/api/users/count"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"));
    }
}